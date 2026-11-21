package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.alarm.AlarmScheduler
import com.example.data.AiReactionMode
import com.example.data.AlertMode
import com.example.data.AppDatabase
import com.example.data.BackupManager
import com.example.data.CheckInLogEntity
import com.example.data.CheckInSource
import com.example.data.CheckInStatus
import com.example.data.DateUtils
import com.example.data.FrequencyType
import com.example.data.JournalAiAssistant
import com.example.data.JournalCorrelationCalculator
import com.example.data.JournalEntryEntity
import com.example.data.JournalEntryType
import com.example.data.RoutineCategory
import com.example.data.RoutineEntity
import com.example.ui.RoutineViewModel
import com.example.ui.components.PixelMedalTier
import com.example.ui.components.PixelSpriteType
import com.example.ui.theme.PixelThemeType
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun `medal tiers and streak summary compute bronze 50 silver 75 and gold 100 accurately`() {
        assertEquals(
            PixelMedalTier.NONE,
            PixelMedalTier.fromCompletionPercent(49, true)
        )
        assertEquals(
            PixelMedalTier.BRONZE,
            PixelMedalTier.fromCompletionPercent(50, true)
        )
        assertEquals(
            PixelMedalTier.SILVER,
            PixelMedalTier.fromCompletionPercent(75, true)
        )
        assertEquals(
            PixelMedalTier.GOLD,
            PixelMedalTier.fromCompletionPercent(100, true)
        )

        val todayKey = DateUtils.todayDateKey()
        val routines = listOf(
            RoutineEntity(
                id = 1L,
                title = "Treino",
                categoryName = RoutineCategory.EXERCICIO.name,
                targetCountPerDay = 4,
                isEnabled = true
            )
        )
        val logsGold = (1..4).map { idx ->
            CheckInLogEntity(
                id = idx.toLong(),
                routineId = 1L,
                routineTitleSnapshot = "Treino",
                categorySnapshot = RoutineCategory.EXERCICIO.name,
                dateKey = todayKey,
                statusName = CheckInStatus.COMPLETED.name,
                sourceName = CheckInSource.IN_APP.name
            )
        }
        val summary = RoutineViewModel.computeStreakMedalSummary(routines, logsGold, todayKey)
        assertEquals(100, summary.todayCompletionPercent)
        assertEquals(PixelMedalTier.GOLD, summary.todayMedal)
        assertTrue(summary.currentStreakDays >= 1)

        val largeBmp = with(PixelSpriteType.DUMBBELL_FITNESS) {
            PixelSpriteType.DUMBBELL_FITNESS
        }
        assertEquals("DUMBBELL_FITNESS", largeBmp.name)
    }

    @Test
    fun `room journal v4 persists entries with ai fields and enforces time capsule lock`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.routineDao()
            val now = System.currentTimeMillis()
            val futureUnlock = now + 7L * 24L * 60L * 60L * 1000L

            val reflection = JournalEntryEntity(
                dateKey = "2026-10-28",
                timestamp = now - 10_000L,
                entryTypeName = JournalEntryType.REFLECTION.name,
                moodScore = 4,
                energyScore = 5,
                contextTagsCsv = "FOCO,TREINO",
                content = "Dia excelente de produtividade e treino leve.",
                routineCompletionPercentSnapshot = 80,
                medalSnapshotName = PixelMedalTier.SILVER.name,
                pomodoroCyclesSnapshot = 3,
                aiReactionModeName = AiReactionMode.VALIDATE_SUMMARY.name,
                aiResponseText = "Ótimo ritmo hoje! TAGS: FOCO, TREINO",
                aiSuggestedRoutineJson = ""
            )
            val capsule = JournalEntryEntity(
                dateKey = "2026-10-28",
                timestamp = now,
                entryTypeName = JournalEntryType.TIME_CAPSULE.name,
                content = "Carta para meu eu futuro.",
                unlockAtMillis = futureUnlock
            )

            dao.insertJournalEntry(reflection)
            dao.insertJournalEntry(capsule)

            val all = dao.getAllJournalEntriesOnce()
            assertEquals(2, all.size)
            assertEquals(JournalEntryType.TIME_CAPSULE, all[0].entryType)
            assertTrue(all[0].isTimeCapsuleLocked(now))
            assertFalse(all[0].isTimeCapsuleLocked(futureUnlock + 1L))

            assertEquals(JournalEntryType.REFLECTION, all[1].entryType)
            assertEquals(AiReactionMode.VALIDATE_SUMMARY, all[1].aiReactionMode)
            assertEquals("Ótimo ritmo hoje! TAGS: FOCO, TREINO", all[1].aiResponseText)
            assertEquals(PixelMedalTier.SILVER, all[1].medalSnapshot)
            assertEquals(3, all[1].pomodoroCyclesSnapshot)
        } finally {
            db.close()
        }
    }

    @Test
    fun `offline journal correlation calculator computes 7 day points and top 30 day tag impacts`() {
        val todayKey = "2026-10-28"
        val yesterdayKey = DateUtils.shiftDateKey(todayKey, -1)
        val oldKey = DateUtils.shiftDateKey(todayKey, -40)

        val routines = listOf(
            RoutineEntity(
                id = 1L,
                title = "Hidratação",
                targetCountPerDay = 2,
                daysOfWeekCsv = "1,2,3,4,5,6,7",
                isEnabled = true
            )
        )
        val logs = listOf(
            CheckInLogEntity(
                id = 1L,
                routineId = 1L,
                routineTitleSnapshot = "Hidratação",
                categorySnapshot = RoutineCategory.HIDRATACAO.name,
                dateKey = todayKey,
                statusName = CheckInStatus.COMPLETED.name
            ),
            CheckInLogEntity(
                id = 2L,
                routineId = 1L,
                routineTitleSnapshot = "Hidratação",
                categorySnapshot = RoutineCategory.HIDRATACAO.name,
                dateKey = todayKey,
                statusName = CheckInStatus.COMPLETED.name
            ),
            CheckInLogEntity(
                id = 3L,
                routineId = 1L,
                routineTitleSnapshot = "Hidratação",
                categorySnapshot = RoutineCategory.HIDRATACAO.name,
                dateKey = yesterdayKey,
                statusName = CheckInStatus.COMPLETED.name
            )
        )
        val journalEntries = listOf(
            JournalEntryEntity(
                id = 1L,
                dateKey = todayKey,
                moodScore = 5,
                energyScore = 4,
                contextTagsCsv = "TREINO,FOCO"
            ),
            JournalEntryEntity(
                id = 2L,
                dateKey = todayKey,
                moodScore = 3,
                energyScore = 4,
                contextTagsCsv = "TREINO"
            ),
            JournalEntryEntity(
                id = 3L,
                dateKey = yesterdayKey,
                moodScore = 2,
                energyScore = 2,
                contextTagsCsv = "SONO_RUIM,FOCO"
            ),
            // Entry older than 30 days should be excluded from 30-day tag impacts
            JournalEntryEntity(
                id = 4L,
                dateKey = oldKey,
                moodScore = 1,
                energyScore = 1,
                contextTagsCsv = "SONO_RUIM,SOBRECARGA,CAFE"
            )
        )

        val summary = JournalCorrelationCalculator.buildCorrelationSummary(
            routines = routines,
            logs = logs,
            journalEntries = journalEntries,
            todayKey = todayKey
        )

        assertEquals(7, summary.last7Days.size)
        val todayPoint = summary.last7Days.last()
        assertEquals(todayKey, todayPoint.dateKey)
        assertEquals(100, todayPoint.routineCompletionPercent)
        assertEquals(4.0f, todayPoint.avgMood ?: 0f, 0.01f)
        assertEquals(4.0f, todayPoint.avgEnergy ?: 0f, 0.01f)

        val yesterdayPoint = summary.last7Days[5]
        assertEquals(yesterdayKey, yesterdayPoint.dateKey)
        assertEquals(50, yesterdayPoint.routineCompletionPercent)
        assertEquals(2.0f, yesterdayPoint.avgMood ?: 0f, 0.01f)

        // Day with no journal entries should preserve null mood/energy for clean line breaks
        val emptyDayPoint = summary.last7Days[0]
        assertNull(emptyDayPoint.avgMood)
        assertNull(emptyDayPoint.avgEnergy)

        // Top tags in last 30 days: FOCO (2), TREINO (2), SONO_RUIM (1)
        assertEquals(3, summary.topContextTagImpacts.size)
        val treinoImpact = summary.topContextTagImpacts.first { it.tagCode == "TREINO" }
        assertEquals(2, treinoImpact.entryCount)
        assertEquals(4.0f, treinoImpact.avgMood ?: 0f, 0.01f)
        assertEquals(4.0f, treinoImpact.avgEnergy ?: 0f, 0.01f)
        val sonoImpact = summary.topContextTagImpacts.first { it.tagCode == "SONO_RUIM" }
        assertEquals(1, sonoImpact.entryCount)
    }

    @Test
    fun `ai suggested routine json parses cleanly and opens prefilled routine editor in viewmodel`() = runBlocking {
        val rawAiOutput = """
            Que tal incluir uma pausa curta de respiração no meio da tarde?
            ${JournalAiAssistant.ROUTINE_JSON_START}
            {
              "title": "Pausa de Respiração",
              "description": "3 minutos de respiração guiada para recarregar energia",
              "categoryName": "MEDITACAO",
              "spriteName": "LOTUS_MIND",
              "frequencyTypeName": "SPECIFIC_TIME",
              "hour": 15,
              "minute": 30,
              "targetCountPerDay": 1
            }
            ${JournalAiAssistant.ROUTINE_JSON_END}
        """.trimIndent()

        val extracted = JournalAiAssistant.extractActionableResult(rawAiOutput)
        assertTrue(extracted.responseText.contains("pausa curta"))
        assertTrue(extracted.suggestedRoutineJson.isNotBlank())

        val draft = JournalAiAssistant.parseSuggestedRoutineJsonToEntity(extracted.suggestedRoutineJson)
        assertNotNull(draft)
        assertEquals(0L, draft!!.id)
        assertEquals("Pausa de Respiração", draft.title)
        assertEquals(RoutineCategory.MEDITACAO, draft.category)
        assertEquals(PixelSpriteType.LOTUS_MIND, draft.sprite)
        assertEquals(FrequencyType.SPECIFIC_TIME, draft.frequencyType)
        assertEquals(15, draft.hour)
        assertEquals(30, draft.minute)

        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = RoutineViewModel(app)
        val collector = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            viewModel.uiState.collect { }
        }
        try {
            val opened = viewModel.openSuggestedRoutineInEditor(extracted.suggestedRoutineJson)
            assertTrue(opened)

            val deadline = System.currentTimeMillis() + 5_000L
            while (!viewModel.uiState.value.isEditorVisible && System.currentTimeMillis() < deadline) {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                Thread.sleep(25L)
            }
            val editorState = viewModel.uiState.value
            assertTrue(editorState.isEditorVisible)
            assertEquals("Pausa de Respiração", editorState.editingRoutine?.title)
            assertEquals(RoutineCategory.MEDITACAO, editorState.editingRoutine?.category)
        } finally {
            collector.cancel()
        }
    }

    @Test
    fun `migration 3 to 4 preserves existing journal entries and adds ai columns with defaults`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_3_4_${System.nanoTime()}.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        val rawDb = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        try {
            rawDb.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `routines` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `categoryName` TEXT NOT NULL,
                    `spriteName` TEXT NOT NULL,
                    `alertModeName` TEXT NOT NULL,
                    `frequencyTypeName` TEXT NOT NULL,
                    `hour` INTEGER NOT NULL,
                    `minute` INTEGER NOT NULL,
                    `intervalMinutes` INTEGER NOT NULL,
                    `windowStartHour` INTEGER NOT NULL,
                    `windowEndHour` INTEGER NOT NULL,
                    `targetCountPerDay` INTEGER NOT NULL,
                    `daysOfWeekCsv` TEXT NOT NULL,
                    `isEnabled` INTEGER NOT NULL,
                    `syncToCalendar` INTEGER NOT NULL,
                    `calendarEventId` INTEGER,
                    `nextTriggerMillis` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            rawDb.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `check_in_logs` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `routineId` INTEGER NOT NULL,
                    `routineTitleSnapshot` TEXT NOT NULL,
                    `categorySnapshot` TEXT NOT NULL,
                    `dateKey` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `statusName` TEXT NOT NULL,
                    `sourceName` TEXT NOT NULL,
                    `note` TEXT NOT NULL
                )
                """.trimIndent()
            )
            rawDb.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `journal_entries` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `dateKey` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `entryTypeName` TEXT NOT NULL,
                    `moodScore` INTEGER,
                    `energyScore` INTEGER,
                    `contextTagsCsv` TEXT NOT NULL,
                    `content` TEXT NOT NULL,
                    `routineCompletionPercentSnapshot` INTEGER NOT NULL,
                    `medalSnapshotName` TEXT NOT NULL,
                    `pomodoroCyclesSnapshot` INTEGER NOT NULL,
                    `unlockAtMillis` INTEGER
                )
                """.trimIndent()
            )
            rawDb.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_journal_entries_dateKey` ON `journal_entries` (`dateKey`)"
            )
            rawDb.execSQL(
                """
                INSERT INTO `journal_entries` (
                    `dateKey`, `timestamp`, `entryTypeName`, `moodScore`, `energyScore`,
                    `contextTagsCsv`, `content`, `routineCompletionPercentSnapshot`,
                    `medalSnapshotName`, `pomodoroCyclesSnapshot`, `unlockAtMillis`
                ) VALUES (
                    '2026-10-28', 1761652800000, 'REFLECTION', 4, 4,
                    'FOCO', 'Nota criada na v3', 75, 'SILVER', 2, NULL
                )
                """.trimIndent()
            )
            rawDb.version = 3
        } finally {
            rawDb.close()
        }

        val migratedDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()
        try {
            val entries = runBlocking { migratedDb.routineDao().getAllJournalEntriesOnce() }
            assertEquals(1, entries.size)
            val row = entries.first()
            assertEquals("Nota criada na v3", row.content)
            assertEquals(AiReactionMode.NONE, row.aiReactionMode)
            assertEquals("", row.aiResponseText)
            assertEquals("", row.aiSuggestedRoutineJson)
        } finally {
            migratedDb.close()
            dbFile.delete()
        }
    }

    @Test
    fun `backup manager version 3 roundtrips ai fields and stays backward compatible with v1 and v2`() {
        val journalEntry = JournalEntryEntity(
            id = 7L,
            dateKey = "2026-10-28",
            timestamp = 1761652800000L,
            entryTypeName = JournalEntryType.REFLECTION.name,
            moodScore = 4,
            energyScore = 3,
            contextTagsCsv = "ESTUDO,CAFE",
            content = "Consegui estudar bem pela manhã.",
            routineCompletionPercentSnapshot = 75,
            medalSnapshotName = PixelMedalTier.SILVER.name,
            pomodoroCyclesSnapshot = 2,
            unlockAtMillis = null,
            aiReactionModeName = AiReactionMode.ACTIONABLE_STEPS.name,
            aiResponseText = "Excelente foco matinal! Mantenha pausas curtas.",
            aiSuggestedRoutineJson = """{"title":"Pausa Visual","categoryName":"CORPO_MENTE"}"""
        )

        val v3Json = BackupManager.serializeToJson(
            themeType = PixelThemeType.PASTEL_RETRO,
            routines = emptyList(),
            checkInLogs = emptyList(),
            journalEntries = listOf(journalEntry)
        )
        val parsedV3 = BackupManager.parseFromJson(v3Json)
        assertEquals(3, parsedV3.version)
        assertEquals(PixelThemeType.PASTEL_RETRO, parsedV3.theme)
        assertEquals(1, parsedV3.journalEntries.size)
        val restored = parsedV3.journalEntries.first()
        assertEquals(AiReactionMode.ACTIONABLE_STEPS, restored.aiReactionMode)
        assertEquals("Excelente foco matinal! Mantenha pausas curtas.", restored.aiResponseText)
        assertTrue(restored.aiSuggestedRoutineJson.contains("Pausa Visual"))

        // Legacy v1 JSON (no journalEntries array)
        val legacyV1Json = """
            {
              "app": "PixelRotina",
              "version": 1,
              "themeId": "pastel_retro",
              "routines": [
                { "id": 1, "title": "Água", "categoryName": "HIDRATACAO" }
              ],
              "checkInLogs": []
            }
        """.trimIndent()
        val parsedV1 = BackupManager.parseFromJson(legacyV1Json)
        assertEquals(1, parsedV1.version)
        assertEquals(1, parsedV1.routines.size)
        assertTrue(parsedV1.journalEntries.isEmpty())

        // Legacy v2 JSON (journalEntries without Stage 2 AI columns)
        val legacyV2Json = """
            {
              "app": "PixelRotina",
              "version": 2,
              "themeId": "neon_arcade",
              "routines": [],
              "checkInLogs": [],
              "journalEntries": [
                {
                  "id": 2,
                  "dateKey": "2026-10-27",
                  "timestamp": 1761566400000,
                  "entryTypeName": "MICRO_LOG",
                  "moodScore": 5,
                  "energyScore": 4,
                  "contextTagsCsv": "FOCO",
                  "content": "Dia ótimo sem IA.",
                  "routineCompletionPercentSnapshot": 100,
                  "medalSnapshotName": "GOLD",
                  "pomodoroCyclesSnapshot": 4,
                  "unlockAtMillis": null
                }
              ]
            }
        """.trimIndent()
        val parsedV2 = BackupManager.parseFromJson(legacyV2Json)
        assertEquals(2, parsedV2.version)
        assertEquals(1, parsedV2.journalEntries.size)
        assertEquals(AiReactionMode.NONE, parsedV2.journalEntries.first().aiReactionMode)
        assertEquals("", parsedV2.journalEntries.first().aiResponseText)
        assertEquals("", parsedV2.journalEntries.first().aiSuggestedRoutineJson)
    }

    @Test
    fun `end of day daily checkin computes future trigger and routine mood checkins average across the day`() {
        val now = System.currentTimeMillis()
        val next21 = AlarmScheduler.computeNextDailyCheckInTriggerMillis(21, 0, now)
        val next06 = AlarmScheduler.computeNextDailyCheckInTriggerMillis(6, 30, now)
        assertTrue("21:00 daily check-in must be in the future", next21 > now)
        assertTrue("06:30 daily check-in must be in the future", next06 > now)

        val todayKey = "2026-11-21"
        val entries = listOf(
            JournalEntryEntity(
                dateKey = todayKey,
                entryTypeName = JournalEntryType.MOOD_CHECKIN.name,
                moodScore = 3,
                energyScore = 2,
                content = "Após rotina: Beber Água"
            ),
            JournalEntryEntity(
                dateKey = todayKey,
                entryTypeName = JournalEntryType.MOOD_CHECKIN.name,
                moodScore = 5,
                energyScore = 4,
                content = "Check-in rápido após rotina: Treino"
            ),
            JournalEntryEntity(
                dateKey = todayKey,
                entryTypeName = JournalEntryType.MOOD_CHECKIN.name,
                moodScore = 4,
                energyScore = 3,
                content = "Check-in do fim do dia via notificação"
            )
        )
        val daySummary = JournalCorrelationCalculator.computeDaySummaryByDate(entries)[todayKey]
        assertNotNull(daySummary)
        assertEquals(3, daySummary!!.entryCount)
        assertEquals(4.0f, daySummary.avgMood ?: 0f, 0.01f)
        assertEquals(3.0f, daySummary.avgEnergy ?: 0f, 0.01f)
    }

    @Test
    fun `full art pixel notifications inflate RemoteViews cleanly with all 5 mood and energy buttons`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Verify 8x8 pixel mood & energy bitmaps for levels 1..5
        for (level in 1..5) {
            val moodBmp = com.example.ui.components.createPixelMoodBitmap(level, 64)
            val energyBmp = com.example.ui.components.createPixelEnergyBitmap(level, 64)
            assertEquals(64, moodBmp.width)
            assertEquals(64, moodBmp.height)
            assertEquals(64, energyBmp.width)
            assertEquals(64, energyBmp.height)
            assertTrue("Mood center pixel for level $level should not be transparent", moodBmp.getPixel(32, 32) != 0)
            assertTrue("Energy center pixel for level $level should not be transparent", energyBmp.getPixel(32, 32) != 0)
        }

        val parent = android.widget.FrameLayout(context)

        // 2. Mood Step 1/2 full-art notification with 5 interactive buttons
        val moodNotif = com.example.alarm.NotificationHelper.buildMoodStepNotification(
            context = context,
            isDailyEndOfDay = true,
            themeType = PixelThemeType.CATPPUCCIN_MOCHA,
            heroArtPreference = com.example.data.HeroArtPreference.NIGHT_CAMPFIRE
        )
        assertNotNull(moodNotif.contentView)
        assertNotNull(moodNotif.bigContentView)
        val moodExpanded = moodNotif.bigContentView.apply(context, parent)
        assertNotNull(moodExpanded.findViewById(R.id.notif_bg_art))
        assertNotNull(moodExpanded.findViewById(R.id.notif_btn_level_1))
        assertNotNull(moodExpanded.findViewById(R.id.notif_btn_level_2))
        assertNotNull(moodExpanded.findViewById(R.id.notif_btn_level_3))
        assertNotNull(moodExpanded.findViewById(R.id.notif_btn_level_4))
        assertNotNull(moodExpanded.findViewById(R.id.notif_btn_level_5))

        // 3. Energy Step 2/2 full-art notification with 5 interactive buttons
        val energyNotif = com.example.alarm.NotificationHelper.buildEnergyStepNotification(
            context = context,
            notificationId = com.example.alarm.NotificationHelper.DAILY_CHECKIN_NOTIFICATION_ID,
            moodScore = 4,
            isDailyEndOfDay = true,
            themeType = PixelThemeType.PASTEL_RETRO,
            heroArtPreference = com.example.data.HeroArtPreference.AFTERNOON_GARDEN
        )
        assertNotNull(energyNotif.contentView)
        assertNotNull(energyNotif.bigContentView)
        val energyExpanded = energyNotif.bigContentView.apply(context, parent)
        assertNotNull(energyExpanded.findViewById(R.id.notif_bg_art))
        assertNotNull(energyExpanded.findViewById(R.id.notif_btn_level_5))

        // 4. Routine full-art notification with 3 pixel buttons
        val sampleRoutine = RoutineEntity(
            id = 42L,
            title = "Beber Água",
            description = "Um copo fresco agora",
            categoryName = RoutineCategory.HIDRATACAO.name,
            spriteName = PixelSpriteType.WATER_DROP.name
        )
        val routineNotif = com.example.alarm.NotificationHelper.buildRoutineNotification(
            context = context,
            routine = sampleRoutine,
            snoozeMinutes = 15,
            themeType = PixelThemeType.CATPPUCCIN_MOCHA,
            heroArtPreference = com.example.data.HeroArtPreference.MORNING_RAIN
        )
        assertNotNull(routineNotif.contentView)
        assertNotNull(routineNotif.bigContentView)
        val routineCollapsed = routineNotif.contentView.apply(context, parent)
        val routineExpanded = routineNotif.bigContentView.apply(context, parent)
        assertNotNull(routineCollapsed.findViewById(R.id.notif_btn_ok_collapsed))
        assertNotNull(routineExpanded.findViewById(R.id.notif_btn_ok))
        assertNotNull(routineExpanded.findViewById(R.id.notif_btn_ok_mood))
        assertNotNull(routineExpanded.findViewById(R.id.notif_btn_snooze))

        // 5. Pomodoro full-art notification
        val pomodoroNotif = com.example.alarm.NotificationHelper.buildPomodoroIntervalFinishedNotification(
            context = context,
            title = "🍅 Foco Concluído!",
            message = "Hora de descansar 5 min.",
            heroArtPreference = com.example.data.HeroArtPreference.AFTERNOON_LIBRARY
        )
        assertNotNull(pomodoroNotif.bigContentView)
        val pomodoroExpanded = pomodoroNotif.bigContentView.apply(context, parent)
        assertNotNull(pomodoroExpanded.findViewById(R.id.notif_bg_art))

        // 6. Persistent Alarm full-art notification with Desligar, OK+Humor, Snooze
        val alarmNotif = com.example.alarm.NotificationHelper.buildPersistentAlarmForegroundNotification(
            context = context,
            routineId = 77L,
            title = "Tomar Vitamina",
            description = "Logo após o café",
            iconName = PixelSpriteType.ALARM_BELL.name,
            snoozeMinutes = 10,
            themeType = PixelThemeType.CATPPUCCIN_MOCHA,
            heroArtPreference = com.example.data.HeroArtPreference.MORNING_SUNRISE
        )
        assertNotNull(alarmNotif.contentView)
        assertNotNull(alarmNotif.bigContentView)
        val alarmCollapsed = alarmNotif.contentView.apply(context, parent)
        val alarmExpanded = alarmNotif.bigContentView.apply(context, parent)
        assertNotNull(alarmCollapsed.findViewById(R.id.notif_btn_dismiss_collapsed))
        assertNotNull(alarmExpanded.findViewById(R.id.notif_btn_dismiss))
        assertNotNull(alarmExpanded.findViewById(R.id.notif_btn_ok_mood))
        assertNotNull(alarmExpanded.findViewById(R.id.notif_btn_snooze))
    }

    @Test
    fun `all 4 pixel art home screen widgets render bitmaps and inflate cleanly across states`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parent = android.widget.FrameLayout(context)

        // Verify new widget pixel art bitmaps
        val goldMedal = com.example.ui.components.createPixelMedalBitmap(PixelMedalTier.GOLD, true, 80)
        val flame = com.example.ui.components.createPixelStreakFlameBitmap(true, 64)
        val bar = com.example.ui.components.createPixelProgressBarBitmap(75, 360, 22)
        assertEquals(80, goldMedal.width)
        assertEquals(64, flame.width)
        assertEquals(360, bar.width)
        assertTrue("goldMedal center pixel should be non-zero", goldMedal.getPixel(40, 48) != 0)
        assertTrue("flame center pixel should be non-zero", flame.getPixel(32, 40) != 0)
        assertTrue("bar border pixel at (0,0) should be non-zero", bar.getPixel(0, 0) != 0)
        assertTrue("bar track pixel at (3,3) should be non-zero", bar.getPixel(3, 3) != 0)
        assertTrue("bar filled segment pixel at (5,5) should be non-zero", bar.getPixel(5, 5) != 0)

        val sampleRoutine = RoutineEntity(
            id = 101L,
            title = "Alongar Postura",
            categoryName = RoutineCategory.CORPO_MENTE.name,
            spriteName = PixelSpriteType.LOTUS_MIND.name,
            hour = 14,
            minute = 30
        )
        val snapshotStep1 = com.example.widget.PixelWidgetSnapshot(
            themeType = PixelThemeType.CATPPUCCIN_MOCHA,
            heroArtPreference = com.example.data.HeroArtPreference.AFTERNOON_GARDEN,
            streakSummary = com.example.ui.StreakMedalSummary(
                hasActiveRoutinesToday = true,
                todayCompletedCount = 3,
                todayTargetCount = 4,
                todayCompletionPercent = 75,
                todayMedal = PixelMedalTier.SILVER,
                currentStreakDays = 5
            ),
            todayRoutines = listOf(
                com.example.widget.WidgetRoutineRow(sampleRoutine, completedToday = 0, targetToday = 1)
            ),
            todayAvgMood = 4.2f,
            todayAvgEnergy = 3.8f,
            todayMoodCount = 2,
            pendingMoodScore = null
        )

        // Widget 1: 2x2 Streak & Medal
        val streakView = com.example.widget.PixelWidgetRenderer
            .buildStreakMedalRemoteViews(context, snapshotStep1)
            .apply(context, parent)
        assertEquals(android.R.id.background, streakView.id)
        assertNotNull(streakView.findViewById(R.id.widget_root))
        assertNotNull(streakView.findViewById(R.id.widget_accent_bar))
        assertNotNull(streakView.findViewById(R.id.widget_medal_icon))
        assertNotNull(streakView.findViewById(R.id.widget_progress_bar))

        // Widget 2: 4x1 Mood Quick (Step 1 Mood + Step 2 Energy)
        val moodStep1View = com.example.widget.PixelWidgetRenderer
            .buildMoodQuickRemoteViews(context, snapshotStep1)
            .apply(context, parent)
        assertEquals(android.R.id.background, moodStep1View.id)
        assertNotNull(moodStep1View.findViewById(R.id.widget_accent_bar))
        assertNotNull(moodStep1View.findViewById(R.id.widget_btn_level_1))
        assertNotNull(moodStep1View.findViewById(R.id.widget_btn_level_5))

        val moodStep2View = com.example.widget.PixelWidgetRenderer
            .buildMoodQuickRemoteViews(context, snapshotStep1.copy(pendingMoodScore = 4))
            .apply(context, parent)
        assertNotNull(moodStep2View.findViewById(R.id.widget_mood_reset_btn))

        // Widget 3: 4x2 Today Card & Routine Checklist
        val todayCardView = com.example.widget.PixelWidgetRenderer
            .buildTodayCardRemoteViews(context, snapshotStep1)
            .apply(context, parent)
        assertEquals(android.R.id.background, todayCardView.id)
        assertNotNull(todayCardView.findViewById(R.id.widget_accent_bar))
        assertNotNull(todayCardView.findViewById(R.id.widget_row_1_ok))
        assertNotNull(todayCardView.findViewById(R.id.widget_refresh_btn))

        // Widget 4: 4x4 Full Pixel Dashboard
        val dashboardView = com.example.widget.PixelWidgetRenderer
            .buildDashboardLargeRemoteViews(context, snapshotStep1)
            .apply(context, parent)
        assertEquals(android.R.id.background, dashboardView.id)
        assertNotNull(dashboardView.findViewById(R.id.widget_accent_bar))
        assertNotNull(dashboardView.findViewById(R.id.widget_row_1_ok))
        assertNotNull(dashboardView.findViewById(R.id.widget_btn_level_5))
        assertNotNull(dashboardView.findViewById(R.id.widget_tab_today))
        assertNotNull(dashboardView.findViewById(R.id.widget_tab_journal))
        assertNotNull(dashboardView.findViewById(R.id.widget_tab_report))
    }
}



