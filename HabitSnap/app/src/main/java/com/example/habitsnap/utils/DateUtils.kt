package com.example.habitsnap.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFmt = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val fullFmt = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    fun today(): String = sdf.format(Date())

    fun yesterday(): String {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return sdf.format(cal.time)
    }

    fun toDisplayDate(epochMs: Long): String = displayFmt.format(Date(epochMs))
    fun toTimeString(epochMs: Long): String = timeFmt.format(Date(epochMs))
    fun toFullString(epochMs: Long): String = fullFmt.format(Date(epochMs))

    fun dateStringToEpoch(dateStr: String): Long? =
        try { sdf.parse(dateStr)?.time } catch (_: Exception) { null }

    fun epochToDateString(epochMs: Long): String = sdf.format(Date(epochMs))

    fun lastNDays(n: Int): List<String> {
        val result = mutableListOf<String>()
        for (i in n - 1 downTo 0) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            result.add(sdf.format(cal.time))
        }
        return result
    }

    fun isToday(dateStr: String): Boolean = dateStr == today()

    fun daysSince(dateStr: String): Int {
        val then = sdf.parse(dateStr)?.time ?: return 0
        val diffMs = System.currentTimeMillis() - then
        return (diffMs / (1000 * 60 * 60 * 24)).toInt()
    }
}