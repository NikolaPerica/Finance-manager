package com.example.financemanager.ui

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Pads this view by the system bars (and optionally the keyboard) on top of its
 * own XML padding, so edge-to-edge content never hides behind them.
 */
fun View.applySystemBarsPadding(includeIme: Boolean = false) {
    val left = paddingLeft
    val top = paddingTop
    val right = paddingRight
    val bottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        val ime = if (includeIme) insets.getInsets(WindowInsetsCompat.Type.ime()).bottom else 0
        view.setPadding(
            left + bars.left,
            top + bars.top,
            right + bars.right,
            bottom + maxOf(bars.bottom, ime)
        )
        insets
    }
}
