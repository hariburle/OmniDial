package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.CallerRule
import com.example.ui.screens.standardAutomationTemplates
import com.example.util.BackupManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase11AutomationTest {

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
    fun testCallerRulePersistenceWithNewAutomationFields() = runBlocking {
        val rule = CallerRule(
            name = "Gate Buzzer Protected",
            phoneNumberPattern = "+15550199",
            isEnabled = true,
            autoAnswer = true,
            answerDelaySec = 1,
            dtmfSequence = "9#",
            dtmfDelayMs = 800L,
            autoHangup = true,
            hangupDelaySec = 2,
            autoSpeakerphone = true,
            autoMuteMic = true,
            requiredWifiSsid = "Home_5G",
            requiredBluetoothDevice = "Tesla Model 3"
        )

        val id = repository.insertRule(rule)
        assertTrue(id > 0)

        val retrieved = repository.getEnabledRules().firstOrNull { it.id == id }
        assertNotNull(retrieved)
        assertEquals("Gate Buzzer Protected", retrieved!!.name)
        assertTrue(retrieved.autoSpeakerphone)
        assertTrue(retrieved.autoMuteMic)
        assertEquals("Home_5G", retrieved.requiredWifiSsid)
        assertEquals("Tesla Model 3", retrieved.requiredBluetoothDevice)
    }

    @Test
    fun testStandardTemplatesContainGateAutomationPresets() {
        val gateTemplate = standardAutomationTemplates.firstOrNull { it.title.contains("Gate", ignoreCase = true) }
        assertNotNull(gateTemplate)
        assertTrue(gateTemplate!!.defaultRule.autoAnswer)
        assertTrue(gateTemplate.defaultRule.autoSpeakerphone)
        assertTrue(gateTemplate.defaultRule.autoMuteMic)
        assertEquals("9#", gateTemplate.defaultRule.dtmfSequence)
    }

    @Test
    fun testBackupAndRestorePreservesAmbientGeofenceFields() = runBlocking {
        val appDb = AppDatabase.getInstance(context)
        val rule = CallerRule(
            name = "Office Extension Mode",
            phoneNumberPattern = "+18005550100",
            isEnabled = true,
            autoAnswer = true,
            dtmfSequence = "104#",
            autoSpeakerphone = true,
            autoMuteMic = true,
            requiredWifiSsid = "Corp_Office_5G",
            requiredBluetoothDevice = "Jabra Evolve"
        )
        appDb.appDao().insertRule(rule)

        val backupJson = BackupManager.createBackupJson(context)
        assertTrue(backupJson.contains("Corp_Office_5G"))
        assertTrue(backupJson.contains("autoSpeakerphone"))
        assertTrue(backupJson.contains("autoMuteMic"))
        assertTrue(backupJson.contains("Jabra Evolve"))
    }
}
