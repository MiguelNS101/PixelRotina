package com.example.data

import kotlin.math.roundToInt

data class CorrelationDayPoint(
    val dateKey: String,
    val dayShortLabel: String,
    val routineCompletionPercent: Int,
    val avgMood: Float?,
    val avgEnergy: Float?
)

data class ContextTagImpactItem(
    val tagCode: String,
    val labelPt: String,
    val entryCount: Int,
    val avgMood: Float?,
    val avgEnergy: Float?
)

data class DayJournalSummary(
    val dateKey: String,
    val entryCount: Int,
    val avgMood: Float?,
    val avgEnergy: Float?
)

data class JournalCorrelationSummary(
    val last7Days: List<CorrelationDayPoint> = emptyList(),
    val topContextTagImpacts: List<ContextTagImpactItem> = emptyList()
)

/**
 * 100% offline correlation & aggregation utilities connecting Routines, Check-ins,
 * and Journal Mood/Energy entries for WeeklyCalendarScreen and WeeklyReportScreen.
 */
object JournalCorrelationCalculator {

    fun computeDaySummaryByDate(
        journalEntries: List<JournalEntryEntity>
    ): Map<String, DayJournalSummary> {
        return journalEntries.groupBy { it.dateKey }.mapValues { (dateKey, list) ->
            val moods = list.mapNotNull { it.moodScore }
            val energies = list.mapNotNull { it.energyScore }
            DayJournalSummary(
                dateKey = dateKey,
                entryCount = list.size,
                avgMood = if (moods.isNotEmpty()) moods.average().toFloat() else null,
                avgEnergy = if (energies.isNotEmpty()) energies.average().toFloat() else null
            )
        }
    }

    fun buildCorrelationSummary(
        routines: List<RoutineEntity>,
        logs: List<CheckInLogEntity>,
        journalEntries: List<JournalEntryEntity>,
        todayKey: String = DateUtils.todayDateKey()
    ): JournalCorrelationSummary {
        return JournalCorrelationSummary(
            last7Days = buildLast7DaysPoints(routines, logs, journalEntries, todayKey),
            topContextTagImpacts = buildTopContextTagImpactsLast30Days(journalEntries, todayKey, topLimit = 3)
        )
    }

    fun buildLast7DaysPoints(
        routines: List<RoutineEntity>,
        logs: List<CheckInLogEntity>,
        journalEntries: List<JournalEntryEntity>,
        todayKey: String = DateUtils.todayDateKey()
    ): List<CorrelationDayPoint> {
        val enabledRoutines = routines.filter { it.isEnabled }
        val completedByDate = logs
            .filter { it.status == CheckInStatus.COMPLETED }
            .groupBy { it.dateKey }
        val journalByDate = journalEntries.groupBy { it.dateKey }

        return (-6..0).map { offset ->
            val key = DateUtils.shiftDateKey(todayKey, offset)
            val isoDay = DateUtils.isoDayOfWeekForDateKey(key)
            val activeForDay = enabledRoutines.filter { isoDay in it.activeDays }
            val logsForDay = completedByDate[key].orEmpty()

            val completionPercent = if (activeForDay.isEmpty()) {
                if (logsForDay.isNotEmpty()) 100 else 0
            } else {
                val targetTotal = activeForDay.sumOf { it.targetCountPerDay }.coerceAtLeast(1)
                val countByRoutine = logsForDay.groupingBy { it.routineId }.eachCount()
                val doneFromActive = activeForDay.sumOf { r ->
                    (countByRoutine[r.id] ?: 0).coerceAtMost(r.targetCountPerDay)
                }
                val effectiveDone = maxOf(doneFromActive, logsForDay.size.coerceAtMost(targetTotal))
                ((effectiveDone * 100f) / targetTotal.toFloat()).roundToInt().coerceIn(0, 100)
            }

            val dayJournal = journalByDate[key].orEmpty()
            val moods = dayJournal.mapNotNull { it.moodScore }
            val energies = dayJournal.mapNotNull { it.energyScore }

            CorrelationDayPoint(
                dateKey = key,
                dayShortLabel = DateUtils.formatDayShortPt(key),
                routineCompletionPercent = completionPercent,
                avgMood = if (moods.isNotEmpty()) moods.average().toFloat() else null,
                avgEnergy = if (energies.isNotEmpty()) energies.average().toFloat() else null
            )
        }
    }

    fun buildTopContextTagImpactsLast30Days(
        journalEntries: List<JournalEntryEntity>,
        todayKey: String = DateUtils.todayDateKey(),
        topLimit: Int = 3
    ): List<ContextTagImpactItem> {
        val minDateKey = DateUtils.shiftDateKey(todayKey, -29)
        val recentEntries = journalEntries.filter { it.dateKey >= minDateKey && it.dateKey <= todayKey }

        val entriesByTag = mutableMapOf<String, MutableList<JournalEntryEntity>>()
        for (entry in recentEntries) {
            for (tag in entry.contextTags.distinct()) {
                entriesByTag.getOrPut(tag.uppercase()) { mutableListOf() }.add(entry)
            }
        }

        return entriesByTag.entries
            .sortedWith(compareByDescending<Map.Entry<String, MutableList<JournalEntryEntity>>> { it.value.size }.thenBy { it.key })
            .take(topLimit)
            .map { (tagCode, list) ->
                val moods = list.mapNotNull { it.moodScore }
                val energies = list.mapNotNull { it.energyScore }
                ContextTagImpactItem(
                    tagCode = tagCode,
                    labelPt = JournalContextTag.labelForCode(tagCode),
                    entryCount = list.size,
                    avgMood = if (moods.isNotEmpty()) moods.average().toFloat() else null,
                    avgEnergy = if (energies.isNotEmpty()) energies.average().toFloat() else null
                )
            }
    }
}
