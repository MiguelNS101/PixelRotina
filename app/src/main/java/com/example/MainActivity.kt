package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alarm.AlarmScheduler
import com.example.alarm.NotificationHelper
import com.example.data.CalendarSyncHelper
import com.example.data.DateUtils
import com.example.data.FrequencyType
import com.example.data.JournalEntryType
import com.example.ui.AppTab
import com.example.ui.RoutineViewModel
import com.example.ui.components.PixelBadge
import com.example.ui.components.PixelConfettiOverlay
import com.example.ui.components.PixelSpriteIcon
import com.example.ui.components.PixelSpriteType
import com.example.ui.components.RoutineEditorDialog
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsThemeBackupScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.screens.WeeklyCalendarScreen
import com.example.ui.screens.WeeklyReportScreen
import com.example.ui.theme.LocalPixelPalette
import com.example.ui.theme.PixelRotinaTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val pendingOpenTabState = mutableStateOf<AppTab?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        parseIntentTab(intent)
        setContent {
            val viewModel: RoutineViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val pendingTab by pendingOpenTabState

            LaunchedEffect(pendingTab) {
                val target = pendingTab ?: return@LaunchedEffect
                if (target == AppTab.JOURNAL) {
                    viewModel.selectJournalDate(DateUtils.todayDateKey())
                }
                viewModel.selectTab(target)
                pendingOpenTabState.value = null
            }

            PixelRotinaTheme(themeType = uiState.settings.themeType) {
                PixelRotinaApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseIntentTab(intent)
    }

    private fun parseIntentTab(intent: Intent?) {
        val tabName = intent?.getStringExtra(AlarmScheduler.EXTRA_OPEN_TAB) ?: return
        val tab = AppTab.entries.firstOrNull { it.name == tabName } ?: return
        pendingOpenTabState.value = tab
    }
}

@Composable
fun PixelRotinaApp(
    viewModel: RoutineViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = LocalPixelPalette.current

    var hasNotifPerm by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }
    var hasCalendarPerm by remember {
        mutableStateOf(
            CalendarSyncHelper.hasReadCalendarPermission(context) &&
                CalendarSyncHelper.hasWriteCalendarPermission(context)
        )
    }
    var canExactAlarm by remember {
        mutableStateOf(AlarmScheduler.canScheduleExactAlarms(context))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPerm = granted
    }

    val calendarPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val readOk = result[Manifest.permission.READ_CALENDAR] == true
        val writeOk = result[Manifest.permission.WRITE_CALENDAR] == true
        hasCalendarPerm = readOk && writeOk
        if (readOk) {
            viewModel.refreshDeviceCalendarEvents()
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportBackupToUri(uri)
        }
    }

    var importReplaceMode by remember { mutableStateOf(false) }
    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importBackupFromUri(uri, replaceExisting = importReplaceMode)
        }
    }

    // Auto-clear status toast banner after 4 seconds
    LaunchedEffect(uiState.statusMessage) {
        if (uiState.statusMessage != null) {
            delay(4000)
            viewModel.clearStatusMessage()
        }
    }

    // BackHandler for non-Today tabs as required by navigation guidelines
    if (uiState.settings.hasCompletedOnboarding && uiState.selectedTab != AppTab.TODAY) {
        BackHandler {
            viewModel.selectTab(AppTab.TODAY)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = uiState.settings.hasCompletedOnboarding,
            transitionSpec = {
                (fadeIn(tween(320)) + scaleIn(initialScale = 0.96f, animationSpec = tween(320)))
                    .togetherWith(fadeOut(tween(220)))
            },
            label = "onboarding_to_app_transition"
        ) { onboardingDone ->
            if (!onboardingDone) {
                OnboardingScreen(
                    selectedTheme = uiState.settings.themeType,
                    selectedHeroArt = uiState.settings.heroArtPreference,
                    hasNotificationPermission = hasNotifPerm,
                    hasCalendarPermission = hasCalendarPerm,
                    onSelectTheme = { viewModel.selectTheme(it) },
                    onSelectHeroArt = { viewModel.selectHeroArtPreference(it) },
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            hasNotifPerm = true
                        }
                    },
                    onRequestCalendarPermission = {
                        calendarPermissionsLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_CALENDAR,
                                Manifest.permission.WRITE_CALENDAR
                            )
                        )
                    },
                    onFinishOnboarding = { viewModel.completeOnboarding() }
                )
            } else {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = palette.background,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    topBar = {
                        PixelTopBar(
                            activeRoutinesCount = uiState.todayActiveRoutines.size,
                            pomodoroRunningText = if (uiState.pomodoroState.isRunning) {
                                "🍅 ${uiState.pomodoroState.formattedTime}"
                            } else {
                                null
                            }
                        )
                    },
                    floatingActionButton = {
                        if (uiState.selectedTab != AppTab.JOURNAL) {
                            FloatingActionButton(
                                onClick = { viewModel.openCreateRoutineDialog(FrequencyType.SPECIFIC_TIME) },
                                containerColor = palette.accent,
                                contentColor = palette.onAccent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .border(2.5.dp, palette.pixelBorder, RoundedCornerShape(8.dp))
                                    .testTag("fab_add_routine")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Criar nova rotina"
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "NOVA ROTINA",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = palette.surfaceElevated,
                            tonalElevation = 0.dp,
                            modifier = Modifier.border(width = 2.dp, color = palette.pixelBorder)
                        ) {
                            val items = listOf(
                                Triple(AppTab.TODAY, stringResource(R.string.tab_today), Icons.Filled.Today to Icons.Outlined.Today),
                                Triple(AppTab.WEEKLY_CALENDAR, stringResource(R.string.tab_calendar), Icons.Filled.CalendarMonth to Icons.Outlined.CalendarMonth),
                                Triple(AppTab.JOURNAL, stringResource(R.string.tab_journal), Icons.Filled.AutoStories to Icons.Outlined.AutoStories),
                                Triple(AppTab.WEEKLY_REPORT, stringResource(R.string.tab_report), Icons.Filled.Analytics to Icons.Outlined.Analytics),
                                Triple(AppTab.SETTINGS_BACKUP, stringResource(R.string.tab_settings), Icons.Filled.Settings to Icons.Outlined.Settings)
                            )

                            items.forEach { (tab, label, icons) ->
                                val selected = uiState.selectedTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) icons.first else icons.second,
                                            contentDescription = label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = palette.onPrimary,
                                        selectedTextColor = palette.primary,
                                        indicatorColor = palette.primary,
                                        unselectedIconColor = palette.textSecondary,
                                        unselectedTextColor = palette.textSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedVisibility(visible = uiState.statusMessage != null) {
                            uiState.statusMessage?.let { msg ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(palette.secondary)
                                        .border(2.dp, palette.pixelBorder, RoundedCornerShape(6.dp))
                                        .clickable { viewModel.clearStatusMessage() }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                        .testTag("status_banner")
                                ) {
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = palette.onSecondary
                                    )
                                }
                            }
                        }

                        // Directional slide + fade screen transition between tabs
                        AnimatedContent(
                            targetState = uiState.selectedTab,
                            transitionSpec = {
                                val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                                (slideInHorizontally(
                                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                                    initialOffsetX = { fullWidth -> direction * (fullWidth / 5) }
                                ) + fadeIn(tween(240)))
                                    .togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                                            targetOffsetX = { fullWidth -> -direction * (fullWidth / 6) }
                                        ) + fadeOut(tween(180))
                                    )
                                    .using(SizeTransform(clip = false))
                            },
                            modifier = Modifier.weight(1f),
                            label = "tab_screen_transition"
                        ) { currentTab ->
                            when (currentTab) {
                                AppTab.TODAY -> TodayScreen(
                                    uiState = uiState,
                                    onCheckInRoutine = { viewModel.markRoutineDoneNow(it) },
                                    onGentleReschedule = { routine, mins -> viewModel.gentleRescheduleRoutine(routine, mins) },
                                    onEditRoutine = { viewModel.openEditRoutineDialog(it) },
                                    onTestAlert = { viewModel.triggerTestAlertForRoutine(it) },
                                    onCreateNewRoutine = { freq -> viewModel.openCreateRoutineDialog(freq) },
                                    onTogglePomodoroExpanded = { viewModel.togglePomodoroExpanded() },
                                    onSelectPomodoroPhase = { viewModel.selectPomodoroPhase(it) },
                                    onSetPomodoroCustomDuration = { viewModel.setPomodoroCustomDurationSeconds(it) },
                                    onTogglePomodoroStartPause = { viewModel.togglePomodoroStartPause() },
                                    onResetPomodoroTimer = { viewModel.resetPomodoroTimer() },
                                    onCompletePomodoroIntervalNow = { viewModel.completePomodoroIntervalNow() },
                                    onLinkRoutineToPomodoro = { routine, autoStart ->
                                        viewModel.linkRoutineToPomodoro(routine, autoStart)
                                    },
                                    onDismissPomodoroAlert = { viewModel.dismissPomodoroVisualAlert() },
                                    onCompleteLinkedRoutineFromPomodoroAlert = { routineId ->
                                        viewModel.completeLinkedRoutineFromPomodoroAlert(routineId)
                                    },
                                    onSaveQuickMoodEnergy = { mood, energy, tags ->
                                        viewModel.saveJournalEntry(
                                            entryType = JournalEntryType.MOOD_CHECKIN,
                                            moodScore = mood,
                                            energyScore = energy,
                                            contextTags = tags,
                                            content = "",
                                            unlockAtMillis = null,
                                            dateKey = uiState.todayDateKey
                                        )
                                    },
                                    onSavePostRoutineMoodEnergy = { routine, mood, energy ->
                                        viewModel.savePostRoutineMoodEnergy(routine, mood, energy)
                                    },
                                    onDismissPostRoutineMoodPrompt = {
                                        viewModel.dismissPostRoutineMoodPrompt()
                                    },
                                    onOpenJournalTab = {
                                        viewModel.selectJournalDate(uiState.todayDateKey)
                                        viewModel.selectTab(AppTab.JOURNAL)
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )

                                AppTab.WEEKLY_CALENDAR -> WeeklyCalendarScreen(
                                    uiState = uiState,
                                    hasCalendarPermission = hasCalendarPerm,
                                    onRequestCalendarPermission = {
                                        calendarPermissionsLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.READ_CALENDAR,
                                                Manifest.permission.WRITE_CALENDAR
                                            )
                                        )
                                    },
                                    onSelectDateKey = { viewModel.selectCalendarDate(it) },
                                    onChangeMonth = { delta -> viewModel.changeCalendarMonth(delta) },
                                    onJumpToToday = { viewModel.jumpToCurrentMonthAndToday() },
                                    onToggleRoutineEnabled = { r, enabled -> viewModel.toggleRoutineEnabled(r, enabled) },
                                    onSyncRoutineToCalendar = { routine ->
                                        if (!hasCalendarPerm) {
                                            calendarPermissionsLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.READ_CALENDAR,
                                                    Manifest.permission.WRITE_CALENDAR
                                                )
                                            )
                                        }
                                        viewModel.syncRoutineToAndroidCalendar(routine)
                                    },
                                    onEditRoutine = { viewModel.openEditRoutineDialog(it) },
                                    onRefreshCalendar = { viewModel.refreshDeviceCalendarEvents() },
                                    onOpenJournalForDate = { dateKey ->
                                        viewModel.selectJournalDate(dateKey)
                                        viewModel.selectTab(AppTab.JOURNAL)
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )

                                AppTab.JOURNAL -> JournalScreen(
                                    uiState = uiState,
                                    onSelectDate = { viewModel.selectJournalDate(it) },
                                    onShiftDate = { viewModel.shiftJournalDate(it) },
                                    onSaveEntry = { type, mood, energy, tags, content, unlockAt ->
                                        viewModel.saveJournalEntry(
                                            entryType = type,
                                            moodScore = mood,
                                            energyScore = energy,
                                            contextTags = tags,
                                            content = content,
                                            unlockAtMillis = unlockAt
                                        )
                                    },
                                    onDeleteEntry = { viewModel.deleteJournalEntry(it) },
                                    onToggleStealthMode = { viewModel.toggleStealthMode() },
                                    onSetStealthMode = { viewModel.setStealthMode(it) },
                                    onRequestAiReaction = { entry, mode ->
                                        viewModel.requestAiReactionForEntry(entry, mode)
                                    },
                                    onRequestWeeklyPatchNotes = { viewModel.requestWeeklyPatchNotes() },
                                    onOpenSuggestedRoutine = { json ->
                                        viewModel.openSuggestedRoutineInEditor(json)
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )

                                AppTab.WEEKLY_REPORT -> WeeklyReportScreen(
                                    uiState = uiState,
                                    modifier = Modifier.fillMaxSize()
                                )

                                AppTab.SETTINGS_BACKUP -> SettingsThemeBackupScreen(
                                    uiState = uiState,
                                    hasNotificationPermission = hasNotifPerm,
                                    canScheduleExactAlarms = canExactAlarm,
                                    hasCalendarPermission = hasCalendarPerm,
                                    onRequestNotificationPermission = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    },
                                    onOpenExactAlarmSettings = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            try {
                                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                    data = Uri.parse("package:${context.packageName}")
                                                }
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                            }
                                        }
                                        canExactAlarm = AlarmScheduler.canScheduleExactAlarms(context)
                                    },
                                    onRequestCalendarPermission = {
                                        calendarPermissionsLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.READ_CALENDAR,
                                                Manifest.permission.WRITE_CALENDAR
                                            )
                                        )
                                    },
                                    onSelectTheme = { viewModel.selectTheme(it) },
                                    onSelectHeroArt = { viewModel.selectHeroArtPreference(it) },
                                    onSelectSnoozeMinutes = { viewModel.updateSnoozeMinutes(it) },
                                    onShareToGoogleDrive = { viewModel.shareBackupToGoogleDrive() },
                                    onExportJsonFile = {
                                        exportJsonLauncher.launch("pixelrotina_save_${DateUtils.todayDateKey()}.json")
                                    },
                                    onImportJsonFile = { replace ->
                                        importReplaceMode = replace
                                        importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                    },
                                    onSaveQuickLocalBackup = { viewModel.saveQuickInternalBackup() },
                                    onRestoreQuickLocalBackup = { viewModel.restoreQuickInternalBackup() },
                                    onStopRingingAlarm = { viewModel.stopRingingAlarmNow() },
                                    onReopenOnboarding = { viewModel.reopenOnboarding() },
                                    onUpdateJournalPin = { viewModel.updateJournalPin(it) },
                                    onToggleStealthMode = { viewModel.toggleStealthMode() },
                                    onUpdateGeminiApiKey = { viewModel.updateGeminiApiKey(it) },
                                    onUpdateGeminiModelName = { viewModel.updateGeminiModelName(it) },
                                    onToggleDailyCheckInNotif = { viewModel.setDailyCheckInNotificationEnabled(it) },
                                    onUpdateDailyCheckInTime = { h, m -> viewModel.setDailyCheckInNotificationTime(h, m) },
                                    onTestDailyCheckInNotif = { viewModel.triggerTestDailyCheckInNotification() },
                                    onTogglePromptMoodAfterRoutine = { viewModel.setPromptMoodAfterRoutineCheckIn(it) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }

        // Full-screen 8-bit Pixel Art Confetti & Sparkle Particle Burst on task completion
        PixelConfettiOverlay(
            triggerId = uiState.confettiTriggerId
        )
    }

    if (uiState.isEditorVisible && uiState.editingRoutine != null) {
        RoutineEditorDialog(
            initialRoutine = uiState.editingRoutine!!,
            onDismiss = { viewModel.closeRoutineDialog() },
            onSave = { viewModel.saveRoutine(it) },
            onDelete = { viewModel.deleteRoutine(it) }
        )
    }
}

@Composable
private fun PixelTopBar(
    activeRoutinesCount: Int,
    pomodoroRunningText: String? = null
) {
    val palette = LocalPixelPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surfaceElevated)
            .border(width = 2.dp, color = palette.pixelBorder)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelSpriteIcon(
                spriteType = PixelSpriteType.LEAF_WELLNESS,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "PIXELROTINA",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.primary
                )
                Text(
                    text = "Bem-estar • Rotina • Sem Pressão",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pomodoroRunningText != null) {
                PixelBadge(
                    text = pomodoroRunningText,
                    containerColor = palette.alarmCoral,
                    contentColor = palette.onPrimary
                )
            }
            PixelBadge(
                text = "$activeRoutinesCount ATIVAS HOJE",
                containerColor = palette.surface,
                contentColor = palette.textPrimary
            )
        }
    }
}
