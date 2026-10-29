package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmRingingService
import com.example.alarm.AlarmScheduler
import com.example.alarm.NotificationHelper
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

enum class AppTab {
    TODAY,
    WEEKLY_CALENDAR,
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
    val weeklyReport: WeeklyReportSummary = WeeklyReportSummary(),
    val monthlyReport: MonthlyReportSummary = MonthlyReportSummary()
) {
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
                pomodoroStateFlow
            ) { msg, confetti, pomodoro -> Triple(msg, confetti, pomodoro) }
        ) { calState, editor, cal, msgConfettiPomodoro ->
            ExtraUiBundle(
                calState = calState,
                editor = editor,
                calendarEvents = cal,
                statusMessage = msgConfettiPomodoro.first,
                confettiTriggerId = msgConfettiPomodoro.second,
                pomodoroState = msgConfettiPomodoro.third
            )
        }
    ) { routines, logs, settings, tab, extra ->
        val todayKey = DateUtils.todayDateKey()
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
            weeklyReport = buildWeeklyReport(routines, logs),
            monthlyReport = buildMonthlyReport(routines, logs)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RoutineUiState()
    )

    init {
        NotificationHelper.ensureChannelsCreated(appContext)
        refreshDeviceCalendarEvents()
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
            showStatus("✨ Rotina '${saved.title}' salva e agendada!")
        }
    }

    fun toggleRoutineEnabled(routine: RoutineEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleRoutineEnabled(routine, enabled)
            val msg = if (enabled) "Ativada: ${routine.title}" else "Pausada: ${routine.title}"
            showStatus(msg)
        }
    }

    fun deleteRoutine(routine: RoutineEntity) {
        viewModelScope.launch {
            repository.deleteRoutine(routine)
            editorStateFlow.value = false to null
            showStatus("Rotina '${routine.title}' removida.")
        }
    }

    fun markRoutineDoneNow(routine: RoutineEntity, note: String = "") {
        viewModelScope.launch {
            AlarmRingingService.stopAlarm(appContext)
            NotificationHelper.cancelNotification(appContext, routine.id)
            repository.recordCheckIn(
                routine = routine,
                status = CheckInStatus.COMPLETED,
                source = CheckInSource.IN_APP,
                note = note.ifBlank { "Check-in confirmado no app" }
            )
            // Trigger 8-bit pixel confetti burst!
            confettiTriggerFlow.value = confettiTriggerFlow.value + 1
            showStatus("🎉 Boa! '${routine.title}' concluída!")
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
            showStatus("🎨 Tema alterado para ${themeType.title}!")
        }
    }

    fun selectHeroArtPreference(preference: HeroArtPreference) {
        viewModelScope.launch {
            prefsRepo.setHeroArtPreference(preference)
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
                showStatus("📂 Save restaurado: ${res.routinesImported} rotinas e ${res.logsImported} registros!")
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
        NotificationHelper.showPomodoroIntervalFinishedNotification(
            context = appContext,
            title = "🍅 ${alert.headline}",
            message = "${alert.subtitle} ${alert.wellnessTip}"
        )
    }

    fun clearStatusMessage() {
        statusMessageFlow.value = null
    }

    private fun showStatus(msg: String) {
        statusMessageFlow.value = msg
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

private data class ExtraUiBundle(
    val calState: CalendarMonthState,
    val editor: Pair<Boolean, RoutineEntity?>,
    val calendarEvents: List<DeviceCalendarEvent>,
    val statusMessage: String?,
    val confettiTriggerId: Int,
    val pomodoroState: PomodoroState
)
