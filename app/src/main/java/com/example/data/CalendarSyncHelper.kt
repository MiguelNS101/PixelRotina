package com.example.data

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.TimeZone

data class DeviceCalendarEvent(
    val id: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val calendarName: String,
    val isAllDay: Boolean,
    val isoDayOfWeek: Int,
    val dateKey: String = DateUtils.todayDateKey(startMillis)
)

object CalendarSyncHelper {

    fun hasReadCalendarPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasWriteCalendarPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Reads events from the Android Calendar provider for a full displayed month (including adjacent week days).
     */
    fun queryMonthEvents(
        context: Context,
        year: Int,
        monthZeroBased: Int
    ): List<DeviceCalendarEvent> {
        if (!hasReadCalendarPermission(context)) return emptyList()

        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, -7)
        }
        val endCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, 1)
            add(Calendar.DAY_OF_MONTH, 7)
        }

        return queryEventsBetween(context, startCal.timeInMillis, endCal.timeInMillis)
    }

    fun queryUpcomingWeekEvents(context: Context): List<DeviceCalendarEvent> {
        val now = Calendar.getInstance()
        return queryMonthEvents(context, now.get(Calendar.YEAR), now.get(Calendar.MONTH))
    }

    private fun queryEventsBetween(
        context: Context,
        startMillis: Long,
        endMillis: Long
    ): List<DeviceCalendarEvent> {
        if (!hasReadCalendarPermission(context)) return emptyList()

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.ALL_DAY
        )

        val events = mutableListOf<DeviceCalendarEvent>()
        try {
            context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
                val calNameIdx = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
                val allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)

                while (cursor.moveToNext()) {
                    val eventId = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
                    val title = if (titleIdx >= 0) cursor.getString(titleIdx) ?: "Evento" else "Evento"
                    val begin = if (beginIdx >= 0) cursor.getLong(beginIdx) else 0L
                    val end = if (endIdx >= 0) cursor.getLong(endIdx) else begin + 3_600_000L
                    val calName = if (calNameIdx >= 0) cursor.getString(calNameIdx) ?: "Calendário" else "Calendário"
                    val allDay = if (allDayIdx >= 0) cursor.getInt(allDayIdx) == 1 else false

                    events.add(
                        DeviceCalendarEvent(
                            id = eventId,
                            title = title,
                            startMillis = begin,
                            endMillis = end,
                            calendarName = calName,
                            isAllDay = allDay,
                            isoDayOfWeek = DateUtils.currentIsoDayOfWeek(begin),
                            dateKey = DateUtils.todayDateKey(begin)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Return whatever was collected safely
        }
        return events
    }

    /**
     * Inserts or updates a RoutineEntity directly into the user's Android Calendar via ContentResolver.
     */
    fun upsertRoutineInDeviceCalendar(context: Context, routine: RoutineEntity): Long? {
        if (!hasReadCalendarPermission(context) || !hasWriteCalendarPermission(context)) {
            return null
        }

        val calendarId = findPrimaryWritableCalendarId(context) ?: return null
        val startCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, if (routine.frequencyType == FrequencyType.INTERVAL) routine.windowStartHour else routine.hour)
            set(Calendar.MINUTE, if (routine.frequencyType == FrequencyType.INTERVAL) 0 else routine.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMillis = startCal.timeInMillis
        val durationMinutes = if (routine.frequencyType == FrequencyType.INTERVAL) 15 else 30

        val byDayTokens = routine.activeDays.sorted().mapNotNull { iso ->
            when (iso) {
                1 -> "MO"
                2 -> "TU"
                3 -> "WE"
                4 -> "TH"
                5 -> "FR"
                6 -> "SA"
                7 -> "SU"
                else -> null
            }
        }.joinToString(",")

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, "✨ [PixelRotina] ${routine.title}")
            put(
                CalendarContract.Events.DESCRIPTION,
                routine.description.ifBlank { "Rotina de bem-estar (${routine.category.label}) gerenciada pelo PixelRotina." }
            )
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DURATION, "PT${durationMinutes}M")
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            if (byDayTokens.isNotEmpty()) {
                put(CalendarContract.Events.RRULE, "FREQ=WEEKLY;BYDAY=$byDayTokens")
            }
        }

        return try {
            val existingId = routine.calendarEventId
            if (existingId != null && existingId > 0L) {
                val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingId)
                val updatedRows = context.contentResolver.update(updateUri, values, null, null)
                if (updatedRows > 0) return existingId
            }
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.lastPathSegment?.toLongOrNull()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Opens the native Android Calendar app pre-filled with the routine details.
     */
    fun openNativeCalendarInsertIntent(context: Context, routine: RoutineEntity) {
        val startCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, if (routine.frequencyType == FrequencyType.INTERVAL) routine.windowStartHour else routine.hour)
            set(Calendar.MINUTE, if (routine.frequencyType == FrequencyType.INTERVAL) 0 else routine.minute)
            set(Calendar.SECOND, 0)
        }
        val endMillis = startCal.timeInMillis + 30 * 60_000L

        val byDayTokens = routine.activeDays.sorted().mapNotNull { iso ->
            when (iso) {
                1 -> "MO"
                2 -> "TU"
                3 -> "WE"
                4 -> "TH"
                5 -> "FR"
                6 -> "SA"
                7 -> "SU"
                else -> null
            }
        }.joinToString(",")

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "✨ ${routine.title}")
            putExtra(
                CalendarContract.Events.DESCRIPTION,
                routine.description.ifBlank { "Rotina de ${routine.category.label} — PixelRotina" }
            )
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startCal.timeInMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            if (byDayTokens.isNotEmpty()) {
                putExtra(CalendarContract.Events.RRULE, "FREQ=WEEKLY;BYDAY=$byDayTokens")
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    private fun findPrimaryWritableCalendarId(context: Context): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        var fallbackId: Long? = null
        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.VISIBLE} = 1",
                null,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val primaryIdx = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)
                val accessIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)

                while (cursor.moveToNext()) {
                    val id = if (idIdx >= 0) cursor.getLong(idIdx) else continue
                    val access = if (accessIdx >= 0) cursor.getInt(accessIdx) else 0
                    val isPrimary = if (primaryIdx >= 0) cursor.getInt(primaryIdx) == 1 else false

                    if (access >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) {
                        if (isPrimary) return id
                        if (fallbackId == null) fallbackId = id
                    }
                }
            }
        } catch (_: Exception) {
        }
        return fallbackId
    }
}
