package com.example.ui.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    private val indonesianLocale = Locale("id", "ID")

    fun formatRupiah(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(indonesianLocale)
        format.maximumFractionDigits = 0
        return format.format(amount).replace("Rp", "Rp ")
    }

    fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", indonesianLocale)
        return sdf.format(Date(millis))
    }

    fun formatTime(millis: Long): String {
        val sdf = SimpleDateFormat("HH:mm", indonesianLocale)
        return sdf.format(Date(millis))
    }

    fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", indonesianLocale)
        return sdf.format(Date(millis))
    }

    fun currentMonthYear(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(Date())
    }

    fun formatMonthYearReadable(monthYear: String): String {
        return try {
            val sdfInput = SimpleDateFormat("yyyy-MM", Locale.US)
            val date = sdfInput.parse(monthYear)
            val sdfOutput = SimpleDateFormat("MMMM yyyy", indonesianLocale)
            if (date != null) sdfOutput.format(date) else monthYear
        } catch (e: Exception) {
            monthYear
        }
    }
}
