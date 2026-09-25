package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.CallerRule
import com.example.data.FavoriteContact
import com.example.util.BackupManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AutoBackupTest {

    private lateinit var context: Context

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences("kishan_dialer_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val dir = BackupManager.getLocalBackupsDir(context)
        dir.listFiles()?.forEach { it.delete() }
        val db = AppDatabase.getInstance(context)
        db.appDao().clearAllRules()
        db.appDao().clearAllFavorites()
        for (call in db.appDao().getAllRecentCallsList()) {
            db.appDao().deleteRecentCall(call)
        }
    }

    @Test
    fun testMarkBackupDirty() {
        val prefs = context.getSharedPreferences("kishan_dialer_prefs", Context.MODE_PRIVATE)
        assertFalse(prefs.getBoolean("auto_backup_dirty", false))

        BackupManager.markBackupDirty(context)
        assertTrue(prefs.getBoolean("auto_backup_dirty", false))
    }

    @Test
    fun testAutoBackupThrottledWhenNotDirtyOrTooRecent() = runBlocking {
        val prefs = context.getSharedPreferences("kishan_dialer_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_backup_enabled", true).commit()

        // 1. Not dirty -> should return false
        assertFalse(BackupManager.checkAndRunAutoBackup(context))

        // 2. Mark dirty, but empty database -> should not backup empty database
        BackupManager.markBackupDirty(context)
        assertFalse(BackupManager.checkAndRunAutoBackup(context))

        // 3. Add a favorite contact so DB is non-empty
        val db = AppDatabase.getInstance(context)
        db.appDao().insertFavorite(
            FavoriteContact(name = "Alice", phoneNumber = "5551234", label = "Mobile")
        )
        BackupManager.markBackupDirty(context)

        // Now auto backup should succeed
        assertTrue(BackupManager.checkAndRunAutoBackup(context))

        // Dirty flag cleared, timestamp recorded
        assertFalse(prefs.getBoolean("auto_backup_dirty", false))
        assertTrue(prefs.getLong("last_auto_backup_timestamp", 0L) > 0L)

        // Immediate subsequent call should be throttled (within 6h window)
        BackupManager.markBackupDirty(context)
        assertFalse(BackupManager.checkAndRunAutoBackup(context))
    }

    @Test
    fun testAutoBackupRotationKeepsFiveNewest() = runBlocking {
        val dir = BackupManager.getLocalBackupsDir(context)
        val db = AppDatabase.getInstance(context)
        db.appDao().insertFavorite(
            FavoriteContact(name = "Bob", phoneNumber = "5559876", label = "Mobile")
        )

        // Create 7 manual simulated auto-backups with distinct filenames
        for (i in 1..7) {
            val file = File(dir, "omnidial_auto_20260925_00000$i.bak")
            file.writeText("""{"appName":"OmniDial","version":3}""")
            file.setLastModified(System.currentTimeMillis() + i * 1000L)
        }

        // Run auto-backup
        BackupManager.saveLocalBackup(context, isAutoBackup = true)

        val autoFiles = dir.listFiles { f ->
            f.name.startsWith("omnidial_auto_") && f.name.endsWith(".bak")
        } ?: emptyArray()

        assertEquals(5, autoFiles.size)
    }

    @Test
    fun testFindEligibleRestoreBackupOnlyWhenDbEmpty() = runBlocking {
        val dir = BackupManager.getLocalBackupsDir(context)
        val sampleBackup = File(dir, "omnidial_backup_20260925_120000.bak")
        sampleBackup.writeText("""{"appName":"OmniDial","version":3,"rules":[]}""")

        val db = AppDatabase.getInstance(context)
        // With empty DB, should find eligible backup
        val eligible = BackupManager.findEligibleRestoreBackup(context)
        assertNotNull(eligible)
        assertEquals(sampleBackup.name, eligible?.name)

        // With non-empty DB, should return null
        db.appDao().insertRule(
            CallerRule(name = "Rule 1", phoneNumberPattern = "123*")
        )
        val notEligible = BackupManager.findEligibleRestoreBackup(context)
        assertNull(notEligible)
    }
}
