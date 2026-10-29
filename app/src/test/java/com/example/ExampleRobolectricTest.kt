package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.alarm.AlarmScheduler
import com.example.data.AlertMode
import com.example.data.BackupManager
import com.example.data.CheckInLogEntity
import com.example.data.CheckInSource
import com.example.data.CheckInStatus
import com.example.data.DateUtils
import com.example.data.FrequencyType
import com.example.data.RoutineCategory
import com.example.data.RoutineEntity
import com.example.ui.theme.PixelThemeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name and calendar tab from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("PixelRotina", context.getString(R.string.app_name))
        assertEquals("Calendário", context.getString(R.string.tab_calendar))
    }

    @Test
    fun `interval and specific time routines compute valid future trigger`() {
        val now = System.currentTimeMillis()
        val intervalRoutine = RoutineEntity(
            id = 1L,
            title = "Beber Água",
            frequencyTypeName = FrequencyType.INTERVAL.name,
            intervalMinutes = 45,
            windowStartHour = 8,
            windowEndHour = 22,
            isEnabled = true
        )
        val nextInterval = AlarmScheduler.computeNextTriggerMillis(intervalRoutine, now)
        assertTrue("Next interval trigger should be in the future", nextInterval > now)

        val alarmRoutine = RoutineEntity(
            id = 2L,
            title = "Alarme de Dormir",
            alertModeName = AlertMode.PERSISTENT_ALARM.name,
            frequencyTypeName = FrequencyType.SPECIFIC_TIME.name,
            hour = 22,
            minute = 30,
            isEnabled = true
        )
        val nextAlarm = AlarmScheduler.computeNextTriggerMillis(alarmRoutine, now)
        assertTrue("Next specific-time alarm should be in the future", nextAlarm > now)
    }

    @Test
    fun `month calendar grid generates 42 cells and backup roundtrips catppuccin mocha`() {
        val cells = DateUtils.buildMonthCalendarCells(2026, 9) // October 2026
        assertEquals(42, cells.size)
        assertTrue(cells.any { it.isCurrentMonth && it.dayOfMonth == 28 })

        val routines = listOf(
            RoutineEntity(
                id = 10L,
                title = "Alongamento",
                categoryName = RoutineCategory.CORPO_MENTE.name,
                alertModeName = AlertMode.QUICK_OK_NOTIFICATION.name,
                frequencyTypeName = FrequencyType.INTERVAL.name,
                intervalMinutes = 90
            )
        )
        val logs = listOf(
            CheckInLogEntity(
                id = 5L,
                routineId = 10L,
                routineTitleSnapshot = "Alongamento",
                categorySnapshot = RoutineCategory.CORPO_MENTE.name,
                dateKey = "2026-10-28",
                statusName = CheckInStatus.COMPLETED.name,
                sourceName = CheckInSource.NOTIFICATION_OK.name
            )
        )

        val json = BackupManager.serializeToJson(PixelThemeType.CATPPUCCIN_MOCHA, routines, logs)
        val (restoredTheme, restoredRoutines, restoredLogs) = BackupManager.parseFromJson(json)

        assertEquals(PixelThemeType.CATPPUCCIN_MOCHA, restoredTheme)
        assertEquals(1, restoredRoutines.size)
        assertEquals(1, restoredLogs.size)
    }

    @Test
    fun `pomodoro state formats time and computes progress accurately`() {
        val state = com.example.ui.components.PomodoroState(
            phase = com.example.ui.components.PomodoroPhase.FOCUS,
            totalSeconds = 1500,
            remainingSeconds = 750,
            isRunning = true
        )
        assertEquals("12:30", state.formattedTime)
        assertEquals(0.5f, state.progressFraction, 0.01f)
    }
}
