package com.example

import android.content.Context
import android.content.Intent
import android.provider.CallLog
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.RecentCall
import com.example.ui.MainViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MissedCallHighlightTest {

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
    fun testExplicitMissedCallNotificationIntentTargetsNumber() = runBlocking {
        val viewModel = MainViewModel(repository, context)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            putExtra("EXTRA_NAV_TAB", "RECENTS")
            putExtra("EXTRA_NAV_TAB_INDEX", 1)
            putExtra("EXTRA_HIGHLIGHT_NUMBER", "+15550199")
        }

        viewModel.handleIncomingIntent(intent)

        assertEquals(1, viewModel.pendingNavTab.value)
        assertEquals("+15550199", viewModel.pendingHighlightNumber.value)
    }

    @Test
    fun testSystemMissedCallNotificationActionAutoTargetsLatestMissedCall() = runBlocking {
        val missedCall = RecentCall(
            callerName = "John Doe",
            phoneNumber = "+14155550123",
            callType = CallLog.Calls.MISSED_TYPE,
            timestamp = System.currentTimeMillis(),
            durationSeconds = 0L,
            simSlot = 1
        )
        repository.insertRecentCall(missedCall)

        val viewModel = MainViewModel(repository, context)
        viewModel.refreshRecentCalls()

        val systemIntent = Intent("android.telecom.action.SHOW_MISSED_CALLS_NOTIFICATION")
        viewModel.handleIncomingIntent(systemIntent)

        assertEquals(1, viewModel.pendingNavTab.value)
        assertEquals("+14155550123", viewModel.pendingHighlightNumber.value)
    }
}
