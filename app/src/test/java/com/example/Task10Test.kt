package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.CallerRule
import com.example.ui.MainViewModel
import com.example.util.BackupManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Task10Test {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: AppRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AppRepository(db.appDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testLocalBackupSaveListAndDelete() = runBlocking {
        // Insert sample rule into singleton database so BackupManager serializes it
        val singletonDb = AppDatabase.getInstance(context)
        singletonDb.appDao().insertRule(
            CallerRule(
                name = "Gate Persistence Test",
                phoneNumberPattern = "+15550199",
                isEnabled = true,
                autoAnswer = true,
                dtmfSequence = "9#",
                autoSpeakerphone = true,
                autoMuteMic = true,
                requiredWifiSsid = "Home_WiFi"
            )
        )

        // Save local backup
        val saved = BackupManager.saveLocalBackup(context)
        assertTrue(saved)

        // Verify it is listed in backups
        val backups = BackupManager.listLocalBackups(context)
        assertTrue(backups.isNotEmpty())
        val latest = backups.first()
        assertTrue(latest.name.startsWith("omnidial_backup_"))
        assertTrue(latest.exists())

        // Verify delete cleans up file
        val deleted = BackupManager.deleteLocalBackup(latest, context)
        assertTrue(deleted)
        assertFalse(latest.exists())
    }

    @Test
    fun testBackupListDeduplication(): Unit = runBlocking {
        val internalDir = BackupManager.getLocalBackupsDir(context)
        val file1 = File(internalDir, "omnidial_backup_20260917_202000.bak")
        val file2 = File(internalDir, "omnidial_backup_20260917_202000.bak.json")
        file1.writeText("{\"appName\":\"OmniDial\"}")
        file2.writeText("{\"appName\":\"OmniDial\"}")

        val list = BackupManager.listLocalBackups(context)
        // Should only return 1 item instead of duplicate entries
        assertEquals(1, list.filter { it.name.contains("20260917_202000") }.size)

        // Cleanup
        file1.delete()
        file2.delete()
        Unit
    }

    @Test
    fun testDismissAllModalsTriggersAndMaximizesCall() = runBlocking {
        val viewModel = MainViewModel(repository, context)
        val initialTrigger = viewModel.dismissModalsTrigger.value

        // Simulate minimized call screen
        viewModel.minimizeCall()
        assertTrue(viewModel.isCallScreenMinimized.value)

        // Execute dismissAllModals
        viewModel.dismissAllModals()

        // Assert trigger updated and call screen maximized
        assertTrue(viewModel.dismissModalsTrigger.value >= initialTrigger)
        assertFalse(viewModel.isCallScreenMinimized.value)
    }

    @Test
    fun testMultiChannelBackupAndRestore() = runBlocking {
        val singletonDb = AppDatabase.getInstance(context)
        val dao = singletonDb.appDao()

        // Insert channel preference and configuration
        dao.setNumberChannelPreference(
            com.example.data.NumberChannelPreference(
                normalizedNumber = "+15551234567",
                preferredChannelId = "whatsapp_business",
                customLabel = "Work WhatsApp",
                updatedTimestamp = 123456789L
            )
        )
        dao.insertOrUpdateChannelConfig(
            com.example.data.ChannelConfig(
                channelId = "sim_2",
                isEnabled = true,
                customName = "International Roaming",
                orderIndex = 1,
                updatedTimestamp = 123456789L
            )
        )

        // Create backup JSON
        val jsonString = BackupManager.createBackupJson(context)
        val root = org.json.JSONObject(jsonString)
        assertTrue(root.has("numberChannelPreferences"))
        assertTrue(root.has("channelConfigurations"))
        assertEquals(3, root.getInt("schemaVersion"))

        // Clear tables
        dao.clearAllNumberChannelPreferences()
        dao.clearAllChannelConfigs()
        assertEquals(0, dao.getAllNumberChannelPreferencesList().size)
        assertEquals(0, dao.getAllChannelConfigsList().size)

        // Restore from JSON root
        val result = BackupManager.restoreBackupFromJsonRoot(context, root)
        assertTrue(result.success)
        assertEquals(1, result.channelPreferencesCount)
        assertEquals(1, result.channelConfigsCount)

        // Verify data restored correctly
        val restoredPrefs = dao.getAllNumberChannelPreferencesList()
        assertEquals(1, restoredPrefs.size)
        assertEquals("+15551234567", restoredPrefs[0].normalizedNumber)
        assertEquals("whatsapp_business", restoredPrefs[0].preferredChannelId)

        val restoredConfigs = dao.getAllChannelConfigsList()
        assertEquals(1, restoredConfigs.size)
        assertEquals("sim_2", restoredConfigs[0].channelId)
        assertEquals("International Roaming", restoredConfigs[0].customName)
    }

    @Test
    fun testLegacyV1BackupRestoreMigratesSimPreferences() = runBlocking {
        val singletonDb = AppDatabase.getInstance(context)
        val dao = singletonDb.appDao()
        dao.clearAllNumberChannelPreferences()

        // Create a legacy schema v1 JSON without channel tables
        val legacyJson = org.json.JSONObject().apply {
            put("version", 1)
            put("schemaVersion", 1)
            put("appName", "OmniDial")
            put("timestamp", System.currentTimeMillis())

            val prefsObj = org.json.JSONObject()
            val simPrefsArr = org.json.JSONArray().apply {
                put("+15559876543:1")
                put("+15559876544:2")
            }
            prefsObj.put("contact_sim_preferences", simPrefsArr)

            val learnedArr = org.json.JSONArray().apply {
                put("+15559876545:whatsapp")
            }
            prefsObj.put("learned_call_modes", learnedArr)
            put("preferences", prefsObj)

            val favArray = org.json.JSONArray().apply {
                put(org.json.JSONObject().apply {
                    put("name", "Legacy Mom")
                    put("phoneNumber", "+15559876543")
                })
            }
            put("favorites", favArray)
        }

        val result = BackupManager.restoreBackupFromJsonRoot(context, legacyJson)
        assertTrue(result.success)
        assertEquals(1, result.favoritesCount)
        assertTrue(result.channelPreferencesCount >= 2)

        val channelPrefs = dao.getAllNumberChannelPreferencesList()
        val num1Pref = channelPrefs.find { it.normalizedNumber == "+15559876543" }
        assertNotNull(num1Pref)
        assertEquals("sim_1", num1Pref?.preferredChannelId)

        val num2Pref = channelPrefs.find { it.normalizedNumber == "+15559876544" }
        assertNotNull(num2Pref)
        assertEquals("sim_2", num2Pref?.preferredChannelId)

        val num3Pref = channelPrefs.find { it.normalizedNumber == "+15559876545" }
        assertNotNull(num3Pref)
        assertEquals("whatsapp", num3Pref?.preferredChannelId)
    }

    @Test
    fun testFutureSchemaAndCorruptChecksumAreForgiven() = runBlocking {
        val singletonDb = AppDatabase.getInstance(context)
        val dao = singletonDb.appDao()
        dao.clearAllRules()

        // Future version with checksum mismatch and extra unknown properties
        val futureJson = org.json.JSONObject().apply {
            put("schemaVersion", 99)
            put("appName", "OmniDial NextGen")
            put("futureCloudSyncId", "sync_xyz_999")
            put("checksum", "tampered_or_invalid_checksum")
            put("payloadSignature", "r=10;t=12345")

            val rulesArr = org.json.JSONArray().apply {
                put(org.json.JSONObject().apply {
                    put("name", "Forward Compatible Rule")
                    put("phoneNumberPattern", "+18005550199")
                    put("futureFeatureFlag", true)
                })
                // Malformed item without phone number pattern
                put(org.json.JSONObject().apply {
                    put("name", "Broken Rule")
                })
            }
            put("rules", rulesArr)
        }

        val result = BackupManager.restoreBackupFromJsonRoot(context, futureJson)
        // Must succeed with best-effort restore instead of failing
        assertTrue(result.success)
        assertEquals(1, result.rulesCount)

        val rules = dao.getAllRulesList()
        assertEquals(1, rules.size)
        assertEquals("Forward Compatible Rule", rules[0].name)
    }

    @Test
    fun testGoogleVoiceChannelDispatchAndResolution() = runBlocking {
        val gvChannel = com.example.domain.model.CallingChannel.GoogleVoice(
            packageName = "com.google.android.apps.googlevoice",
            isAvailable = true
        )
        assertEquals("google_voice", gvChannel.id)
        assertEquals(0xFF0F9D58, gvChannel.brandColorHex)
        assertEquals("Google Voice", gvChannel.displayName)

        // Verify ChannelDispatchCoordinator dispatchCall for Google Voice
        val coordinator = com.example.telecom.ChannelDispatchCoordinator.getInstance(context)
        var gvCallDispatchedNumber: String? = null
        val callResult = coordinator.dispatchCall(
            phoneNumber = "+15559876543",
            channel = gvChannel,
            reason = "Test GV Call",
            onCellularCall = { _, _ -> },
            onWhatsAppCall = {},
            onShowPicker = {},
            onGoogleVoiceCall = { num -> gvCallDispatchedNumber = num }
        )
        assertTrue(callResult is com.example.telecom.DispatchResult.Dispatched)
        assertEquals(gvChannel, (callResult as com.example.telecom.DispatchResult.Dispatched).channel)
        assertEquals("+15559876543", gvCallDispatchedNumber)

        // Verify ChannelDispatchCoordinator dispatchMessage for Google Voice
        var gvMsgDispatchedNumber: String? = null
        val msgResult = coordinator.dispatchMessage(
            phoneNumber = "+15559876543",
            channel = gvChannel,
            onSms = {},
            onWhatsAppMessage = {},
            onGoogleVoiceMessage = { num -> gvMsgDispatchedNumber = num }
        )
        assertTrue(msgResult is com.example.telecom.DispatchResult.Dispatched)
        assertEquals("+15559876543", gvMsgDispatchedNumber)

        // Verify ChannelPreferenceRepository caching and retrieval
        val prefRepo = com.example.data.ChannelPreferenceRepository.getInstance(context)
        prefRepo.setPreferenceForNumber("+15559876543", "google_voice")
        val cached = prefRepo.getCachedPreference("+15559876543")
        assertEquals("google_voice", cached)
    }

    @Test
    fun testRecentCallsSelectiveBackupExcludesPlainCalls() = runBlocking {
        val dao = AppDatabase.getInstance(context).appDao()
        // Clear recent calls
        dao.clearAllRecentCalls()

        // 1. Call with note
        dao.insertRecentCall(
            com.example.data.RecentCall(
                phoneNumber = "+15551111111",
                callerName = "Alice",
                callType = 1,
                note = "Follow up regarding invoice"
            )
        )

        // 2. Call with callback reminder
        dao.insertRecentCall(
            com.example.data.RecentCall(
                phoneNumber = "+15552222222",
                callerName = "Bob",
                callType = 3,
                reminderTime = System.currentTimeMillis() + 3600000L
            )
        )

        // 3. Call marked as spam
        dao.insertRecentCall(
            com.example.data.RecentCall(
                phoneNumber = "+15553333333",
                callerName = "Spam Caller",
                callType = 1,
                isSpam = true
            )
        )

        // 4. Plain call without notes, reminders, or spam (should be SKIPPED)
        dao.insertRecentCall(
            com.example.data.RecentCall(
                phoneNumber = "+15554444444",
                callerName = "Charlie",
                callType = 2,
                durationSeconds = 120L
            )
        )

        // Generate backup
        val jsonString = BackupManager.createBackupJson(context)
        val root = org.json.JSONObject(jsonString)
        val recentArray = root.getJSONArray("recentCalls")

        // Only 3 annotated calls should be backed up, plain call skipped
        assertEquals(3, recentArray.length())

        val backedUpNumbers = (0 until recentArray.length()).map {
            recentArray.getJSONObject(it).getString("phoneNumber")
        }.toSet()

        assertTrue(backedUpNumbers.contains("+15551111111"))
        assertTrue(backedUpNumbers.contains("+15552222222"))
        assertTrue(backedUpNumbers.contains("+15553333333"))
        assertFalse(backedUpNumbers.contains("+15554444444"))
    }
}

