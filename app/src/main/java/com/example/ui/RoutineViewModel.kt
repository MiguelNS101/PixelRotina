package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmRingingService
import com.example.alarm.AlarmScheduler
import com.example.alarm.NotificationHelper
import com.example.data.AiReactionMode
import com.example.data.AppDatabase
import com.example.data.BackupManager
import com.example.data.CalendarSyncHelper
import com.example.data.CheckInLogEntity
import com.example.data.CheckInSource
import com.example.data.CheckInStatus
import com.example.data.DateUtils
import com.example.data.DeviceCalendarEvent
import com.example.data.FrequencyType
import com.example.data.HeroArtPreference
import com.example.data.JournalAiAssistant
import com.example.data.JournalCorrelationCalculator
import com.example.data.JournalCorrelationSummary
import com.example.data.JournalEntryEntity
import com.example.data.JournalEntryType
import com.example.data.RoutineCategory
import com.example.data.RoutineEntity
import com.example.data.RoutineRepository
import com.example.data.UserPreferencesRepository
import com.example.data.UserSettings
import com.example.ui.components.PomodoroPhase
import com.example.ui.components.PomodoroState
import com.example.ui.components.PomodoroVisualAlert
import com.example.ui.theme.PixelThemeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

enum class AppTab {
    TODAY,
    WEEKLY_CALENDAR,
    JOURNAL,
    WEEKLY_REPORT,
    SETTINGS_BACKUP
}

data class DayReportBar(
    val dateKey: String,
    val dayShortLabel: String,
    val completedCount: Int,
    val snoozedCount: Int
)

data class CategoryReportItem(
    val category: RoutineCategory,
    val completedCount: Int,
    val targetTotal: Int
)

data class WeeklyReportSummary(
    val days: List<DayReportBar> = emptyList(),
    val categoryBreakdown: List<CategoryReportItem> = emptyList(),
    val totalCompletedWeek: Int = 0,
    val quickOkCountWeek: Int = 0,
    val alarmDismissedCountWeek: Int = 0,
    val bestPeriodOfDay: String = "Manhã (08h–12h)"
)

data class MonthWeekBar(
    val weekLabel: String,
    val rangeLabel: String,
    val completedCount: Int,
    val snoozedCount: Int
)

data class MonthlyReportSummary(
    val monthTitle: String = "",
    val totalCompletedMonth: Int = 0,
    val activeDaysInMonth: Int = 0,
    val totalDaysInMonth: Int = 30,
    val quickOkCountMonth: Int = 0,
    val alarmDismissedCountMonth: Int = 0,
    val weeksOfMonth: List<MonthWeekBar> = emptyList(),
    val dailyCompletedCounts: List<Pair<Int, Int>> = emptyList(),
    val categoryBreakdownMonth: List<CategoryReportItem> = emptyList()
)

data class DayMedalHistoryItem(
    val dateKey: String,
    val dayShortLabel: String,
    val completionPercent: Int,
    val medal: com.example.ui.components.PixelMedalTier
)

data class StreakMedalSummary(
    val hasActiveRoutinesToday: Boolean = false,
    val todayCompletedCount: Int = 0,
    val todayTargetCount: Int = 0,
    val todayCompletionPercent: Int = 0,
    val todayMedal: com.example.ui.components.PixelMedalTier = com.example.ui.components.PixelMedalTier.NONE,
    val currentStreakDays: Int = 0,
    val goldStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val recent7DaysMedals: List<DayMedalHistoryItem> = emptyList()
)

data class CalendarMonthState(
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val monthZeroBased: Int = Calendar.getInstance().get(Calendar.MONTH),
    val selectedDateKey: String = DateUtils.todayDateKey()
)

data class RoutineUiState(
    val routines: List<RoutineEntity> = emptyList(),
    val checkInLogs: List<CheckInLogEntity> = emptyList(),
    val settings: UserSettings = UserSettings(),
    val calendarEvents: List<DeviceCalendarEvent> = emptyList(),
    val selectedTab: AppTab = AppTab.TODAY,
    val calendarMonthState: CalendarMonthState = CalendarMonthState(),
    val editingRoutine: RoutineEntity? = null,
    val isEditorVisible: Boolean = false,
    val statusMessage: String? = null,
    val confettiTriggerId: Int = 0,
    val pomodoroState: PomodoroState = PomodoroState(),
    val todayDateKey: String = DateUtils.todayDateKey(),
    val streakMedalSummary: StreakMedalSummary = StreakMedalSummary(),
    val weeklyReport: WeeklyReportSummary = WeeklyReportSummary(),
    val monthlyReport: MonthlyReportSummary = MonthlyReportSummary(),
    val journalEntries: List<JournalEntryEntity> = emptyList(),
    val selectedJournalDateKey: String = DateUtils.todayDateKey(),
    val todayLatestMood: Int? = null,
    val todayLatestEnergy: Int? = null,
    val todayAvgMood: Float? = null,
    val todayAvgEnergy: Float? = null,
    val todayMoodCheckInCount: Int = 0,
    val pendingMoodPromptRoutine: RoutineEntity? = null,
    val memory30DaysAgoEntry: JournalEntryEntity? = null,
    val journalCorrelation: JournalCorrelationSummary = JournalCorrelationSummary(),
    val isAiLoading: Boolean = false,
    val aiLoadingEntryId: Long? = null
) {
    val selectedDateJournalEntries: List<JournalEntryEntity>
        get() = journalEntries.filter { it.dateKey == selectedJournalDateKey }

    val todayIsoDay: Int
        get() = DateUtils.currentIsoDayOfWeek()

    val selectedIsoDay: Int
        get() = DateUtils.isoDayOfWeekForDateKey(calendarMonthState.selectedDateKey)

    val todayActiveRoutines: List<RoutineEntity>
        get() = routines.filter { it.isEnabled && todayIsoDay in it.activeDays }

    val todayIntervalRoutines: List<RoutineEntity>
        get() = todayActiveRoutines.filter { it.frequencyType == FrequencyType.INTERVAL }

    val todayScheduledRoutines: List<RoutineEntity>
        get() = todayActiveRoutines
            .filter { it.frequencyType == FrequencyType.SPECIFIC_TIME }
            .sortedWith(compareBy({ it.hour }, { it.minute }))

    val todayCompletedCountByRoutine: Map<Long, Int>
        get() = checkInLogs
            .filter { it.dateKey == todayDateKey && it.status == CheckInStatus.COMPLETED }
            .groupingBy { it.routineId }
            .eachCount()

    val completedCountByDateKey: Map<String, Int>
        get() = checkInLogs
            .filter { it.status == CheckInStatus.COMPLETED }
            .groupingBy { it.dateKey }
            .eachCount()

    val overdueTodayRoutines: List<RoutineEntity>
        get() {
            val cal = Calendar.getInstance()
            val nowMinutesOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val completedCounts = todayCompletedCountByRoutine
            return todayScheduledRoutines.filter { routine ->
                val routineMinutes = routine.hour * 60 + routine.minute
                val doneCount = completedCounts[routine.id] ?: 0
                val isSnoozedInFuture = routine.nextTriggerMillis > System.currentTimeMillis() &&
                    DateUtils.todayDateKey(routine.nextTriggerMillis) == todayDateKey &&
                    routine.nextTriggerMillis > System.currentTimeMillis() + 5 * 60_000L
                doneCount < routine.targetCountPerDay && routineMinutes + 15 < nowMinutesOfDay && !isSnoozedInFuture
            }
        }
}

class RoutineViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val dao = AppDatabase.getInstance(appContext).routineDao()
    private val prefsRepo = UserPreferencesRepository(appContext)
    private val repository = RoutineRepository(appContext, dao, prefsRepo)

    private val selectedTabFlow = MutableStateFlow(AppTab.TODAY)
    private val calendarMonthStateFlow = MutableStateFlow(CalendarMonthState())
    private val editorStateFlow = MutableStateFlow<Pair<Boolean, RoutineEntity?>>(false to null)
    private val calendarEventsFlow = MutableStateFlow<List<DeviceCalendarEvent>>(emptyList())
    private val statusMessageFlow = MutableStateFlow<String?>(null)
    private val confettiTriggerFlow = MutableStateFlow(0)
    private val pomodoroStateFlow = MutableStateFlow(PomodoroState())
    private val selectedJournalDateKeyFlow = MutableStateFlow(DateUtils.todayDateKey())
    private val aiLoadingStateFlow = MutableStateFlow<Pair<Boolean, Long?>>(false to null)
    private val pendingMoodPromptRoutineFlow = MutableStateFlow<RoutineEntity?>(null)
    private var pomodoroTickerJob: Job? = null

    val uiState: StateFlow<RoutineUiState> = combine(
        repository.allRoutines,
        repository.allCheckIns,
        repository.userSettings,
        selectedTabFlow,
        combine(
            calendarMonthStateFlow,
            editorStateFlow,
            calendarEventsFlow,
            combine(
                statusMessageFlow,
                confettiTriggerFlow,
                pomodoroStateFlow,
                pendingMoodPromptRoutineFlow
            ) { msg, confetti, pomodoro, pendingPrompt ->
                PromptAndStatusBundle(msg, confetti, pomodoro, pendingPrompt)
            },
            combine(
                repository.allJournalEntries,
                selectedJournalDateKeyFlow,
                aiLoadingStateFlow
            ) { entries, selectedJournalDate, aiLoading -> Triple(entries, selectedJournalDate, aiLoading) }
        ) { calState, editor, cal, statusBundle, journalBundle ->
            ExtraUiBundle(
                calState = calState,
                editor = editor,
                calendarEvents = cal,
                statusMessage = statusBundle.statusMessage,
                confettiTriggerId = statusBundle.confettiTriggerId,
                pomodoroState = statusBundle.pomodoroState,
                pendingMoodPromptRoutine = statusBundle.pendingMoodPromptRoutine,
                journalEntries = journalBundle.first,
                selectedJournalDateKey = journalBundle.second,
                isAiLoading = journalBundle.third.first,
                aiLoadingEntryId = journalBundle.third.second
            )
        }
    ) { routines, logs, settings, tab, extra ->
        val todayKey = DateUtils.todayDateKey()
        val nowMillis = System.currentTimeMillis()
        val entries = extra.journalEntries
        val todayEntries = entries.filter { it.dateKey == todayKey }
        val todayLatestMood = todayEntries.firstOrNull { it.moodScore != null }?.moodScore
        val todayLatestEnergy = todayEntries.firstOrNull { it.energyScore != null }?.energyScore
        val todayMoods = todayEntries.mapNotNull { it.moodScore }
        val todayEnergies = todayEntries.mapNotNull { it.energyScore }
        val todayAvgMood = if (todayMoods.isNotEmpty()) todayMoods.average().toFloat() else null
        val todayAvgEnergy = if (todayEnergies.isNotEmpty()) todayEnergies.average().toFloat() else null
        val todayMoodCount = todayEntries.count { it.moodScore != null || it.energyScore != null }
        val date30DaysAgoKey = DateUtils.dateKeyForOffsetDays(-30, nowMillis)
        val memory30DaysAgo = entries.firstOrNull {
            it.dateKey == date30DaysAgoKey && !it.isTimeCapsuleLocked(nowMillis)
        }

        RoutineUiState(
            routines = routines,
            checkInLogs = logs,
            settings = settings,
            calendarEvents = extra.calendarEvents,
            selectedTab = tab,
            calendarMonthState = extra.calState,
            editingRoutine = extra.editor.second,
            isEditorVisible = extra.editor.first,
            statusMessage = extra.statusMessage,
            confettiTriggerId = extra.confettiTriggerId,
            pomodoroState = extra.pomodoroState,
            todayDateKey = todayKey,
            streakMedalSummary = buildStreakMedalSummary(routines, logs, todayKey),
            weeklyReport = buildWeeklyReport(routines, logs),
            monthlyReport = buildMonthlyReport(routines, logs),
            journalEntries = entries,
            selectedJournalDateKey = extra.selectedJournalDateKey,
            todayLatestMood = todayLatestMood,
            todayLatestEnergy = todayLatestEnergy,
            todayAvgMood = todayAvgMood,
            todayAvgEnergy = todayAvgEnergy,
            todayMoodCheckInCount = todayMoodCount,
            pendingMoodPromptRoutine = extra.pendingMoodPromptRoutine,
            memory30DaysAgoEntry = memory30DaysAgo,
            journalCorrelation = JournalCorrelationCalculator.buildCorrelationSummary(
                routines = routines,
                logs = logs,
                journalEntries = entries,
                todayKey = todayKey
            ),
            isAiLoading = extra.isAiLoading,
            aiLoadingEntryId = extra.aiLoadingEntryId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RoutineUiState()
    )

    init {
        NotificationHelper.ensureChannelsCreated(appContext)
        refreshDeviceCalendarEvents()
        viewModelScope.launch(Dispatchers.IO) {
            val s = prefsRepo.settingsFlow.first()
            AlarmScheduler.scheduleDailyCheckInNotification(
                context = appContext,
                enabled = s.dailyCheckInNotifEnabled,
                hour = s.dailyCheckInNotifHour,
                minute = s.dailyCheckInNotifMinute
            )
        }
    }

    fun selectTab(tab: AppTab) {
        selectedTabFlow.value = tab
        if (tab == AppTab.WEEKLY_CALENDAR) {
            refreshDeviceCalendarEvents()
        }
    }

    fun selectCalendarDate(dateKey: String) {
        calendarMonthStateFlow.value = calendarMonthStateFlow.value.copy(
            selectedDateKey = dateKey
        )
    }

    fun changeCalendarMonth(monthDelta: Int) {
        val current = calendarMonthStateFlow.value
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, current.year)
            set(Calendar.MONTH, current.monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, monthDelta)
        }
        val newYear = cal.get(Calendar.YEAR)
        val newMonth = cal.get(Calendar.MONTH)
        val today = Calendar.getInstance()
        val selectedKey = if (newYear == today.get(Calendar.YEAR) && newMonth == today.get(Calendar.MONTH)) {
            DateUtils.todayDateKey()
        } else {
            DateUtils.dateKeyForYmd(newYear, newMonth, 1)
        }
        calendarMonthStateFlow.value = CalendarMonthState(
            year = newYear,
            monthZeroBased = newMonth,
            selectedDateKey = selectedKey
        )
        refreshDeviceCalendarEvents(newYear, newMonth)
    }

    fun jumpToCurrentMonthAndToday() {
        val now = Calendar.getInstance()
        val y = now.get(Calendar.YEAR)
        val m = now.get(Calendar.MONTH)
        calendarMonthStateFlow.value = CalendarMonthState(
            year = y,
            monthZeroBased = m,
            selectedDateKey = DateUtils.todayDateKey()
        )
        refreshDeviceCalendarEvents(y, m)
    }

    fun openCreateRoutineDialog(defaultFrequency: FrequencyType = FrequencyType.SPECIFIC_TIME) {
        val defaultRoutine = RoutineEntity(
            title = "",
            description = "",
            categoryName = if (defaultFrequency == FrequencyType.INTERVAL) {
                RoutineCategory.HIDRATACAO.name
            } else {
                RoutineCategory.CORPO_MENTE.name
            },
            frequencyTypeName = defaultFrequency.name,
            targetCountPerDay = if (defaultFrequency == FrequencyType.INTERVAL) 6 else 1,
            daysOfWeekCsv = (1..7).joinToString(",")
        )
        editorStateFlow.value = true to defaultRoutine
    }

    fun openEditRoutineDialog(routine: RoutineEntity) {
        editorStateFlow.value = true to routine
    }

    fun closeRoutineDialog() {
        editorStateFlow.value = false to null
    }

    fun saveRoutine(routine: RoutineEntity) {
        viewModelScope.launch {
            val cleanTitle = routine.title.trim().ifEmpty { "Nova Rotina de Bem-Estar" }
            val saved = repository.saveRoutine(routine.copy(title = cleanTitle))
            editorStateFlow.value = false to null
            if (saved.syncToCalendar) {
                refreshDeviceCalendarEvents()
            }
            com.example.widget.PixelWidgetUpdater.refreshAll(appContext)
            showStatus("✨ Rotina '${saved.title}' salva e agendada!")
        }
    }

    fun toggleRoutineEnabled(routine: RoutineEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleRoutineEnabled(routine, enabled)
            com.example.widget.PixelWidgetUpdater.refreshAll(appContext)
            val msg = if (enabled) "Ativada: ${routine.title}" else "Pausada: ${routine.title}"
            showStatus(msg)
        }
    }

    fun deleteRoutine(routine: RoutineEntity) {
        viewModelScope.launch {
            repository.deleteRoutine(routine)
            editorStateFlow.value = false to null
            com.example.widget.PixelWidgetUpdater.refreshAll(appContext)
            showStatus("Rotina '${routine.title}' removida.")
        }
    }

    fun markRoutineDoneNow(routine: RoutineEntity, note: String = "") {
        viewModelScope.launch {
            val previousMedal = uiState.value.streakMedalSummary.todayMedal
            AlarmRingingService.stopAlarm(appContext)
            NotificationHelper.cancelNotification(appContext, routine.id)
            repository.recordCheckIn(
                routine = routine,
                status = CheckInStatus.COMPLETED,
                source = CheckInSource.IN_APP,
                note = note.ifBlank { "Check-in confirmado no app" }
            )
            com.example.widget.PixelWidgetUpdater.refreshAll(appContext)
            // Trigger 8-bit pixel confetti burst!
            confettiTriggerFlow.value = confettiTriggerFlow.value + 1

            val currentState = uiState.value
            val activeToday = currentState.todayActiveRoutines
            val totalTarget = activeToday.sumOf { it.targetCountPerDay }.coerceAtLeast(1)
            val updatedDone = activeToday.sumOf { r ->
                val baseDone = currentState.todayCompletedCountByRoutine[r.id] ?: 0
                val afterIncrement = if (r.id == routine.id) baseDone + 1 else baseDone
                afterIncrement.coerceAtMost(r.targetCountPerDay)
            }
            val newPercent = ((updatedDone * 100f) / totalTarget.toFloat()).toInt().coerceIn(0, 100)
            val newMedal = com.example.ui.components.PixelMedalTier.fromCompletionPercent(
                percent = newPercent,
                hasActiveRoutines = activeToday.isNotEmpty()
            )

            val celebrationMsg = if (newMedal.thresholdPercent > previousMedal.thresholdPercent) {
                when (newMedal) {
                    com.example.ui.components.PixelMedalTier.GOLD ->
                        "🥇 Dia 100% Completo! Medalha de Ouro conquistada com '${routine.title}'!"
                    com.example.ui.components.PixelMedalTier.SILVER ->
                        "🥈 75% do dia concluído! Medalha de Prata conquistada!"
                    com.example.ui.components.PixelMedalTier.BRONZE ->
                        "🥉 50% do dia concluído! Medalha de Bronze conquistada!"
                    else -> "🎉 Boa! '${routine.title}' concluída!"
                }
            } else {
                "🎉 Boa! '${routine.title}' concluída!"
            }
            showStatus(celebrationMsg)
            if (currentState.settings.promptMoodAfterRoutineCheckIn) {
                pendingMoodPromptRoutineFlow.value = routine
            }
        }
    }

    fun dismissPostRoutineMoodPrompt() {
        pendingMoodPromptRoutineFlow.value = null
    }

    fun savePostRoutineMoodEnergy(
        routine: RoutineEntity,
        moodScore: Int,
        energyScore: Int
    ) {
        pendingMoodPromptRoutineFlow.value = null
        saveJournalEntry(
            entryType = JournalEntryType.MOOD_CHECKIN,
            moodScore = moodScore,
            energyScore = energyScore,
            contextTags = emptyList(),
            content = "Após rotina: ${routine.title}",
            unlockAtMillis = null,
            dateKey = uiState.value.todayDateKey
        )
    }

    fun setDailyCheckInNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setDailyCheckInNotifEnabled(enabled)
            val s = prefsRepo.settingsFlow.first()
            AlarmScheduler.scheduleDailyCheckInNotification(
                context = appContext,
                enabled = enabled,
                hour = s.dailyCheckInNotifHour,
                minute = s.dailyCheckInNotifMinute
            )
            val formattedTime = String.format(Locale.US, "%02d:%02d", s.dailyCheckInNotifHour, s.dailyCheckInNotifMinute)
            showStatus(
                if (enabled) "🌙 Check-in diário agendado para às $formattedTime!"
                else "Notificação de check-in do fim do dia desativada."
            )
        }
    }

    fun setDailyCheckInNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            val safeHour = ((hour % 24) + 24) % 24
            val safeMinute = ((minute % 60) + 60) % 60
            prefsRepo.setDailyCheckInNotifTime(safeHour, safeMinute)
            val s = prefsRepo.settingsFlow.first()
            if (s.dailyCheckInNotifEnabled) {
                AlarmScheduler.scheduleDailyCheckInNotification(
                    context = appContext,
                    enabled = true,
                    hour = safeHour,
                    minute = safeMinute
                )
            }
            val formattedTime = String.format(Locale.US, "%02d:%02d", safeHour, safeMinute)
            showStatus("🌙 Horário do check-in diário ajustado para $formattedTime!")
        }
    }

    fun triggerTestDailyCheckInNotification() {
        AlarmScheduler.triggerImmediateDailyCheckInTest(appContext)
        showStatus("🌙 Notificação de Check-in enviada! Responda Humor e Energia direto pela barra.")
    }

    fun setPromptMoodAfterRoutineCheckIn(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setPromptMoodAfterRoutineCheckIn(enabled)
            showStatus(
                if (enabled) "✨ Pergunta rápida de Humor & Energia após concluir rotina ativada!"
                else "Pergunta automática de Humor & Energia após rotina desativada."
            )
        }
    }

    fun gentleRescheduleRoutine(routine: RoutineEntity, delayMinutes: Int = 15) {
        viewModelScope.launch {
            val nextTime = repository.gentleRescheduleRoutine(routine, delayMinutes)
            showStatus("⏰ '${routine.title}' reagendado suavemente para ${DateUtils.formatTime(nextTime)}.")
        }
    }

    fun triggerTestAlertForRoutine(routine: RoutineEntity) {
        AlarmScheduler.triggerImmediateTest(appContext, routine)
        val typeMsg = if (routine.alertMode == com.example.data.AlertMode.PERSISTENT_ALARM) {
            "🔔 Disparando Alarme Persistente (som e vibração)..."
        } else {
            "📩 Notificação enviada! Confira o botão 'OK! Feito' na barra de notificações."
        }
        showStatus(typeMsg)
    }

    fun stopRingingAlarmNow() {
        AlarmRingingService.stopAlarm(appContext)
        showStatus("Alarme silenciado.")
    }

    fun refreshDeviceCalendarEvents(
        year: Int = calendarMonthStateFlow.value.year,
        monthZeroBased: Int = calendarMonthStateFlow.value.monthZeroBased
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val events = CalendarSyncHelper.queryMonthEvents(appContext, year, monthZeroBased)
            calendarEventsFlow.value = events
        }
    }

    fun syncRoutineToAndroidCalendar(routine: RoutineEntity) {
        viewModelScope.launch {
            val eventId = withContext(Dispatchers.IO) {
                CalendarSyncHelper.upsertRoutineInDeviceCalendar(appContext, routine)
            }
            if (eventId != null) {
                repository.saveRoutine(routine.copy(syncToCalendar = true, calendarEventId = eventId))
                refreshDeviceCalendarEvents()
                showStatus("📅 '${routine.title}' sincronizada com o Calendário do Android!")
            } else {
                CalendarSyncHelper.openNativeCalendarInsertIntent(appContext, routine)
                showStatus("Abrindo o Calendário do Android para adicionar '${routine.title}'...")
            }
        }
    }

    fun selectTheme(themeType: PixelThemeType) {
        viewModelScope.launch {
            prefsRepo.setTheme(themeType)
            com.example.widget.PixelWidgetUpdater.refreshAll(appContext)
            showStatus("🎨 Tema alterado para ${themeType.title}!")
        }
    }

    fun selectHeroArtPreference(preference: HeroArtPreference) {
        viewModelScope.launch {
            prefsRepo.setHeroArtPreference(preference)
            com.example.widget.PixelWidgetUpdater.refreshAll(appContext)
            showStatus("🖼️ Arte do card Hoje ajustada para: ${preference.title}!")
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            prefsRepo.setOnboardingCompleted(true)
            confettiTriggerFlow.value = confettiTriggerFlow.value + 1
        }
    }

    fun reopenOnboarding() {
        viewModelScope.launch {
            prefsRepo.setOnboardingCompleted(false)
        }
    }

    fun updateSnoozeMinutes(minutes: Int) {
        viewModelScope.launch {
            prefsRepo.setDefaultSnoozeMinutes(minutes)
            showStatus("Soneca padrão ajustada para +${minutes} min.")
        }
    }

    fun selectJournalDate(dateKey: String) {
        selectedJournalDateKeyFlow.value = dateKey
    }

    fun shiftJournalDate(daysDelta: Int) {
        val shifted = DateUtils.shiftDateKey(selectedJournalDateKeyFlow.value, daysDelta)
        selectedJournalDateKeyFlow.value = shifted
    }

    fun saveJournalEntry(
        entryType: JournalEntryType,
        moodScore: Int?,
        energyScore: Int?,
        contextTags: List<String>,
        content: String,
        unlockAtMillis: Long? = null,
        dateKey: String = uiState.value.selectedJournalDateKey
    ) {
        viewModelScope.launch {
            val stateSnapshot = uiState.value
            val todayKey = stateSnapshot.todayDateKey
            val effectiveDateKey = dateKey.ifBlank { todayKey }
            val isFirstMoodToday = moodScore != null &&
                effectiveDateKey == todayKey &&
                stateSnapshot.journalEntries.none { it.dateKey == todayKey && it.moodScore != null }

            val cleanTagsCsv = contextTags
                .map { it.trim().uppercase() }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString(",")

            val entry = JournalEntryEntity(
                dateKey = effectiveDateKey,
                timestamp = System.currentTimeMillis(),
                entryTypeName = entryType.name,
                moodScore = moodScore?.coerceIn(1, 5),
                energyScore = energyScore?.coerceIn(1, 5),
                contextTagsCsv = cleanTagsCsv,
                content = content.trim(),
                routineCompletionPercentSnapshot = stateSnapshot.streakMedalSummary.todayCompletionPercent,
                medalSnapshotName = stateSnapshot.streakMedalSummary.todayMedal.name,
                pomodoroCyclesSnapshot = stateSnapshot.pomodoroState.completedFocusCyclesToday,
                unlockAtMillis = if (entryType == JournalEntryType.TIME_CAPSULE) unlockAtMillis else null
            )

            repository.insertJournalEntry(entry)
            com.example.widget.PixelWidgetUpdater.refreshAll(appContext)

            if (isFirstMoodToday) {
                confettiTriggerFlow.value = confettiTriggerFlow.value + 1
            }

            val msg = when (entryType) {
                JournalEntryType.MOOD_CHECKIN -> if (isFirstMoodToday) {
                    "✨ Primeiro check-in de humor e energia do dia registrado!"
                } else {
                    "🔋 Humor e energia atualizados no seu diário!"
                }
                JournalEntryType.MICRO_LOG -> "📝 Micro-log salvo com sucesso no diário!"
                JournalEntryType.REFLECTION -> "📖 Reflexão salva com sucesso no diário!"
                JournalEntryType.TIME_CAPSULE -> {
                    val unlockStr = unlockAtMillis?.let { DateUtils.formatDateNumericPt(it) } ?: "o futuro"
                    "🔒 Cápsula do Tempo selada! Ela abrirá em $unlockStr."
                }
            }
            showStatus(msg)
        }
    }

    fun deleteJournalEntry(entry: JournalEntryEntity) {
        viewModelScope.launch {
            repository.deleteJournalEntry(entry)
            showStatus("Registro removido do diário.")
        }
    }

    fun toggleStealthMode() {
        viewModelScope.launch {
            val next = !uiState.value.settings.stealthModeEnabled
            prefsRepo.setStealthModeEnabled(next)
            showStatus(
                if (next) "🔒 Modo Furtivo ativado: textos do diário ocultados."
                else "🔓 Modo Furtivo desativado: textos visíveis."
            )
        }
    }

    fun setStealthMode(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setStealthModeEnabled(enabled)
            showStatus(
                if (enabled) "🔒 Modo Furtivo ativado: textos do diário ocultados."
                else "🔓 Modo Furtivo desativado: textos visíveis."
            )
        }
    }

    fun updateJournalPin(pin: String) {
        viewModelScope.launch {
            val sanitized = pin.filter { it.isDigit() }.take(4)
            prefsRepo.setJournalPin(sanitized)
            if (sanitized.isEmpty()) {
                showStatus("🔓 PIN do Diário removido.")
            } else {
                showStatus("🔐 PIN de 4 dígitos configurado para o Modo Furtivo!")
            }
        }
    }

    fun updateGeminiApiKey(apiKey: String) {
        viewModelScope.launch {
            val clean = apiKey.trim()
            prefsRepo.setGeminiApiKey(clean)
            if (clean.isEmpty()) {
                showStatus("Chave Gemini removida. O app segue 100% offline.")
            } else {
                showStatus("✨ Chave Gemini salva localmente no aparelho!")
            }
        }
    }

    fun updateGeminiModelName(modelName: String) {
        viewModelScope.launch {
            val clean = modelName.trim().ifEmpty { JournalAiAssistant.DEFAULT_MODEL_NAME }
            prefsRepo.setGeminiModelName(clean)
            showStatus("🤖 Modelo Gemini selecionado: $clean")
        }
    }

    fun requestAiReactionForEntry(entry: JournalEntryEntity, mode: AiReactionMode) {
        if (aiLoadingStateFlow.value.first) return
        val currentState = uiState.value
        val apiKey = currentState.settings.geminiApiKey.trim()
        if (apiKey.isEmpty()) {
            showStatus("Configure sua chave Gemini em Ajustes para usar reações de IA.")
            return
        }
        val modelName = currentState.settings.geminiModelName
        val recentTitles = currentState.routines.filter { it.isEnabled }.map { it.title }

        viewModelScope.launch {
            aiLoadingStateFlow.value = true to entry.id
            try {
                val assistant = JournalAiAssistant(apiKey = apiKey, modelName = modelName)
                val result = assistant.analyzeJournalEntry(
                    entry = entry,
                    mode = mode,
                    recentRoutinesTitles = recentTitles
                )
                result.onSuccess { analysis ->
                    val mergedTags = if (analysis.suggestedTags.isNotEmpty() && entry.contextTags.isEmpty()) {
                        analysis.suggestedTags.joinToString(",")
                    } else {
                        entry.contextTagsCsv
                    }
                    val updated = entry.copy(
                        contextTagsCsv = mergedTags,
                        aiReactionModeName = mode.name,
                        aiResponseText = analysis.responseText,
                        aiSuggestedRoutineJson = analysis.suggestedRoutineJson
                    )
                    repository.insertJournalEntry(updated)
                    showStatus("✨ Análise '${mode.labelPt}' concluída!")
                }.onFailure {
                    showStatus("Não foi possível consultar o Gemini agora. Verifique a internet ou sua chave.")
                }
            } finally {
                aiLoadingStateFlow.value = false to null
            }
        }
    }

    fun requestWeeklyPatchNotes() {
        if (aiLoadingStateFlow.value.first) return
        val currentState = uiState.value
        val apiKey = currentState.settings.geminiApiKey.trim()
        if (apiKey.isEmpty()) {
            showStatus("Configure sua chave Gemini em Ajustes para gerar os Patch Notes.")
            return
        }
        val modelName = currentState.settings.geminiModelName
        val todayKey = currentState.todayDateKey
        val minKey = DateUtils.shiftDateKey(todayKey, -6)
        val nowMillis = System.currentTimeMillis()
        val recentEntries = currentState.journalEntries.filter {
            it.dateKey >= minKey && it.dateKey <= todayKey && !it.isTimeCapsuleLocked(nowMillis)
        }

        viewModelScope.launch {
            aiLoadingStateFlow.value = true to null
            try {
                val assistant = JournalAiAssistant(apiKey = apiKey, modelName = modelName)
                val result = assistant.generateWeeklyPatchNotes(
                    recentEntries = recentEntries,
                    weeklyReport = currentState.weeklyReport,
                    streakSummary = currentState.streakMedalSummary
                )
                result.onSuccess { patchNotes ->
                    prefsRepo.saveCachedWeeklyPatchNotes(patchNotes, todayKey)
                    showStatus("🎮 Patch Notes da Semana gerados com sucesso!")
                }.onFailure {
                    showStatus("Não foi possível gerar os Patch Notes agora. Verifique a conexão ou chave.")
                }
            } finally {
                aiLoadingStateFlow.value = false to null
            }
        }
    }

    fun openSuggestedRoutineInEditor(suggestedJson: String): Boolean {
        val draft = JournalAiAssistant.parseSuggestedRoutineJsonToEntity(suggestedJson)
        return if (draft != null) {
            editorStateFlow.value = true to draft
            showStatus("✨ Rascunho de rotina pronto para revisão!")
            true
        } else {
            showStatus("Não foi possível interpretar a sugestão de rotina.")
            false
        }
    }

    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            val json = withContext(Dispatchers.IO) {
                repository.exportFullBackupJson(uiState.value.settings.themeType)
            }
            val success = withContext(Dispatchers.IO) {
                BackupManager.writeBackupToUri(appContext, uri, json)
            }
            if (success) {
                showStatus("💾 Arquivo de Save (.json) exportado com sucesso!")
            } else {
                showStatus("Não foi possível salvar no arquivo selecionado.")
            }
        }
    }

    fun importBackupFromUri(uri: Uri, replaceExisting: Boolean) {
        viewModelScope.launch {
            val json = withContext(Dispatchers.IO) {
                BackupManager.readBackupFromUri(appContext, uri)
            }
            if (json.isNullOrBlank()) {
                showStatus("Arquivo de save inválido ou vazio.")
                return@launch
            }
            try {
                val res = withContext(Dispatchers.IO) {
                    repository.importFullBackupJson(json, replaceExisting)
                }
                val journalSuffix = if (res.journalEntriesImported > 0) {
                    " e ${res.journalEntriesImported} notas do diário"
                } else {
                    ""
                }
                showStatus("📂 Save restaurado: ${res.routinesImported} rotinas, ${res.logsImported} check-ins$journalSuffix!")
            } catch (_: Exception) {
                showStatus("Formato de arquivo JSON incompatível.")
            }
        }
    }

    fun shareBackupToGoogleDrive() {
        viewModelScope.launch {
            val json = withContext(Dispatchers.IO) {
                repository.exportFullBackupJson(uiState.value.settings.themeType)
            }
            val file = BackupManager.shareSaveFileToGoogleDrive(appContext, json)
            if (file != null) {
                showStatus("☁️ Escolha o Google Drive para enviar seu save atualizado!")
            } else {
                showStatus("Cópia local salva, mas não foi possível abrir o compartilhamento.")
            }
        }
    }

    fun saveQuickInternalBackup() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.exportFullBackupJson(uiState.value.settings.themeType)
            }
            showStatus("⚡ Save rápido salvo na memória interna do aparelho!")
        }
    }

    fun restoreQuickInternalBackup() {
        viewModelScope.launch {
            val json = withContext(Dispatchers.IO) {
                BackupManager.loadInternalQuickCopy(appContext)
            }
            if (json.isNullOrBlank()) {
                showStatus("Nenhum save rápido encontrado no aparelho ainda.")
                return@launch
            }
            val res = withContext(Dispatchers.IO) {
                repository.importFullBackupJson(json, replaceExisting = true)
            }
            showStatus("⚡ Save rápido restaurado (${res.routinesImported} rotinas)!")
        }
    }

    fun togglePomodoroExpanded() {
        pomodoroStateFlow.update { it.copy(isExpanded = !it.isExpanded) }
    }

    fun selectPomodoroPhase(phase: PomodoroPhase) {
        pomodoroTickerJob?.cancel()
        val durationSec = phase.defaultMinutes * 60
        pomodoroStateFlow.update {
            it.copy(
                phase = phase,
                totalSeconds = durationSec,
                remainingSeconds = durationSec,
                isRunning = false
            )
        }
    }

    fun setPomodoroCustomDurationSeconds(seconds: Int) {
        pomodoroTickerJob?.cancel()
        val safeSeconds = seconds.coerceAtLeast(3)
        pomodoroStateFlow.update {
            it.copy(
                totalSeconds = safeSeconds,
                remainingSeconds = safeSeconds,
                isRunning = false
            )
        }
    }

    fun togglePomodoroStartPause() {
        val current = pomodoroStateFlow.value
        if (current.isRunning) {
            pomodoroTickerJob?.cancel()
            pomodoroStateFlow.update { it.copy(isRunning = false) }
        } else {
            val startSecs = if (current.remainingSeconds <= 0) current.totalSeconds else current.remainingSeconds
            pomodoroStateFlow.update {
                it.copy(
                    remainingSeconds = startSecs,
                    isRunning = true,
                    isExpanded = true,
                    visualAlert = null
                )
            }
            startPomodoroTicker()
        }
    }

    fun resetPomodoroTimer() {
        pomodoroTickerJob?.cancel()
        pomodoroStateFlow.update {
            it.copy(
                remainingSeconds = it.totalSeconds,
                isRunning = false
            )
        }
    }

    fun completePomodoroIntervalNow() {
        pomodoroTickerJob?.cancel()
        onPomodoroIntervalFinished()
    }

    fun linkRoutineToPomodoro(routine: RoutineEntity?, autoStartFocus: Boolean = false) {
        if (routine == null) {
            pomodoroStateFlow.update {
                it.copy(
                    linkedRoutineId = null,
                    linkedRoutineTitle = null
                )
            }
            showStatus("Pomodoro em modo de foco livre.")
            return
        }

        val shouldSwitchToFocus = pomodoroStateFlow.value.phase != PomodoroPhase.FOCUS
        val focusDuration = PomodoroPhase.FOCUS.defaultMinutes * 60
        pomodoroStateFlow.update { state ->
            state.copy(
                phase = PomodoroPhase.FOCUS,
                totalSeconds = if (shouldSwitchToFocus) focusDuration else state.totalSeconds,
                remainingSeconds = if (shouldSwitchToFocus) focusDuration else state.remainingSeconds,
                linkedRoutineId = routine.id,
                linkedRoutineTitle = routine.title,
                isExpanded = true,
                visualAlert = null
            )
        }
        if (autoStartFocus && !pomodoroStateFlow.value.isRunning) {
            togglePomodoroStartPause()
            showStatus("🍅 Foco iniciado em '${routine.title}'!")
        } else {
            showStatus("🍅 Tarefa '${routine.title}' vinculada ao Pomodoro!")
        }
    }

    fun dismissPomodoroVisualAlert() {
        pomodoroStateFlow.update { it.copy(visualAlert = null) }
    }

    fun completeLinkedRoutineFromPomodoroAlert(routineId: Long) {
        val routine = uiState.value.routines.firstOrNull { it.id == routineId }
        val alert = pomodoroStateFlow.value.visualAlert
        if (routine != null) {
            markRoutineDoneNow(routine, note = "Concluída após sessão de foco Pomodoro")
        }
        val nextPhase = if (alert != null && alert.completedCyclesToday > 0 && alert.completedCyclesToday % 4 == 0) {
            PomodoroPhase.LONG_BREAK
        } else {
            PomodoroPhase.SHORT_BREAK
        }
        pomodoroStateFlow.update { it.copy(visualAlert = null) }
        selectPomodoroPhase(nextPhase)
        togglePomodoroStartPause()
    }

    private fun startPomodoroTicker() {
        pomodoroTickerJob?.cancel()
        pomodoroTickerJob = viewModelScope.launch {
            while (pomodoroStateFlow.value.isRunning && pomodoroStateFlow.value.remainingSeconds > 0) {
                delay(1000L)
                val nextRemaining = (pomodoroStateFlow.value.remainingSeconds - 1).coerceAtLeast(0)
                pomodoroStateFlow.update { it.copy(remainingSeconds = nextRemaining) }
                if (nextRemaining <= 0) {
                    onPomodoroIntervalFinished()
                    break
                }
            }
        }
    }

    private fun onPomodoroIntervalFinished() {
        val finishedState = pomodoroStateFlow.value
        val wasFocus = finishedState.phase == PomodoroPhase.FOCUS
        val newCyclesCount = if (wasFocus) {
            finishedState.completedFocusCyclesToday + 1
        } else {
            finishedState.completedFocusCyclesToday
        }

        val alert = if (wasFocus) {
            val taskMention = finishedState.linkedRoutineTitle?.let { " em '$it'" } ?: ""
            val isLongBreakNext = newCyclesCount % 4 == 0
            PomodoroVisualAlert(
                completedPhase = PomodoroPhase.FOCUS,
                completedCyclesToday = newCyclesCount,
                linkedRoutineId = finishedState.linkedRoutineId,
                linkedRoutineTitle = finishedState.linkedRoutineTitle,
                headline = "Sessão de Foco Concluída!$taskMention",
                subtitle = if (isLongBreakNext) {
                    "Você completou $newCyclesCount blocos hoje! Hora de uma Pausa Longa (15 min)."
                } else {
                    "Ciclo #$newCyclesCount finalizado com sucesso! Que tal uma Pausa Curta (5 min)?"
                },
                wellnessTip = "💧 Lembrete Pixel: Olhe para longe da tela por 20 segundos, beba água e solte os ombros."
            )
        } else {
            PomodoroVisualAlert(
                completedPhase = finishedState.phase,
                completedCyclesToday = newCyclesCount,
                linkedRoutineId = finishedState.linkedRoutineId,
                linkedRoutineTitle = finishedState.linkedRoutineTitle,
                headline = "${finishedState.phase.title} Finalizada!",
                subtitle = "Energia renovada! Quando quiser, inicie o próximo bloco tranquilo de foco.",
                wellnessTip = "✨ Respire fundo: escolha uma única tarefa por vez e mantenha um ritmo gentil."
            )
        }

        pomodoroStateFlow.update {
            it.copy(
                isRunning = false,
                remainingSeconds = it.totalSeconds,
                isExpanded = true,
                completedFocusCyclesToday = newCyclesCount,
                visualAlert = alert
            )
        }

        // Trigger pixel art confetti & system notification
        confettiTriggerFlow.value = confettiTriggerFlow.value + 1
        val linkedSprite = finishedState.linkedRoutineId?.let { id ->
            uiState.value.routines.firstOrNull { it.id == id }?.sprite
        } ?: com.example.ui.components.PixelSpriteType.BOOK_FOCUS
        val currentSettings = uiState.value.settings
        NotificationHelper.showPomodoroIntervalFinishedNotification(
            context = appContext,
            title = "🍅 ${alert.headline}",
            message = "${alert.subtitle} ${alert.wellnessTip}",
            spriteType = linkedSprite,
            themeType = currentSettings.themeType,
            heroArtPreference = currentSettings.heroArtPreference
        )
    }

    fun clearStatusMessage() {
        statusMessageFlow.value = null
    }

    private fun showStatus(msg: String) {
        statusMessageFlow.value = msg
    }

    private fun buildStreakMedalSummary(
        routines: List<RoutineEntity>,
        logs: List<CheckInLogEntity>,
        todayKey: String
    ): StreakMedalSummary {
        return computeStreakMedalSummary(routines, logs, todayKey)
    }

    companion object {
        fun computeStreakMedalSummary(
            routines: List<RoutineEntity>,
            logs: List<CheckInLogEntity>,
            todayKey: String = DateUtils.todayDateKey()
        ): StreakMedalSummary {
            val enabledRoutines = routines.filter { it.isEnabled }
            val completedLogsByDate = logs
                .filter { it.status == CheckInStatus.COMPLETED }
                .groupBy { it.dateKey }

            fun evaluateDay(dateKey: String): Pair<Int, com.example.ui.components.PixelMedalTier> {
                val isoDay = DateUtils.isoDayOfWeekForDateKey(dateKey)
                val activeForDay = enabledRoutines.filter { isoDay in it.activeDays }
                val logsForDay = completedLogsByDate[dateKey].orEmpty()

                if (activeForDay.isEmpty()) {
                    return if (logsForDay.isNotEmpty()) {
                        100 to com.example.ui.components.PixelMedalTier.GOLD
                    } else {
                        0 to com.example.ui.components.PixelMedalTier.NONE
                    }
                }

                val targetTotal = activeForDay.sumOf { it.targetCountPerDay }.coerceAtLeast(1)
                val countByRoutine = logsForDay.groupingBy { it.routineId }.eachCount()
                val doneFromActive = activeForDay.sumOf { r ->
                    (countByRoutine[r.id] ?: 0).coerceAtMost(r.targetCountPerDay)
                }
                val effectiveDone = maxOf(doneFromActive, logsForDay.size.coerceAtMost(targetTotal))
                val percent = ((effectiveDone * 100f) / targetTotal.toFloat()).toInt().coerceIn(0, 100)
                val medal = com.example.ui.components.PixelMedalTier.fromCompletionPercent(
                    percent = percent,
                    hasActiveRoutines = true
                )
                return percent to medal
            }

            val todayIso = DateUtils.isoDayOfWeekForDateKey(todayKey)
            val todayActive = enabledRoutines.filter { todayIso in it.activeDays }
            val todayLogs = completedLogsByDate[todayKey].orEmpty()
            val todayCountByRoutine = todayLogs.groupingBy { it.routineId }.eachCount()
            val todayTarget = todayActive.sumOf { it.targetCountPerDay }
            val todayDone = todayActive.sumOf { r ->
                (todayCountByRoutine[r.id] ?: 0).coerceAtMost(r.targetCountPerDay)
            }
            val (todayPercent, todayMedal) = evaluateDay(todayKey)

            // Evaluate last 30 days for streak & best streak
            val last30Offsets = (-29..0).toList()
            val dayEvaluations = last30Offsets.map { offset ->
                val key = DateUtils.dateKeyForOffsetDays(offset)
                val (pct, medal) = evaluateDay(key)
                Triple(key, pct, medal)
            }

            // Consecutive streak of medal days (>= 50% completion) up to today (or yesterday if today is still in progress)
            var currentStreak = 0
            val reversed = dayEvaluations.asReversed()
            for ((idx, item) in reversed.withIndex()) {
                val medal = item.third
                if (medal != com.example.ui.components.PixelMedalTier.NONE) {
                    currentStreak++
                } else if (idx == 0) {
                    // Today hasn't reached 50% yet; check if yesterday had an active streak
                    continue
                } else {
                    break
                }
            }

            // Consecutive Gold streak (100% completion)
            var goldStreak = 0
            for ((idx, item) in reversed.withIndex()) {
                val medal = item.third
                if (medal == com.example.ui.components.PixelMedalTier.GOLD) {
                    goldStreak++
                } else if (idx == 0) {
                    continue
                } else {
                    break
                }
            }

            // Best streak in the 30-day window
            var bestStreak = 0
            var running = 0
            for (item in dayEvaluations) {
                if (item.third != com.example.ui.components.PixelMedalTier.NONE) {
                    running++
                    if (running > bestStreak) bestStreak = running
                } else {
                    running = 0
                }
            }

            val recent7 = dayEvaluations.takeLast(7).map { (key, pct, medal) ->
                DayMedalHistoryItem(
                    dateKey = key,
                    dayShortLabel = DateUtils.formatDayShortPt(key),
                    completionPercent = pct,
                    medal = medal
                )
            }

            return StreakMedalSummary(
                hasActiveRoutinesToday = todayActive.isNotEmpty(),
                todayCompletedCount = todayDone,
                todayTargetCount = todayTarget,
                todayCompletionPercent = todayPercent,
                todayMedal = todayMedal,
                currentStreakDays = currentStreak,
                goldStreakDays = goldStreak,
                bestStreakDays = maxOf(bestStreak, currentStreak),
                recent7DaysMedals = recent7
            )
        }
    }

    private fun buildWeeklyReport(
        routines: List<RoutineEntity>,
        logs: List<CheckInLogEntity>
    ): WeeklyReportSummary {
        val last7Keys = (-6..0).map { offset -> DateUtils.dateKeyForOffsetDays(offset) }
        val weekLogs = logs.filter { it.dateKey in last7Keys }

        val dayBars = last7Keys.map { key ->
            val logsForDay = weekLogs.filter { it.dateKey == key }
            DayReportBar(
                dateKey = key,
                dayShortLabel = DateUtils.formatDayShortPt(key),
                completedCount = logsForDay.count { it.status == CheckInStatus.COMPLETED },
                snoozedCount = logsForDay.count { it.status == CheckInStatus.SNOOZED }
            )
        }

        val completedWeek = weekLogs.filter { it.status == CheckInStatus.COMPLETED }
        val categoryBreakdown = RoutineCategory.entries.map { cat ->
            val catRoutines = routines.filter { it.category == cat && it.isEnabled }
            val weeklyTarget = catRoutines.sumOf { r ->
                r.targetCountPerDay * r.activeDays.size.coerceAtLeast(1)
            }.coerceAtLeast(if (catRoutines.isNotEmpty()) 7 else 1)
            val done = completedWeek.count { it.categorySnapshot == cat.name }
            CategoryReportItem(
                category = cat,
                completedCount = done,
                targetTotal = weeklyTarget
            )
        }

        val morningCount = completedWeek.count {
            val h = Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.HOUR_OF_DAY)
            h in 5..11
        }
        val afternoonCount = completedWeek.count {
            val h = Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.HOUR_OF_DAY)
            h in 12..17
        }
        val eveningCount = completedWeek.count {
            val h = Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.HOUR_OF_DAY)
            h >= 18 || h < 5
        }

        val bestPeriod = when {
            morningCount >= afternoonCount && morningCount >= eveningCount -> "Manhã (06h–12h)"
            afternoonCount >= morningCount && afternoonCount >= eveningCount -> "Tarde (12h–18h)"
            else -> "Noite (18h–23h)"
        }

        return WeeklyReportSummary(
            days = dayBars,
            categoryBreakdown = categoryBreakdown,
            totalCompletedWeek = completedWeek.size,
            quickOkCountWeek = completedWeek.count { it.source == CheckInSource.NOTIFICATION_OK },
            alarmDismissedCountWeek = completedWeek.count { it.source == CheckInSource.ALARM_DISMISSED },
            bestPeriodOfDay = bestPeriod
        )
    }

    private fun buildMonthlyReport(
        routines: List<RoutineEntity>,
        logs: List<CheckInLogEntity>
    ): MonthlyReportSummary {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val monthZero = now.get(Calendar.MONTH)
        val monthCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZero)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val totalDaysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val monthPrefix = String.format(java.util.Locale.US, "%04d-%02d-", year, monthZero + 1)

        val monthLogs = logs.filter { it.dateKey.startsWith(monthPrefix) }
        val completedMonth = monthLogs.filter { it.status == CheckInStatus.COMPLETED }

        val dailyCounts = (1..totalDaysInMonth).map { day ->
            val key = DateUtils.dateKeyForYmd(year, monthZero, day)
            val count = completedMonth.count { it.dateKey == key }
            day to count
        }

        val activeDaysCount = dailyCounts.count { it.second > 0 }

        val weekRanges = listOf(
            Triple("SEM 1", 1, 7),
            Triple("SEM 2", 8, 14),
            Triple("SEM 3", 15, 21),
            Triple("SEM 4", 22, 28),
            Triple("SEM 5", 29, totalDaysInMonth)
        ).filter { it.second <= totalDaysInMonth }

        val weekBars = weekRanges.map { (label, startDay, endDay) ->
            val keysInRange = (startDay..endDay).map { d ->
                DateUtils.dateKeyForYmd(year, monthZero, d)
            }.toSet()
            val logsInBucket = monthLogs.filter { it.dateKey in keysInRange }
            MonthWeekBar(
                weekLabel = label,
                rangeLabel = String.format(java.util.Locale.US, "%02d–%02d", startDay, endDay),
                completedCount = logsInBucket.count { it.status == CheckInStatus.COMPLETED },
                snoozedCount = logsInBucket.count { it.status == CheckInStatus.SNOOZED }
            )
        }

        val categoryBreakdownMonth = RoutineCategory.entries.map { cat ->
            val catRoutines = routines.filter { it.category == cat && it.isEnabled }
            val monthlyTarget = catRoutines.sumOf { r ->
                r.targetCountPerDay * r.activeDays.size.coerceAtLeast(1) * 4
            }.coerceAtLeast(if (catRoutines.isNotEmpty()) 20 else 1)
            val done = completedMonth.count { it.categorySnapshot == cat.name }
            CategoryReportItem(
                category = cat,
                completedCount = done,
                targetTotal = monthlyTarget
            )
        }

        return MonthlyReportSummary(
            monthTitle = DateUtils.formatMonthYearPt(year, monthZero),
            totalCompletedMonth = completedMonth.size,
            activeDaysInMonth = activeDaysCount,
            totalDaysInMonth = totalDaysInMonth,
            quickOkCountMonth = completedMonth.count { it.source == CheckInSource.NOTIFICATION_OK },
            alarmDismissedCountMonth = completedMonth.count { it.source == CheckInSource.ALARM_DISMISSED },
            weeksOfMonth = weekBars,
            dailyCompletedCounts = dailyCounts,
            categoryBreakdownMonth = categoryBreakdownMonth
        )
    }
}

private data class PromptAndStatusBundle(
    val statusMessage: String?,
    val confettiTriggerId: Int,
    val pomodoroState: PomodoroState,
    val pendingMoodPromptRoutine: RoutineEntity?
)

private data class ExtraUiBundle(
    val calState: CalendarMonthState,
    val editor: Pair<Boolean, RoutineEntity?>,
    val calendarEvents: List<DeviceCalendarEvent>,
    val statusMessage: String?,
    val confettiTriggerId: Int,
    val pomodoroState: PomodoroState,
    val pendingMoodPromptRoutine: RoutineEntity? = null,
    val journalEntries: List<JournalEntryEntity>,
    val selectedJournalDateKey: String,
    val isAiLoading: Boolean = false,
    val aiLoadingEntryId: Long? = null
)
