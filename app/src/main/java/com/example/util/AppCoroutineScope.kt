package com.example.util

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

/**
 * Application-wide structured coroutine scope backed by a SupervisorJob and Dispatchers.IO.
 * Used for persistent background operations (e.g. BroadcastReceivers, database writes, background SMS)
 * that should survive individual viewmodel or screen lifecycles without leaking or failing silently.
 */
object AppCoroutineScope : CoroutineScope {
    private const val TAG = "AppCoroutineScope"

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Unhandled exception in application background coroutine", throwable)
    }

    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.IO + exceptionHandler
}
