package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ui.components.PixelSpriteType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class RoutineCategory(
    val label: String,
    val defaultSprite: PixelSpriteType
) {
    HIDRATACAO("Hidratação", PixelSpriteType.WATER_DROP),
    CORPO_MENTE("Corpo & Mente", PixelSpriteType.LEAF_WELLNESS),
    DESCANSO("Sono & Descanso", PixelSpriteType.MOON_REST),
    ALIMENTACAO("Nutrição & Energia", PixelSpriteType.SUN_MORNING),
    FOCO("Foco & Estudos", PixelSpriteType.BOOK_FOCUS),
    LAZER("Autocuidado & Lazer", PixelSpriteType.HEART_CARE)
}

enum class AlertMode(
    val title: String,
    val description: String
) {
    QUICK_OK_NOTIFICATION(
        title = "Notificação com OK Rápido",
        description = "Aviso leve com botão 'OK! Feito' direto na notificação (ideal para água e pausas)"
    ),
    SCHEDULED_NOTIFICATION(
        title = "Lembrete de Horário",
        description = "Notificação pontual com opções de concluir ou reagendar suavemente"
    ),
    PERSISTENT_ALARM(
        title = "Alarme Sonora & Vibração",
        description = "Funciona como despertador: vibra e toca continuamente até ser desligado"
    )
}

enum class FrequencyType(
    val title: String,
    val description: String
) {
    SPECIFIC_TIME(
        title = "Horário Específico",
        description = "Toca em um horário exato nos dias da semana escolhidos"
    ),
    INTERVAL(
        title = "Intervalo Customizável",
        description = "Repete a cada X minutos dentro da sua janela de horário ativo"
    )
}

enum class CheckInStatus(val label: String) {
    COMPLETED("Concluído"),
    SNOOZED("Adiado"),
    SKIPPED("Reagendado")
}

enum class CheckInSource(val label: String) {
    IN_APP("No App"),
    NOTIFICATION_OK("OK na Notificação"),
    ALARM_DISMISSED("Alarme Desligado")
}

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String = "",
    val categoryName: String = RoutineCategory.CORPO_MENTE.name,
    val spriteName: String = PixelSpriteType.LEAF_WELLNESS.name,
    val alertModeName: String = AlertMode.QUICK_OK_NOTIFICATION.name,
    val frequencyTypeName: String = FrequencyType.SPECIFIC_TIME.name,
    // For SPECIFIC_TIME: hour (0..23) and minute (0..59)
    val hour: Int = 9,
    val minute: Int = 0,
    // For INTERVAL: repeat every intervalMinutes between startHour..endHour
    val intervalMinutes: Int = 60,
    val windowStartHour: Int = 8,
    val windowEndHour: Int = 21,
    val targetCountPerDay: Int = 1,
    // Comma-separated ISO day of week (1=Seg, 2=Ter, 3=Qua, 4=Qui, 5=Sex, 6=Sab, 7=Dom)
    val daysOfWeekCsv: String = "1,2,3,4,5,6,7",
    val isEnabled: Boolean = true,
    val syncToCalendar: Boolean = false,
    val calendarEventId: Long? = null,
    val nextTriggerMillis: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    val category: RoutineCategory
        get() = RoutineCategory.entries.firstOrNull { it.name == categoryName }
            ?: RoutineCategory.CORPO_MENTE

    val sprite: PixelSpriteType
        get() = PixelSpriteType.entries.firstOrNull { it.name == spriteName }
            ?: category.defaultSprite

    val alertMode: AlertMode
        get() = AlertMode.entries.firstOrNull { it.name == alertModeName }
            ?: AlertMode.QUICK_OK_NOTIFICATION

    val frequencyType: FrequencyType
        get() = FrequencyType.entries.firstOrNull { it.name == frequencyTypeName }
            ?: FrequencyType.SPECIFIC_TIME

    val activeDays: Set<Int>
        get() = daysOfWeekCsv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .toSet()
            .ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) }

    fun formattedScheduleSummary(): String {
        return if (frequencyType == FrequencyType.INTERVAL) {
            val hStr = if (intervalMinutes >= 60 && intervalMinutes % 60 == 0) {
                "a cada ${intervalMinutes / 60}h"
            } else {
                "a cada ${intervalMinutes} min"
            }
            "$hStr (${String.format(Locale.getDefault(), "%02d:00–%02d:00", windowStartHour, windowEndHour)})"
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        }
    }

    fun formattedDaysSummary(): String {
        val days = activeDays
        if (days.size == 7) return "Todos os dias"
        if (days == setOf(1, 2, 3, 4, 5)) return "Seg a Sex"
        if (days == setOf(6, 7)) return "Fim de semana"
        val map = mapOf(
            1 to "Seg",
            2 to "Ter",
            3 to "Qua",
            4 to "Qui",
            5 to "Sex",
            6 to "Sáb",
            7 to "Dom"
        )
        return days.sorted().mapNotNull { map[it] }.joinToString(", ")
    }
}

@Entity(tableName = "check_in_logs")
data class CheckInLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val routineId: Long,
    val routineTitleSnapshot: String,
    val categorySnapshot: String,
    val dateKey: String, // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    val statusName: String = CheckInStatus.COMPLETED.name,
    val sourceName: String = CheckInSource.IN_APP.name,
    val note: String = ""
) {
    val status: CheckInStatus
        get() = CheckInStatus.entries.firstOrNull { it.name == statusName }
            ?: CheckInStatus.COMPLETED

    val source: CheckInSource
        get() = CheckInSource.entries.firstOrNull { it.name == sourceName }
            ?: CheckInSource.IN_APP
}

data class MonthCalendarCell(
    val dateKey: String,
    val dayOfMonth: Int,
    val isoDayOfWeek: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean
)

object DateUtils {
    fun todayDateKey(nowMillis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(nowMillis))
    }

    fun dateKeyForYmd(year: Int, monthZeroBased: Int, dayOfMonth: Int): String {
        return String.format(Locale.US, "%04d-%02d-%02d", year, monthZeroBased + 1, dayOfMonth)
    }

    fun dateKeyForOffsetDays(daysOffset: Int, baseMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply {
            timeInMillis = baseMillis
            add(Calendar.DAY_OF_YEAR, daysOffset)
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(cal.time)
    }

    /**
     * Converts Calendar.DAY_OF_WEEK (Sunday=1..Saturday=7) to ISO 1..7 (Monday=1..Sunday=7)
     */
    fun currentIsoDayOfWeek(nowMillis: Long = System.currentTimeMillis()): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return calendarDayToIso(cal.get(Calendar.DAY_OF_WEEK))
    }

    fun isoDayOfWeekForDateKey(dateKey: String): Int {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.parse(dateKey) ?: return currentIsoDayOfWeek()
            val cal = Calendar.getInstance().apply { time = date }
            calendarDayToIso(cal.get(Calendar.DAY_OF_WEEK))
        } catch (_: Exception) {
            currentIsoDayOfWeek()
        }
    }

    private fun calendarDayToIso(calDay: Int): Int {
        return when (calDay) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    fun formatTime(millis: Long): String {
        if (millis <= 0L) return "--:--"
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatMonthYearPt(year: Int, monthZeroBased: Int): String {
        val months = listOf(
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        )
        val mName = months.getOrElse(monthZeroBased.coerceIn(0, 11)) { "Mês" }
        return "$mName $year"
    }

    fun formatFullDatePt(dateKey: String): String {
        return try {
            val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdfIn.parse(dateKey) ?: return dateKey
            val cal = Calendar.getInstance().apply { time = date }
            val dayName = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Segunda-feira"
                Calendar.TUESDAY -> "Terça-feira"
                Calendar.WEDNESDAY -> "Quarta-feira"
                Calendar.THURSDAY -> "Quinta-feira"
                Calendar.FRIDAY -> "Sexta-feira"
                Calendar.SATURDAY -> "Sábado"
                Calendar.SUNDAY -> "Domingo"
                else -> ""
            }
            val months = listOf(
                "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
                "Jul", "Ago", "Set", "Out", "Nov", "Dez"
            )
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val month = months.getOrElse(cal.get(Calendar.MONTH)) { "" }
            "$dayName, $day de $month"
        } catch (_: Exception) {
            dateKey
        }
    }

    fun formatDayShortPt(dateKey: String): String {
        return try {
            val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdfIn.parse(dateKey) ?: return dateKey
            val cal = Calendar.getInstance().apply { time = date }
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Seg"
                Calendar.TUESDAY -> "Ter"
                Calendar.WEDNESDAY -> "Qua"
                Calendar.THURSDAY -> "Qui"
                Calendar.FRIDAY -> "Sex"
                Calendar.SATURDAY -> "Sáb"
                Calendar.SUNDAY -> "Dom"
                else -> dateKey.takeLast(2)
            }
        } catch (_: Exception) {
            dateKey.takeLast(2)
        }
    }

    /**
     * Generates a 42-cell (6 weeks x 7 days, Monday-first) grid for any month & year.
     */
    fun buildMonthCalendarCells(year: Int, monthZeroBased: Int): List<MonthCalendarCell> {
        val todayKey = todayDateKey()
        val firstOfMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val firstIsoDay = calendarDayToIso(firstOfMonth.get(Calendar.DAY_OF_WEEK)) // 1 (Mon)..7 (Sun)
        val leadingDays = firstIsoDay - 1

        val gridStartCal = (firstOfMonth.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, -leadingDays)
        }

        val cells = mutableListOf<MonthCalendarCell>()
        for (i in 0 until 42) {
            val cellYear = gridStartCal.get(Calendar.YEAR)
            val cellMonth = gridStartCal.get(Calendar.MONTH)
            val cellDay = gridStartCal.get(Calendar.DAY_OF_MONTH)
            val cellIso = calendarDayToIso(gridStartCal.get(Calendar.DAY_OF_WEEK))
            val key = dateKeyForYmd(cellYear, cellMonth, cellDay)
            cells.add(
                MonthCalendarCell(
                    dateKey = key,
                    dayOfMonth = cellDay,
                    isoDayOfWeek = cellIso,
                    isCurrentMonth = (cellYear == year && cellMonth == monthZeroBased),
                    isToday = (key == todayKey)
                )
            )
            gridStartCal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return cells
    }
}
