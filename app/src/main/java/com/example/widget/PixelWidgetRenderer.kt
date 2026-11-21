package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.toArgb
import com.example.MainActivity
import com.example.R
import com.example.alarm.AlarmScheduler
import com.example.data.AppDatabase
import com.example.data.CheckInStatus
import com.example.data.DateUtils
import com.example.data.HeroArtPreference
import com.example.data.JournalCorrelationCalculator
import com.example.data.RoutineEntity
import com.example.data.UserPreferencesRepository
import com.example.ui.AppTab
import com.example.ui.RoutineViewModel
import com.example.ui.StreakMedalSummary
import com.example.ui.components.PixelMedalTier
import com.example.ui.components.createNotificationLargeBitmap
import com.example.ui.components.createPixelEnergyBitmap
import com.example.ui.components.createPixelMedalBitmap
import com.example.ui.components.createPixelMoodBitmap
import com.example.ui.components.createPixelProgressBarBitmap
import com.example.ui.components.createPixelStreakFlameBitmap
import com.example.ui.components.moodLabelPt
import com.example.ui.theme.PixelThemeType
import com.example.ui.theme.getPixelPalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

data class WidgetRoutineRow(
    val routine: RoutineEntity,
    val completedToday: Int,
    val targetToday: Int
) {
    val isDone: Boolean get() = completedToday >= targetToday
}

data class PixelWidgetSnapshot(
    val themeType: PixelThemeType = PixelThemeType.CATPPUCCIN_MOCHA,
    val heroArtPreference: HeroArtPreference = HeroArtPreference.AUTO,
    val streakSummary: StreakMedalSummary = StreakMedalSummary(),
    val todayRoutines: List<WidgetRoutineRow> = emptyList(),
    val todayAvgMood: Float? = null,
    val todayAvgEnergy: Float? = null,
    val todayMoodCount: Int = 0,
    val pendingMoodScore: Int? = null,
    val recentSavedMessage: String? = null
)

object PixelWidgetStateStore {
    private const val PREFS_NAME = "pixel_widget_state"
    private const val KEY_PENDING_MOOD = "pending_mood_score"
    private const val KEY_SAVED_MSG = "recent_saved_msg"
    private const val KEY_SAVED_TIME = "recent_saved_time"

    fun getPendingMood(context: Context): Int? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getInt(KEY_PENDING_MOOD, 0)
        return if (value in 1..5) value else null
    }

    fun setPendingMood(context: Context, moodScore: Int?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (moodScore != null && moodScore in 1..5) {
                putInt(KEY_PENDING_MOOD, moodScore)
            } else {
                remove(KEY_PENDING_MOOD)
            }
            apply()
        }
    }

    fun setRecentSavedMessage(context: Context, message: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_SAVED_MSG, message)
            .putLong(KEY_SAVED_TIME, System.currentTimeMillis())
            .apply()
    }

    fun getRecentSavedMessage(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedAt = prefs.getLong(KEY_SAVED_TIME, 0L)
        if (System.currentTimeMillis() - savedAt > 20_000L) return null
        return prefs.getString(KEY_SAVED_MSG, null)
    }
}

object PixelWidgetRenderer {

    @DrawableRes
    private fun resolveHeroArtDrawable(pref: HeroArtPreference): Int {
        return pref.resolveForHour().drawableRes ?: R.drawable.img_pixel_hero_banner
    }

    private fun openTabPendingIntent(context: Context, tab: AppTab, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmScheduler.EXTRA_OPEN_TAB, tab.name)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun widgetActionPendingIntent(
        context: Context,
        action: String,
        requestCode: Int,
        configure: Intent.() -> Unit = {}
    ): PendingIntent {
        val intent = Intent(context, PixelWidgetActionReceiver::class.java).apply {
            this.action = action
            configure()
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    suspend fun loadSnapshot(context: Context): PixelWidgetSnapshot {
        val appContext = context.applicationContext
        val dao = AppDatabase.getInstance(appContext).routineDao()
        val settings = UserPreferencesRepository(appContext).settingsFlow.first()
        val now = System.currentTimeMillis()
        val todayKey = DateUtils.todayDateKey(now)
        val calDow = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.DAY_OF_WEEK)
        val todayIsoDow = if (calDow == Calendar.SUNDAY) 7 else calDow - 1

        val allRoutines = dao.getAllRoutinesOnce()
        val allLogs = dao.getAllCheckInsOnce()
        val allJournal = dao.getAllJournalEntriesOnce()

        val streakSummary = RoutineViewModel.computeStreakMedalSummary(
            routines = allRoutines,
            logs = allLogs,
            todayKey = todayKey
        )

        val todayCompletedByRoutine = allLogs
            .filter { it.dateKey == todayKey && it.status == CheckInStatus.COMPLETED }
            .groupingBy { it.routineId }
            .eachCount()

        val activeToday = allRoutines
            .filter { it.isEnabled && todayIsoDow in it.activeDays }
            .map { routine ->
                WidgetRoutineRow(
                    routine = routine,
                    completedToday = todayCompletedByRoutine[routine.id] ?: 0,
                    targetToday = routine.targetCountPerDay.coerceAtLeast(1)
                )
            }
            // Show uncompleted tasks first, ordered by schedule time
            .sortedWith(
                compareBy<WidgetRoutineRow> { it.isDone }
                    .thenBy { it.routine.hour }
                    .thenBy { it.routine.minute }
            )

        val daySummary = JournalCorrelationCalculator.computeDaySummaryByDate(allJournal)[todayKey]

        return PixelWidgetSnapshot(
            themeType = settings.themeType,
            heroArtPreference = settings.heroArtPreference,
            streakSummary = streakSummary,
            todayRoutines = activeToday,
            todayAvgMood = daySummary?.avgMood,
            todayAvgEnergy = daySummary?.avgEnergy,
            todayMoodCount = daySummary?.entryCount ?: 0,
            pendingMoodScore = PixelWidgetStateStore.getPendingMood(appContext),
            recentSavedMessage = PixelWidgetStateStore.getRecentSavedMessage(appContext)
        )
    }

    /**
     * Widget 1 (2x2): Compact Streak & Pixel Medal full-art card.
     */
    fun buildStreakMedalRemoteViews(
        context: Context,
        snapshot: PixelWidgetSnapshot
    ): RemoteViews {
        val palette = getPixelPalette(snapshot.themeType)
        val accentArgb = palette.primary.toArgb()
        val greenArgb = palette.wellnessGreen.toArgb()
        val heroRes = resolveHeroArtDrawable(snapshot.heroArtPreference)
        val s = snapshot.streakSummary

        val moodPillText = if (snapshot.todayAvgMood != null || snapshot.todayAvgEnergy != null) {
            val m = snapshot.todayAvgMood?.let { String.format(Locale.US, "%.1f", it) } ?: "-"
            val e = snapshot.todayAvgEnergy?.let { String.format(Locale.US, "%.1f", it) } ?: "-"
            "H $m • E $e"
        } else {
            "TOQUE P/ ABRIR"
        }

        return RemoteViews(context.packageName, R.layout.widget_streak_medal).apply {
            setImageViewResource(R.id.widget_bg_art, heroRes)
            setInt(R.id.widget_accent_bar, "setColorFilter", accentArgb)
            setImageViewBitmap(
                R.id.widget_streak_icon,
                createPixelStreakFlameBitmap(isActive = s.currentStreakDays > 0, sizePx = 56)
            )
            setTextViewText(R.id.widget_streak_text, "${s.currentStreakDays}d")
            setTextViewText(R.id.widget_percent_badge, "${s.todayCompletionPercent}%")
            setImageViewBitmap(
                R.id.widget_medal_icon,
                createPixelMedalBitmap(
                    tier = s.todayMedal,
                    isUnlocked = s.todayMedal != PixelMedalTier.NONE,
                    sizePx = 96
                )
            )
            setTextViewText(R.id.widget_medal_label, s.todayMedal.shortLabelPt)
            setTextViewText(
                R.id.widget_tasks_summary,
                "${s.todayCompletedCount}/${s.todayTargetCount} rotinas"
            )
            setImageViewBitmap(
                R.id.widget_progress_bar,
                createPixelProgressBarBitmap(
                    percent = s.todayCompletionPercent,
                    widthPx = 280,
                    heightPx = 20,
                    fillColorArgb = greenArgb
                )
            )
            setTextViewText(R.id.widget_mood_pill, moodPillText)
            setOnClickPendingIntent(
                R.id.widget_root,
                openTabPendingIntent(context, AppTab.TODAY, 81_001)
            )
        }
    }

    /**
     * Widget 2 (4x1): Interactive 5-level Mood & Energy Check-in strip.
     */
    fun buildMoodQuickRemoteViews(
        context: Context,
        snapshot: PixelWidgetSnapshot
    ): RemoteViews {
        val palette = getPixelPalette(snapshot.themeType)
        val pendingMood = snapshot.pendingMoodScore
        val accentArgb = if (pendingMood != null) palette.wellnessGreen.toArgb() else palette.primary.toArgb()
        val heroRes = resolveHeroArtDrawable(snapshot.heroArtPreference)

        return RemoteViews(context.packageName, R.layout.widget_mood_quick).apply {
            setImageViewResource(R.id.widget_bg_art, heroRes)
            setInt(R.id.widget_accent_bar, "setColorFilter", accentArgb)
            bindMoodEnergyStrip(
                context = context,
                views = this,
                snapshot = snapshot,
                requestBase = 82_000
            )
            setOnClickPendingIntent(
                R.id.widget_mood_avg_badge,
                openTabPendingIntent(context, AppTab.JOURNAL, 82_099)
            )
        }
    }

    /**
     * Widget 3 (4x2): Today Hero Card + Interactive 3-Row Routine Checklist.
     */
    fun buildTodayCardRemoteViews(
        context: Context,
        snapshot: PixelWidgetSnapshot
    ): RemoteViews {
        val palette = getPixelPalette(snapshot.themeType)
        val heroArt = snapshot.heroArtPreference.resolveForHour()
        val heroRes = heroArt.drawableRes ?: R.drawable.img_pixel_hero_banner
        val s = snapshot.streakSummary

        return RemoteViews(context.packageName, R.layout.widget_today_card).apply {
            setImageViewResource(R.id.widget_bg_art, heroRes)
            setInt(R.id.widget_accent_bar, "setColorFilter", palette.primary.toArgb())
            bindHeaderAndRoutines(
                context = context,
                views = this,
                snapshot = snapshot,
                heroGreeting = heroArt.greeting,
                progressFillArgb = palette.wellnessGreen.toArgb(),
                requestBase = 83_000
            )
        }
    }

    /**
     * Widget 4 (4x4): Full Pixel Dashboard combining Hero Art, Streak/Medal, Routines,
     * 5-Level Mood/Energy Check-in, and Tab Shortcuts.
     */
    fun buildDashboardLargeRemoteViews(
        context: Context,
        snapshot: PixelWidgetSnapshot
    ): RemoteViews {
        val palette = getPixelPalette(snapshot.themeType)
        val heroArt = snapshot.heroArtPreference.resolveForHour()
        val heroRes = heroArt.drawableRes ?: R.drawable.img_pixel_hero_banner

        return RemoteViews(context.packageName, R.layout.widget_dashboard_large).apply {
            setImageViewResource(R.id.widget_bg_art, heroRes)
            setInt(R.id.widget_accent_bar, "setColorFilter", palette.primary.toArgb())
            bindHeaderAndRoutines(
                context = context,
                views = this,
                snapshot = snapshot,
                heroGreeting = heroArt.greeting,
                progressFillArgb = palette.wellnessGreen.toArgb(),
                requestBase = 84_000
            )
            bindMoodEnergyStrip(
                context = context,
                views = this,
                snapshot = snapshot,
                requestBase = 84_500
            )
            setOnClickPendingIntent(
                R.id.widget_tab_today,
                openTabPendingIntent(context, AppTab.TODAY, 84_901)
            )
            setOnClickPendingIntent(
                R.id.widget_tab_journal,
                openTabPendingIntent(context, AppTab.JOURNAL, 84_902)
            )
            setOnClickPendingIntent(
                R.id.widget_tab_report,
                openTabPendingIntent(context, AppTab.WEEKLY_REPORT, 84_903)
            )
        }
    }

    private fun bindHeaderAndRoutines(
        context: Context,
        views: RemoteViews,
        snapshot: PixelWidgetSnapshot,
        heroGreeting: String,
        progressFillArgb: Int,
        requestBase: Int
    ) {
        val s = snapshot.streakSummary
        views.setImageViewBitmap(
            R.id.widget_medal_icon,
            createPixelMedalBitmap(
                tier = s.todayMedal,
                isUnlocked = s.todayMedal != PixelMedalTier.NONE,
                sizePx = 72
            )
        )
        views.setTextViewText(R.id.widget_greeting, heroGreeting)
        views.setTextViewText(
            R.id.widget_progress_text,
            "${s.todayCompletedCount}/${s.todayTargetCount} feitas • ${s.todayCompletionPercent}% • ${s.todayMedal.shortLabelPt}"
        )
        views.setImageViewBitmap(
            R.id.widget_streak_icon,
            createPixelStreakFlameBitmap(isActive = s.currentStreakDays > 0, sizePx = 48)
        )
        views.setTextViewText(R.id.widget_streak_text, "${s.currentStreakDays}d")
        views.setImageViewBitmap(
            R.id.widget_progress_bar,
            createPixelProgressBarBitmap(
                percent = s.todayCompletionPercent,
                widthPx = 380,
                heightPx = 20,
                fillColorArgb = progressFillArgb
            )
        )
        views.setOnClickPendingIntent(
            R.id.widget_header_area,
            openTabPendingIntent(context, AppTab.TODAY, requestBase + 1)
        )
        views.setOnClickPendingIntent(
            R.id.widget_refresh_btn,
            widgetActionPendingIntent(
                context = context,
                action = PixelWidgetActionReceiver.ACTION_WIDGET_REFRESH,
                requestCode = requestBase + 2
            )
        )

        val rowIds = intArrayOf(R.id.widget_row_1, R.id.widget_row_2, R.id.widget_row_3)
        val iconIds = intArrayOf(R.id.widget_row_1_icon, R.id.widget_row_2_icon, R.id.widget_row_3_icon)
        val titleIds = intArrayOf(R.id.widget_row_1_title, R.id.widget_row_2_title, R.id.widget_row_3_title)
        val metaIds = intArrayOf(R.id.widget_row_1_meta, R.id.widget_row_2_meta, R.id.widget_row_3_meta)
        val okIds = intArrayOf(R.id.widget_row_1_ok, R.id.widget_row_2_ok, R.id.widget_row_3_ok)

        val pendingRoutines = snapshot.todayRoutines.filter { !it.isDone }
        val displayList = if (pendingRoutines.isNotEmpty()) {
            pendingRoutines.take(3)
        } else {
            emptyList()
        }

        if (snapshot.todayRoutines.isEmpty()) {
            views.setViewVisibility(R.id.widget_routines_container, View.GONE)
            views.setViewVisibility(R.id.widget_empty_message, View.VISIBLE)
            views.setTextViewText(
                R.id.widget_empty_message,
                "✨ Nenhuma rotina ativa hoje.\nToque para adicionar!"
            )
            views.setOnClickPendingIntent(
                R.id.widget_empty_message,
                openTabPendingIntent(context, AppTab.TODAY, requestBase + 5)
            )
            return
        }

        if (displayList.isEmpty()) {
            views.setViewVisibility(R.id.widget_routines_container, View.GONE)
            views.setViewVisibility(R.id.widget_empty_message, View.VISIBLE)
            views.setTextViewText(
                R.id.widget_empty_message,
                "🏆 DIA 100% COMPLETO!\nMedalha de Ouro conquistada!"
            )
            views.setOnClickPendingIntent(
                R.id.widget_empty_message,
                openTabPendingIntent(context, AppTab.TODAY, requestBase + 6)
            )
            return
        }

        views.setViewVisibility(R.id.widget_empty_message, View.GONE)
        views.setViewVisibility(R.id.widget_routines_container, View.VISIBLE)

        for (i in 0 until 3) {
            if (i < displayList.size) {
                val item = displayList[i]
                val routine = item.routine
                views.setViewVisibility(rowIds[i], View.VISIBLE)
                views.setImageViewBitmap(
                    iconIds[i],
                    routine.sprite.createNotificationLargeBitmap(64)
                )
                views.setTextViewText(titleIds[i], routine.title)
                val progressPart = if (item.targetToday > 1) {
                    " • ${item.completedToday}/${item.targetToday}"
                } else {
                    ""
                }
                views.setTextViewText(
                    metaIds[i],
                    "${routine.formattedScheduleSummary()}$progressPart"
                )
                views.setOnClickPendingIntent(
                    okIds[i],
                    widgetActionPendingIntent(
                        context = context,
                        action = PixelWidgetActionReceiver.ACTION_WIDGET_COMPLETE_ROUTINE,
                        requestCode = requestBase + 100 + i
                    ) {
                        putExtra(AlarmScheduler.EXTRA_ROUTINE_ID, routine.id)
                        putExtra(AlarmScheduler.EXTRA_ROUTINE_TITLE, routine.title)
                    }
                )
                views.setOnClickPendingIntent(
                    rowIds[i],
                    openTabPendingIntent(context, AppTab.TODAY, requestBase + 120 + i)
                )
            } else {
                views.setViewVisibility(rowIds[i], View.GONE)
            }
        }
    }

    private fun bindMoodEnergyStrip(
        context: Context,
        views: RemoteViews,
        snapshot: PixelWidgetSnapshot,
        requestBase: Int
    ) {
        val pendingMood = snapshot.pendingMoodScore
        val headerText = when {
            pendingMood != null -> "PASSO 2/2 • ENERGIA (H $pendingMood/5 ${moodLabelPt(pendingMood).uppercase()})"
            snapshot.recentSavedMessage != null -> snapshot.recentSavedMessage
            else -> "PASSO 1/2 • HUMOR AGORA (1–5)"
        }
        views.setTextViewText(R.id.widget_mood_header, headerText)

        val avgBadge = if (snapshot.todayAvgMood != null || snapshot.todayAvgEnergy != null) {
            val m = snapshot.todayAvgMood?.let { String.format(Locale.US, "%.1f", it) } ?: "-"
            val e = snapshot.todayAvgEnergy?.let { String.format(Locale.US, "%.1f", it) } ?: "-"
            "MÉDIA H $m • E $e"
        } else {
            "DIÁRIO ▶"
        }
        views.setTextViewText(R.id.widget_mood_avg_badge, avgBadge)

        if (pendingMood != null) {
            views.setViewVisibility(R.id.widget_mood_reset_btn, View.VISIBLE)
            views.setOnClickPendingIntent(
                R.id.widget_mood_reset_btn,
                widgetActionPendingIntent(
                    context = context,
                    action = PixelWidgetActionReceiver.ACTION_WIDGET_RESET_MOOD,
                    requestCode = requestBase + 90
                )
            )
        } else {
            views.setViewVisibility(R.id.widget_mood_reset_btn, View.GONE)
        }

        val btnIds = intArrayOf(
            R.id.widget_btn_level_1,
            R.id.widget_btn_level_2,
            R.id.widget_btn_level_3,
            R.id.widget_btn_level_4,
            R.id.widget_btn_level_5
        )
        val iconIds = intArrayOf(
            R.id.widget_icon_level_1,
            R.id.widget_icon_level_2,
            R.id.widget_icon_level_3,
            R.id.widget_icon_level_4,
            R.id.widget_icon_level_5
        )
        val labelIds = intArrayOf(
            R.id.widget_label_level_1,
            R.id.widget_label_level_2,
            R.id.widget_label_level_3,
            R.id.widget_label_level_4,
            R.id.widget_label_level_5
        )

        val moodLabels = listOf("1 Esg.", "2 Bai.", "3 Est.", "4 Bem", "5 Óti.")
        val energyLabels = listOf("1 Crít", "2 Bai.", "3 Méd.", "4 Alta", "5 Máx.")

        for (i in 0 until 5) {
            val level = i + 1
            if (pendingMood == null) {
                views.setImageViewBitmap(iconIds[i], createPixelMoodBitmap(level, 56))
                views.setTextViewText(labelIds[i], moodLabels[i])
                views.setOnClickPendingIntent(
                    btnIds[i],
                    widgetActionPendingIntent(
                        context = context,
                        action = PixelWidgetActionReceiver.ACTION_WIDGET_PICK_MOOD,
                        requestCode = requestBase + 10 + level
                    ) {
                        putExtra(AlarmScheduler.EXTRA_MOOD_SCORE, level)
                    }
                )
            } else {
                views.setImageViewBitmap(iconIds[i], createPixelEnergyBitmap(level, 56))
                views.setTextViewText(labelIds[i], energyLabels[i])
                views.setOnClickPendingIntent(
                    btnIds[i],
                    widgetActionPendingIntent(
                        context = context,
                        action = PixelWidgetActionReceiver.ACTION_WIDGET_PICK_ENERGY,
                        requestCode = requestBase + 30 + level
                    ) {
                        putExtra(AlarmScheduler.EXTRA_MOOD_SCORE, pendingMood)
                        putExtra(AlarmScheduler.EXTRA_ENERGY_SCORE, level)
                    }
                )
            }
        }
    }
}

object PixelWidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun refreshAll(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            refreshAllSuspending(appContext)
        }
    }

    suspend fun refreshAllSuspending(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext) ?: return

        val streakIds = manager.getAppWidgetIds(
            ComponentName(appContext, PixelStreakMedalWidgetProvider::class.java)
        )
        val moodIds = manager.getAppWidgetIds(
            ComponentName(appContext, PixelMoodQuickWidgetProvider::class.java)
        )
        val todayIds = manager.getAppWidgetIds(
            ComponentName(appContext, PixelTodayCardWidgetProvider::class.java)
        )
        val dashboardIds = manager.getAppWidgetIds(
            ComponentName(appContext, PixelDashboardWidgetProvider::class.java)
        )

        if (streakIds.isEmpty() && moodIds.isEmpty() && todayIds.isEmpty() && dashboardIds.isEmpty()) {
            return
        }

        val snapshot = PixelWidgetRenderer.loadSnapshot(appContext)

        if (streakIds.isNotEmpty()) {
            val rv = PixelWidgetRenderer.buildStreakMedalRemoteViews(appContext, snapshot)
            manager.updateAppWidget(streakIds, rv)
        }
        if (moodIds.isNotEmpty()) {
            val rv = PixelWidgetRenderer.buildMoodQuickRemoteViews(appContext, snapshot)
            manager.updateAppWidget(moodIds, rv)
        }
        if (todayIds.isNotEmpty()) {
            val rv = PixelWidgetRenderer.buildTodayCardRemoteViews(appContext, snapshot)
            manager.updateAppWidget(todayIds, rv)
        }
        if (dashboardIds.isNotEmpty()) {
            val rv = PixelWidgetRenderer.buildDashboardLargeRemoteViews(appContext, snapshot)
            manager.updateAppWidget(dashboardIds, rv)
        }
    }
}
