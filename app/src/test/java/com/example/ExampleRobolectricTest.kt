package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DevicePreferences
import com.example.data.DeviceState
import com.example.data.DeviceTask
import com.example.data.PaymentOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Free Baby Call", appName)
  }

  @Test
  fun `lock state and enrollment persist across preferences`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = DevicePreferences(context)
    assertFalse(prefs.isDeviceLocked())
    assertFalse(prefs.isEnrolled())

    prefs.setEnrolled(true)
    prefs.saveCachedLockState(isLocked = true, amountDue = 3500.0)

    val reloaded = DevicePreferences(context)
    assertTrue(reloaded.isEnrolled())
    assertTrue(reloaded.isDeviceLocked())
    assertEquals(3500.0, reloaded.getCachedAmountDue(), 0.01)
  }

  @Test
  fun `firestore models have safe defaults and status helpers`() {
    val device = DeviceState(status = "locked", amountDue = 2500.0)
    assertTrue(device.isLocked)

    val task = DeviceTask(name = "Verify", status = "done")
    assertTrue(task.isDone)

    val order = PaymentOrder(status = "paid", amount = 2500.0)
    assertTrue(order.isPaid)
  }
}
