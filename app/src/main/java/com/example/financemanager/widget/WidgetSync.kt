package com.example.financemanager.widget

import android.content.Context
import com.example.financemanager.di.appEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Redraws the widget whenever transactions change. Runs only while a widget is on the
 * home screen; the widget's own six-hourly update covers the start of a new month.
 */
object WidgetSync {
    private val started = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Starts syncing if the user has placed a widget. Call on app start. */
    fun startIfPlaced(context: Context) {
        if (FinanceWidget.isPlaced(context)) start(context)
    }

    fun start(context: Context) {
        if (!started.compareAndSet(false, true)) return
        val app = context.applicationContext
        scope.launch {
            // The first value is what the widget already shows.
            app.appEntryPoint().repository().transactions().distinctUntilChanged().drop(1).collect { FinanceWidget.updateAll(app) }
        }
    }

    /** Redraws the widget with the current data and settings, then calls [onDone]. */
    fun refresh(context: Context, onDone: () -> Unit = {}) {
        val app = context.applicationContext
        scope.launch {
            try {
                FinanceWidget.updateAll(app)
            } finally {
                onDone()
            }
        }
    }
}
