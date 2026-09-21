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
    entities = [CallerRule::class, AutomationLog::class, RecentCall::class, FavoriteContact::class, SpamNumber::class, IgnoredContact::class, LocalContact::class, ContactSimPreference::class, NumberChannelPreference::class, ChannelConfig::class, ContactDefaultNumber::class],
    version = 17,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS contact_default_numbers (
                            normalized_number TEXT NOT NULL PRIMARY KEY,
                            contact_id INTEGER,
                            default_number TEXT NOT NULL,
                            default_label TEXT NOT NULL,
                            updated_timestamp INTEGER NOT NULL
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_contact_default_numbers_normalized_number ON contact_default_numbers(normalized_number)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_contact_default_numbers_contact_id ON contact_default_numbers(contact_id)")
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Error migrating DB 16 to 17", e)
                }
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS channel_configurations (
                            channel_id TEXT NOT NULL PRIMARY KEY,
                            is_enabled INTEGER NOT NULL,
                            custom_name TEXT,
                            order_index INTEGER NOT NULL,
                            updated_timestamp INTEGER NOT NULL
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_channel_configurations_channel_id ON channel_configurations(channel_id)")
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Error migrating DB 15 to 16", e)
                }
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS number_channel_preferences (
                            normalized_number TEXT NOT NULL PRIMARY KEY,
                            preferred_channel_id TEXT NOT NULL,
                            custom_label TEXT,
                            updated_timestamp INTEGER NOT NULL
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_number_channel_preferences_normalized_number ON number_channel_preferences(normalized_number)")

                    db.execSQL("""
                        INSERT OR IGNORE INTO number_channel_preferences (normalized_number, preferred_channel_id, custom_label, updated_timestamp)
                        SELECT normalized_number, 
                               CASE preferred_sim_slot 
                                   WHEN 1 THEN 'sim_1'
                                   WHEN 2 THEN 'sim_2'
                                   WHEN -1 THEN 'ask'
                                   ELSE 'system'
                               END,
                               NULL,
                               strftime('%s', 'now') * 1000
                        FROM contact_sim_preferences
                    """.trimIndent())
                } catch (_: Throwable) {}
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE caller_rules ADD COLUMN autoSpeakerphone INTEGER NOT NULL DEFAULT 0")
                } catch (_: Throwable) {}
                try {
                    db.execSQL("ALTER TABLE caller_rules ADD COLUMN autoMuteMic INTEGER NOT NULL DEFAULT 0")
                } catch (_: Throwable) {}
                try {
                    db.execSQL("ALTER TABLE caller_rules ADD COLUMN requiredWifiSsid TEXT NOT NULL DEFAULT ''")
                } catch (_: Throwable) {}
                try {
                    db.execSQL("ALTER TABLE caller_rules ADD COLUMN requiredBluetoothDevice TEXT NOT NULL DEFAULT ''")
                } catch (_: Throwable) {}
            }
        }

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
                .addMigrations(MIGRATION_16_17, MIGRATION_15_16, MIGRATION_14_15, MIGRATION_13_14, MIGRATION_12_13, MIGRATION_11_12, MIGRATION_10_11, MIGRATION_9_11, MIGRATION_8_11)
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
