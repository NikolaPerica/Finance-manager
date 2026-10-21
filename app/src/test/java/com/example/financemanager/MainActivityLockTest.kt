package com.example.financemanager

import android.os.Build
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class MainActivityLockTest {

    private val app get() = ApplicationProvider.getApplicationContext<FinanceApp>()

    @Test
    fun rotatingDoesNotLockButLeavingDoes() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        assertTrue(app.appLock.locked.value)
        app.appLock.unlock()

        controller.configurationChange(android.content.res.Configuration().apply { orientation = android.content.res.Configuration.ORIENTATION_LANDSCAPE })
        assertFalse(app.appLock.locked.value)

        // Leaving and coming back right away keeps it open (the timeout is minutes).
        controller.pause().stop()
        controller.start().resume()
        assertFalse(app.appLock.locked.value)
        controller.pause().stop().destroy()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun olderAndroidHidesTheWindowOnlyWhileInTheBackground() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val flags = { controller.get().window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE }
        assertEquals(0, flags())
        controller.pause()
        assertEquals(WindowManager.LayoutParams.FLAG_SECURE, flags())
        controller.resume()
        assertEquals(0, flags())
        controller.pause().stop().destroy()
    }
}
