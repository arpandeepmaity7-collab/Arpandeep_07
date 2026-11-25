package com.example.ui.workspace

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateFormatting {
    fun startOfDayMillis(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun endOfDayMillis(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun todayDayMillis(): Long = endOfDayMillis(System.currentTimeMillis())

    fun tomorrowDayMillis(): Long {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return endOfDayMillis(cal.timeInMillis)
    }

    fun nextWeekDayMillis(): Long {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 7)
        }
        return endOfDayMillis(cal.timeInMillis)
    }

    fun defaultReminderForDate(dueMillis: Long, hour: Int = 9, minute: Int = 0): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dueMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun isOverdue(dueAtMillis: Long?, completed: Boolean, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (dueAtMillis == null || completed) return false
        return dueAtMillis < startOfDayMillis(nowMillis)
    }

    fun isDueToday(dueAtMillis: Long?, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (dueAtMillis == null) return false
        val start = startOfDayMillis(nowMillis)
        val end = endOfDayMillis(nowMillis)
        return dueAtMillis in start..end
    }

    fun isUpcoming(dueAtMillis: Long?, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (dueAtMillis == null) return false
        return dueAtMillis > endOfDayMillis(nowMillis)
    }

    fun formatEditorialHeaderDate(nowMillis: Long = System.currentTimeMillis()): String {
        val fmt = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        return fmt.format(Date(nowMillis))
    }

    fun formatDueBadge(dueAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val startToday = startOfDayMillis(nowMillis)
        val endToday = endOfDayMillis(nowMillis)
        val startTomorrow = startOfDayMillis(nowMillis + 86_400_000L)
        val endTomorrow = endOfDayMillis(nowMillis + 86_400_000L)

        return when {
            dueAtMillis in startToday..endToday -> "Today"
            dueAtMillis in startTomorrow..endTomorrow -> "Tomorrow"
            else -> {
                val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
                fmt.format(Date(dueAtMillis))
            }
        }
    }

    fun formatTime(millis: Long): String {
        val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
        return fmt.format(Date(millis))
    }

    fun extractHourMinute(millis: Long): Pair<Int, Int> {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return cal.get(Calendar.HOUR_OF_DAY) to cal.get(Calendar.MINUTE)
    }
}
