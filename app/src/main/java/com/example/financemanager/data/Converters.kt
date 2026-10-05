package com.example.financemanager.data

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.format.DateTimeParseException

class Converters {
    // ISO "yyyy-MM-dd", the format the transactions table has always used.
    @TypeConverter
    fun dateToText(date: LocalDate): String = date.toString()

    @TypeConverter
    fun textToDate(text: String): LocalDate = try {
        LocalDate.parse(text)
    } catch (_: DateTimeParseException) {
        LocalDate.of(1970, 1, 1)
    }
}
