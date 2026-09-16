package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [CallerRule::class, AutomationLog::class, RecentCall::class, FavoriteContact::class, SpamNumber::class, IgnoredContact::class, LocalContact::class, ContactSimPreference::class],
    version = 13,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS contact_sim_preferences (
                            normalized_number TEXT NOT NULL PRIMARY KEY,
                            preferred_sim_slot INTEGER NOT NULL
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_contact_sim_preferences_normalized_number ON contact_sim_preferences(normalized_number)")
                } catch (_: Throwable) {}
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_recent_calls_phoneNumber ON recent_calls(phoneNumber)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_recent_calls_timestamp ON recent_calls(timestamp)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_caller_rules_phoneNumberPattern ON caller_rules(phoneNumberPattern)")
                } catch (_: Throwable) {}
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE recent_calls ADD COLUMN normalized_number TEXT NOT NULL DEFAULT ''")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_recent_calls_normalized_number ON recent_calls(normalized_number)")
                } catch (_: Throwable) {}
                try {
                    db.execSQL("ALTER TABLE favorite_contacts ADD COLUMN normalized_number TEXT NOT NULL DEFAULT ''")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_favorite_contacts_normalized_number ON favorite_contacts(normalized_number)")
                } catch (_: Throwable) {}
                try {
                    db.execSQL("ALTER TABLE offline_spam_numbers ADD COLUMN isBlocked INTEGER NOT NULL DEFAULT 1")
                } catch (_: Throwable) {}
                try {
                    db.execSQL("ALTER TABLE offline_spam_numbers ADD COLUMN normalized_number TEXT NOT NULL DEFAULT ''")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_offline_spam_numbers_normalized_number ON offline_spam_numbers(normalized_number)")
                } catch (_: Throwable) {}
            }
        }

        private val MIGRATION_9_11 = object : Migration(9, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_10_11.migrate(db)
            }
        }

        private val MIGRATION_8_11 = object : Migration(8, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_10_11.migrate(db)
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "telecom_dialer_db"
                )
                .addMigrations(MIGRATION_12_13, MIGRATION_11_12, MIGRATION_10_11, MIGRATION_9_11, MIGRATION_8_11)
                .fallbackToDestructiveMigration(dropAllTables = false)
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = false)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default offline spam numbers safely directly via SQLite without triggering recursive DAO instantiation
                        try {
                            db.execSQL("INSERT OR IGNORE INTO offline_spam_numbers (phoneNumber, label, reportCount, isBlocked, normalized_number) VALUES ('+18005550199', 'Robocall / Auto-dialer', 428, 1, '+18005550199')")
                            db.execSQL("INSERT OR IGNORE INTO offline_spam_numbers (phoneNumber, label, reportCount, isBlocked, normalized_number) VALUES ('+18885550144', 'Suspected Fraud / IRS Scam', 890, 1, '+18885550144')")
                            db.execSQL("INSERT OR IGNORE INTO offline_spam_numbers (phoneNumber, label, reportCount, isBlocked, normalized_number) VALUES ('+19005550123', 'High Risk Telemarketer', 312, 1, '+19005550123')")
                        } catch (_: Throwable) {}
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
