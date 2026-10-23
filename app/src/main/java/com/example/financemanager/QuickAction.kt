package com.example.financemanager

import android.content.Context
import android.content.Intent

/** Shortcuts into the app from outside it, e.g. the home screen widget's buttons. */
enum class QuickAction(val action: String) {
    ADD_INCOME("com.example.financemanager.action.ADD_INCOME"),
    ADD_EXPENSE("com.example.financemanager.action.ADD_EXPENSE"),
    ;

    /** Opens the app (or brings it back) and performs this action once it is unlocked. */
    fun intent(context: Context): Intent = Intent(context, MainActivity::class.java)
        .setAction(action)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    companion object {
        fun from(intent: Intent?): QuickAction? = entries.find { it.action == intent?.action }
    }
}
