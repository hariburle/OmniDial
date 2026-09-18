package com.example.telecom

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReminderSchedulerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testScheduleReminderCreatesAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)

        val futureEpoch = System.currentTimeMillis() + 60_000L // 1 minute in the future
        ReminderScheduler.scheduleReminder(
            context = context,
            callId = 42L,
            phoneNumber = "+15550001111",
            callerName = "Jane Doe",
            note = "Follow up on contract",
            reminderEpoch = futureEpoch
        )

        val nextScheduled = shadowAlarmManager.nextScheduledAlarm
        assertNotNull(nextScheduled)
        assertEquals(futureEpoch, nextScheduled!!.triggerAtTime)

        // Verify scheduled broadcast intent payload
        val shadowIntent = shadowOf(nextScheduled.operation)
        val savedIntent = shadowIntent.savedIntent
        assertEquals(ReminderReceiver.ACTION_FIRE_REMINDER, savedIntent.action)
        assertEquals(42L, savedIntent.getLongExtra(ReminderReceiver.EXTRA_CALL_ID, -1L))
        assertEquals("+15550001111", savedIntent.getStringExtra(ReminderReceiver.EXTRA_PHONE_NUMBER))
        assertEquals("Jane Doe", savedIntent.getStringExtra(ReminderReceiver.EXTRA_CALLER_NAME))
        assertEquals("Follow up on contract", savedIntent.getStringExtra(ReminderReceiver.EXTRA_NOTE))
    }

    @Test
    fun testPastReminderIsNotScheduled() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)

        val pastEpoch = System.currentTimeMillis() - 10_000L // 10 seconds in the past
        ReminderScheduler.scheduleReminder(
            context = context,
            callId = 99L,
            phoneNumber = "+15550009999",
            callerName = "Past Caller",
            note = "Old note",
            reminderEpoch = pastEpoch
        )

        val nextScheduled = shadowAlarmManager.nextScheduledAlarm
        assertTrue(nextScheduled == null)
    }

    @Test
    fun testCancelReminderRemovesAlarm() {
        val futureEpoch = System.currentTimeMillis() + 120_000L
        ReminderScheduler.scheduleReminder(
            context = context,
            callId = 55L,
            phoneNumber = "+15552223333",
            callerName = "Cancel Me",
            note = "To be cancelled",
            reminderEpoch = futureEpoch
        )

        ReminderScheduler.cancelReminder(
            context = context,
            callId = 55L,
            phoneNumber = "+15552223333"
        )

        // Cancellation sends a cancel operation to alarmManager
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)
        val nextScheduled = shadowAlarmManager.nextScheduledAlarm
        // Alarm should be cleared or canceled
        assertTrue(nextScheduled == null || nextScheduled.triggerAtTime != futureEpoch)
    }
}
