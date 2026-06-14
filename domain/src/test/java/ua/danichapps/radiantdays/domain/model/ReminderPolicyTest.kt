package ua.danichapps.radiantdays.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPolicyTest {

    @Test
    fun fireTime_isAlarmMinusOffset() {
        val event = CalendarEvent(
            description = "x",
            startTimeMillis = 0L,
            endTimeMillis = 1L,
            alarmTimeMillis = 10_000L,
            notificationMinutesBefore = 15,
        )
        assertEquals(10_000L - 15 * 60_000L, ReminderPolicy.reminderFireTimeMillis(event))
    }

    @Test
    fun shouldSchedule_falseWhenCompletedOrNoAlarm() {
        val base = CalendarEvent(
            description = "x",
            startTimeMillis = 0L,
            endTimeMillis = 1L,
            alarmTimeMillis = 100_000L,
            notificationMinutesBefore = 0,
        )
        assertFalse(ReminderPolicy.shouldScheduleReminder(base.copy(isCompleted = true), nowMillis = 0L))
        assertFalse(ReminderPolicy.shouldScheduleReminder(base.copy(alarmTimeMillis = null), nowMillis = 0L))
        assertTrue(ReminderPolicy.shouldScheduleReminder(base, nowMillis = 50_000L))
    }
}
