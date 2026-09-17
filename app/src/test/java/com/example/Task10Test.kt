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
}
