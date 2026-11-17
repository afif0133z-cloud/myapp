const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_uid_1";
const DEVICE_A_ID = "android_dev_a";
const DEVICE_B_ID = "android_dev_b";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("config/app: public read succeeds, client write fails, admin write succeeds", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(ADMIN_UID).set({ role: "admin" });
    await db.collection("config").doc("app").set({
      companyName: "Free Baby Call",
      logoUrl: "https://i.postimg.cc/Xqss1GKX/file-00000000c6f0821191367557d1089f92.png",
      lockBackgroundUrl: "https://i.postimg.cc/CxThLRpL/images-(1).jpg",
      warningHeading: "Device Locked",
      warningMessage: "Please pay installment",
      activeMessage: "Device active",
      paymentGatewayUrl: "https://pay.example.com",
      paymentInstructions: "Pay and enter PIN",
    });
  });

  const clientDb = testEnv.unauthenticatedContext().firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  await assertSucceeds(clientDb.collection("config").doc("app").get());
  await assertFails(
    clientDb.collection("config").doc("app").update({ companyName: "Hacked" })
  );
  await assertSucceeds(
    adminDb.collection("config").doc("app").update({ companyName: "Free Baby Call Pro" })
  );
});

test("devices/{deviceId}: self-enroll by Android ID succeeds, but cannot list all or alter lock status", async () => {
  const past = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("devices").doc(DEVICE_B_ID).set({
      deviceId: DEVICE_B_ID,
      status: "locked",
      amountDue: 5000,
      fcmToken: "token_b",
      lastSeen: past,
    });
  });

  const clientDb = testEnv.unauthenticatedContext().firestore();

  // New device enrolls with Android ID, active status, 0 due
  await assertSucceeds(
    clientDb.collection("devices").doc(DEVICE_A_ID).set({
      deviceId: DEVICE_A_ID,
      status: "active",
      amountDue: 0,
      fcmToken: "token_a",
      lastSeen: past,
    })
  );

  // Can read single device by ID and refresh lastSeen/fcmToken
  await assertSucceeds(clientDb.collection("devices").doc(DEVICE_A_ID).get());
  await assertSucceeds(
    clientDb.collection("devices").doc(DEVICE_A_ID).update({
      deviceId: DEVICE_A_ID,
      fcmToken: "token_a_v2",
      lastSeen: past,
    })
  );

  // Cannot list all devices
  await assertFails(clientDb.collection("devices").get());

  // Cannot unlock a locked device or modify amountDue
  await assertFails(
    clientDb.collection("devices").doc(DEVICE_B_ID).update({ status: "active", amountDue: 0 })
  );
  await assertFails(
    clientDb.collection("devices").doc("forged_locked").set({
      deviceId: "forged_locked",
      status: "locked",
      amountDue: 0,
      lastSeen: past,
    })
  );
});

test("orders/{orderId}: client can read single order by ID, cannot list or write orders", async () => {
  const past = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("orders").doc("order_a").set({
      deviceId: DEVICE_A_ID,
      amount: 3500,
      status: "pending",
      createdAt: past,
    });
  });

  const clientDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(clientDb.collection("orders").doc("order_a").get());
  await assertFails(clientDb.collection("orders").get());
  await assertFails(
    clientDb.collection("orders").doc("order_a").update({ status: "paid" })
  );
});

test("tasks/{taskId}: device lists own + global tasks, can only transition pending -> done on own task", async () => {
  const past = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("devices").doc(DEVICE_A_ID).set({
      deviceId: DEVICE_A_ID,
      status: "active",
      amountDue: 0,
      lastSeen: past,
    });
    await db.collection("tasks").doc("task_a1").set({
      deviceId: DEVICE_A_ID,
      name: "Verify ID",
      description: "Take a photo",
      steps: ["Step 1", "Step 2"],
      status: "pending",
      requiresPermission: "camera",
      createdAt: past,
    });
    await db.collection("tasks").doc("task_global").set({
      deviceId: "all",
      name: "Policy Sync",
      description: "Read policy",
      steps: ["Step 1"],
      status: "pending",
      createdAt: past,
    });
  });

  const clientDb = testEnv.unauthenticatedContext().firestore();

  // Filtered queries matching Kotlin repository
  await assertSucceeds(
    clientDb.collection("tasks").where("deviceId", "==", DEVICE_A_ID).get()
  );
  await assertSucceeds(
    clientDb.collection("tasks").where("deviceId", "==", "all").get()
  );

  // Device can mark own pending task as done (only status field)
  await assertSucceeds(
    clientDb.collection("tasks").doc("task_a1").update({ status: "done" })
  );

  // Cannot revert done -> pending or tamper with name/steps
  await assertFails(
    clientDb.collection("tasks").doc("task_a1").update({ status: "pending" })
  );
  await assertFails(
    clientDb.collection("tasks").doc("task_a1").update({ name: "Tampered" })
  );

  // Global task completion recorded per-device in subcollection
  await assertSucceeds(
    clientDb
      .collection("devices")
      .doc(DEVICE_A_ID)
      .collection("taskCompletions")
      .doc("task_global")
      .set({
        taskId: "task_global",
        deviceId: DEVICE_A_ID,
        completedAt: new Date(Date.now() - 1000),
      })
  );
  await assertSucceeds(
    clientDb
      .collection("devices")
      .doc(DEVICE_A_ID)
      .collection("taskCompletions")
      .get()
  );
});
