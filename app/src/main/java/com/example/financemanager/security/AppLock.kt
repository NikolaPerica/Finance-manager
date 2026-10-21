package com.example.financemanager.security

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Whether the app is locked behind the fingerprint/PIN screen.
 *
 * It starts locked, and locks again when the app comes back after being in the
 * background for [lockAfter] or longer. Time is measured with [now] (by default the
 * time since boot), so changing the phone's clock can't skip the lock.
 */
class AppLock(
    private val lockAfter: Duration = DEFAULT_LOCK_AFTER,
    private val now: () -> Long = SystemClock::elapsedRealtime,
) {
    private val _locked = MutableStateFlow(true)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    /** When the app went to the background while unlocked, in [now] milliseconds. */
    private var backgroundSince: Long? = null

    fun unlock() {
        _locked.value = false
        backgroundSince = null
    }

    fun lockNow() {
        _locked.value = true
        backgroundSince = null
    }

    /** The app is no longer visible. */
    fun onBackground() {
        if (!_locked.value) backgroundSince = now()
    }

    /** The app is visible again: locks it if it was away for too long. */
    fun onForeground() {
        val since = backgroundSince ?: return
        backgroundSince = null
        if (now() - since >= lockAfter.inWholeMilliseconds) _locked.value = true
    }

    companion object {
        val DEFAULT_LOCK_AFTER: Duration = 2.minutes
    }
}
