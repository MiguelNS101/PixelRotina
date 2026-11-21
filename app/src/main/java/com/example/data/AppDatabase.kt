package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RoutineEntity::class, CheckInLogEntity::class, JournalEntryEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao

    companion object {
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
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
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_journal_entries_dateKey` ON `journal_entries` (`dateKey`)"
                )
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `journal_entries` ADD COLUMN `aiReactionModeName` TEXT NOT NULL DEFAULT 'NONE'"
                )
                db.execSQL(
                    "ALTER TABLE `journal_entries` ADD COLUMN `aiResponseText` TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE `journal_entries` ADD COLUMN `aiSuggestedRoutineJson` TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pixel_rotina_db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
