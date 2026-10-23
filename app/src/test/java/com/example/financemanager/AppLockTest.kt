package com.example.financemanager

import com.example.financemanager.security.AppLock
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class AppLockTest {
    private var clock = 0L
    private val lock = AppLock(lockAfter = 2.minutes) { clock }

    private fun away(time: kotlin.time.Duration) {
        lock.onBackground()
        clock += time.inWholeMilliseconds
        lock.onForeground()
    }

    @Test
    fun startsLocked() {
        assertTrue(lock.locked.value)
        lock.unlock()
        assertFalse(lock.locked.value)
    }

    @Test
    fun aShortTripAwayKeepsItOpen() {
        lock.unlock()
        away(119.seconds)
        assertFalse(lock.locked.value)
    }

    @Test
    fun locksAfterBeingAwayLongEnough() {
        lock.unlock()
        away(2.minutes)
        assertTrue(lock.locked.value)
    }

    @Test
    fun eachTripIsTimedOnItsOwn() {
        lock.unlock()
        away(90.seconds)
        clock += 10.minutes.inWholeMilliseconds // in the app, not away
        away(90.seconds)
        assertFalse(lock.locked.value)
    }

    @Test
    fun goingAwayWhileLockedDoesNothingUntilUnlocked() {
        away(10.minutes)
        assertTrue(lock.locked.value)
        lock.unlock()
        // Coming back without having left (e.g. a second onStart) doesn't lock.
        lock.onForeground()
        assertFalse(lock.locked.value)
    }

    @Test
    fun canBeLockedByHand() {
        lock.unlock()
        lock.lockNow()
        assertTrue(lock.locked.value)
        // Leaving and coming back much later while locked stays locked, then unlock works.
        away(5.minutes)
        lock.unlock()
        assertFalse(lock.locked.value)
    }
}
