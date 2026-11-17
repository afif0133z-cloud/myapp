const { onRequest } = require("firebase-functions/v2/https");
const { onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const crypto = require("crypto");

initializeApp();
const db = getFirestore();

/**
 * 1. Register or refresh device enrollment in devices/{deviceId} using Android ID.
 *    Preserves any existing "locked" status and amountDue on the server.
 */
exports.registerDevice = onRequest({ cors: true }, async (req, res) => {
  if (req.method !== "POST") {
    return res.status(405).json({ error: "Method not allowed" });
  }
  try {
    const { deviceId, fcmToken } = req.body || {};
    if (!deviceId || typeof deviceId !== "string" || !/^[a-zA-Z0-9_-]{1,128}$/.test(deviceId)) {
      return res.status(400).json({ error: "Invalid deviceId" });
    }

    const deviceRef = db.collection("devices").doc(deviceId);
    const snap = await deviceRef.get();

    if (!snap.exists) {
      await deviceRef.set({
        deviceId,
        status: "active",
        amountDue: 0,
        fcmToken: typeof fcmToken === "string" ? fcmToken : "",
        lastSeen: FieldValue.serverTimestamp(),
      });
    } else {
      const updatePayload = {
        deviceId,
        lastSeen: FieldValue.serverTimestamp(),
      };
      if (typeof fcmToken === "string" && fcmToken.length > 0) {
        updatePayload.fcmToken = fcmToken;
      }
      await deviceRef.update(updatePayload);
    }

    return res.status(200).json({ ok: true, deviceId });
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

/**
 * 2. Create a payment order and build the payment gateway redirect URL.
 *    Called by Android client when user taps PAY NOW on the Locked screen.
 */
exports.createPaymentOrder = onRequest({ cors: true }, async (req, res) => {
  if (req.method !== "POST") {
    return res.status(405).json({ error: "Method not allowed" });
  }
  try {
    const { deviceId } = req.body || {};
    if (!deviceId || typeof deviceId !== "string" || !/^[a-zA-Z0-9_-]{1,128}$/.test(deviceId)) {
      return res.status(400).json({ error: "Invalid deviceId" });
    }

    const [deviceSnap, configSnap] = await Promise.all([
      db.collection("devices").doc(deviceId).get(),
      db.collection("config").doc("app").get(),
    ]);

    if (!deviceSnap.exists) {
      return res.status(404).json({ error: "Device not found" });
    }
    const device = deviceSnap.data();

    const amount = Number(device.amountDue || 0);
    const gatewayBaseUrl = (configSnap.exists && configSnap.data().paymentGatewayUrl) || "";
    if (!gatewayBaseUrl) {
      return res.status(400).json({ error: "Payment gateway URL not configured" });
    }

    const orderRef = db.collection("orders").doc();
    await orderRef.set({
      deviceId,
      amount,
      status: "pending",
      createdAt: FieldValue.serverTimestamp(),
    });

    const redirectUrl = new URL(gatewayBaseUrl);
    redirectUrl.searchParams.set("orderId", orderRef.id);
    redirectUrl.searchParams.set("amount", String(amount));
    redirectUrl.searchParams.set("deviceId", deviceId);

    return res.status(200).json({
      orderId: orderRef.id,
      amount,
      gatewayUrl: redirectUrl.toString(),
    });
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

/**
 * 3. Webhook endpoint called by the payment gateway server-to-server to confirm payment
 *    and generate a 6-digit unlock PIN in orders/{orderId}.
 *    Configure PAYMENT_GATEWAY_WEBHOOK_SECRET in Cloud Functions secrets/env.
 */
exports.paymentWebhook = onRequest(async (req, res) => {
  if (req.method !== "POST") {
    return res.status(405).json({ error: "Method not allowed" });
  }
  try {
    const expectedSecret = process.env.PAYMENT_GATEWAY_WEBHOOK_SECRET;
    const providedSecret = req.headers["x-webhook-secret"];
    if (!expectedSecret || providedSecret !== expectedSecret) {
      return res.status(401).json({ error: "Invalid webhook signature" });
    }

    const { orderId, status } = req.body || {};
    if (!orderId || status !== "paid") {
      return res.status(400).json({ error: "Invalid payment confirmation payload" });
    }

    const orderRef = db.collection("orders").doc(orderId);
    const orderSnap = await orderRef.get();
    if (!orderSnap.exists) {
      return res.status(404).json({ error: "Order not found" });
    }

    if (orderSnap.data().status === "paid" && orderSnap.data().pin) {
      return res.status(200).json({ ok: true, alreadyPaid: true });
    }

    const generatedPin = String(crypto.randomInt(100000, 1000000));
    await orderRef.update({
      status: "paid",
      pin: generatedPin,
    });

    return res.status(200).json({ ok: true });
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

/**
 * 4. Verify the 6-digit unlock PIN submitted on the PIN screen and unlock the device.
 *    Called by Android client on PinScreen. Never exposes PINs to the client.
 */
exports.verifyUnlockPin = onRequest({ cors: true }, async (req, res) => {
  if (req.method !== "POST") {
    return res.status(405).json({ error: "Method not allowed" });
  }
  try {
    const { deviceId, orderId, pin } = req.body || {};

    if (!deviceId || typeof deviceId !== "string" || typeof pin !== "string" || !/^\d{6}$/.test(pin)) {
      return res.status(400).json({ verified: false, error: "Invalid input" });
    }

    const deviceRef = db.collection("devices").doc(deviceId);
    const deviceSnap = await deviceRef.get();
    if (!deviceSnap.exists) {
      return res.status(404).json({ verified: false, error: "Device not found" });
    }

    let orderSnap = null;
    if (orderId) {
      const snap = await db.collection("orders").doc(orderId).get();
      if (snap.exists && snap.data().deviceId === deviceId && snap.data().status === "paid") {
        orderSnap = snap;
      }
    } else {
      const query = await db
        .collection("orders")
        .where("deviceId", "==", deviceId)
        .where("status", "==", "paid")
        .orderBy("createdAt", "desc")
        .limit(1)
        .get();
      if (!query.empty) {
        orderSnap = query.docs[0];
      }
    }

    if (!orderSnap) {
      return res.status(400).json({ verified: false, error: "No paid order found" });
    }

    const expectedPin = String(orderSnap.data().pin || "");
    if (expectedPin.length !== 6 || pin !== expectedPin) {
      return res.status(403).json({ verified: false, error: "Incorrect PIN" });
    }

    await db.runTransaction(async (tx) => {
      tx.update(deviceRef, {
        status: "active",
        amountDue: 0,
        lastSeen: FieldValue.serverTimestamp(),
      });
    });

    return res.status(200).json({ verified: true });
  } catch (err) {
    return res.status(500).json({ verified: false, error: err.message });
  }
});

/**
 * 5. Firestore onUpdate trigger on devices/{deviceId} that sends an FCM push
 *    whenever device status changes ("active" <-> "locked"), as backup to the realtime listener.
 */
exports.onDeviceStatusUpdate = onDocumentUpdated("devices/{deviceId}", async (event) => {
  const before = event.data.before.data();
  const after = event.data.after.data();
  if (!before || !after || before.status === after.status) {
    return null;
  }

  const fcmToken = after.fcmToken;
  if (!fcmToken) {
    return null;
  }

  const message = {
    token: fcmToken,
    data: {
      type: "DEVICE_STATUS",
      deviceId: event.params.deviceId,
      status: String(after.status || "active"),
      amountDue: String(after.amountDue || 0),
    },
    android: {
      priority: "high",
    },
  };

  return getMessaging().send(message);
});
