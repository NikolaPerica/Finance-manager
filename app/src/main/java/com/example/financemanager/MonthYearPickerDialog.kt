package com.example.financemanager

import android.app.DatePickerDialog
import android.content.Context
import android.content.res.Resources
import android.widget.DatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MonthYearPickerDialog(
    context: Context,
    private val onDateSetListener: OnDateSetListener,
    year: Int,
    month: Int,
    private val resources: Resources
) : DatePickerDialog(
    context,
    null,
    year,
    month,
    1
) {
    private lateinit var datePicker: DatePicker

    init {
        setTitle(null)
        setButton(BUTTON_POSITIVE, resources.getString(android.R.string.ok)) { _, _ ->
            onDateSetListener.onDateSet(datePicker, datePicker.year, datePicker.month)
        }
        setButton(BUTTON_NEGATIVE, resources.getString(android.R.string.cancel)) { _, _ ->
            cancel()
        }
    }

    override fun onDateChanged(view: DatePicker, year: Int, month: Int, dayOfMonth: Int) {
        super.onDateChanged(view, year, month, dayOfMonth)
        datePicker = view
    }

    override fun show() {
        super.show()
        updateMonthYear()
    }

    private fun updateMonthYear() {
        val year = datePicker.year
        val month = datePicker.month

        val date = Calendar.getInstance()
        date.set(Calendar.YEAR, year)
        date.set(Calendar.MONTH, month)

        setTitle(getFormattedMonthYear(date))
    }

    private fun getFormattedMonthYear(calendar: Calendar): String {
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return monthYearFormat.format(calendar.time)
    }

    interface OnDateSetListener {
        fun onDateSet(view: DatePicker, year: Int, month: Int)
    }
}

fun getStartOfYear(calendar: Calendar): Calendar {
    val year = calendar.get(Calendar.YEAR)
    val startOfYear = Calendar.getInstance()
    startOfYear.set(Calendar.YEAR, year)
    startOfYear.set(Calendar.MONTH, Calendar.JANUARY)
    startOfYear.set(Calendar.DAY_OF_MONTH, 1)
    startOfYear.set(Calendar.HOUR_OF_DAY, 0)
    startOfYear.set(Calendar.MINUTE, 0)
    startOfYear.set(Calendar.SECOND, 0)
    startOfYear.set(Calendar.MILLISECOND, 0)
    return startOfYear
}

fun getEndOfYear(calendar: Calendar): Calendar {
    val year = calendar.get(Calendar.YEAR)
    val endOfYear = Calendar.getInstance()
    endOfYear.set(Calendar.YEAR, year)
    endOfYear.set(Calendar.MONTH, Calendar.DECEMBER)
    endOfYear.set(Calendar.DAY_OF_MONTH, 31)
    endOfYear.set(Calendar.HOUR_OF_DAY, 23)
    endOfYear.set(Calendar.MINUTE, 59)
    endOfYear.set(Calendar.SECOND, 59)
    endOfYear.set(Calendar.MILLISECOND, 999)
    return endOfYear
}

