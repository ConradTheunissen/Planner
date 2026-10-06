package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room Database abstract class providing local persistence for DepartmentEntries,
 * Departments, Google Drive files, Mail messages, and AI summaries.
 *
 * Implemented as a thread-safe Singleton with explicit Migration strategies
 * and safe destructive migration fallback.
 */
@Database(
    entities = [
        DepartmentEntity::class,
        DepartmentEntryEntity::class,
        DriveFileEntity::class,
        MailMessageEntity::class,
        AiSummaryEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Primary DAO providing access to DepartmentEntries and related entities.
     */
    abstract fun departmentEntryDao(): OmniSyncDao

    fun dao(): OmniSyncDao = departmentEntryDao()

    companion object {
        const val DATABASE_NAME = "app_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration 1 -> 2:
         * Adds departments and department_entries tables with appropriate indices.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `departments` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `code` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `leadName` TEXT NOT NULL,
                        `leadEmail` TEXT NOT NULL,
                        `colorHex` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_departments_code` ON `departments` (`code`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `department_entries` (
                        `id` TEXT NOT NULL,
                        `departmentId` TEXT NOT NULL,
                        `departmentName` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `submitterName` TEXT NOT NULL,
                        `submitterEmail` TEXT NOT NULL,
                        `priorityStr` TEXT NOT NULL,
                        `statusStr` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `reviewNotes` TEXT NOT NULL,
                        `driveFileId` TEXT,
                        `driveFileName` TEXT,
                        `driveFileLink` TEXT,
                        `aiSummary` TEXT,
                        `syncStatus` TEXT NOT NULL DEFAULT 'SYNCED',
                        `lastSyncedAt` INTEGER NOT NULL DEFAULT 0,
                        `remoteDriveId` TEXT,
                        `remoteMailId` TEXT,
                        `syncError` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_department_entries_departmentId` ON `department_entries` (`departmentId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_department_entries_statusStr` ON `department_entries` (`statusStr`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_department_entries_syncStatus` ON `department_entries` (`syncStatus`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_department_entries_createdAt` ON `department_entries` (`createdAt`)")
            }
        }

        /**
         * Migration 2 -> 3:
         * Adds sync status tracking and remote linkage columns to department_entries.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `department_entries` ADD COLUMN `syncStatus` TEXT NOT NULL DEFAULT 'SYNCED'")
                } catch (e: Exception) {
                    // Column already present in some installations
                }
                try {
                    db.execSQL("ALTER TABLE `department_entries` ADD COLUMN `lastSyncedAt` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                }
                try {
                    db.execSQL("ALTER TABLE `department_entries` ADD COLUMN `remoteDriveId` TEXT")
                } catch (e: Exception) {
                }
                try {
                    db.execSQL("ALTER TABLE `department_entries` ADD COLUMN `remoteMailId` TEXT")
                } catch (e: Exception) {
                }
                try {
                    db.execSQL("ALTER TABLE `department_entries` ADD COLUMN `syncError` TEXT")
                } catch (e: Exception) {
                }
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_department_entries_syncStatus` ON `department_entries` (`syncStatus`)")
            }
        }

        /**
         * Returns the singleton instance of AppDatabase.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

typealias OmniSyncDatabase = AppDatabase
