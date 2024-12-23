package com.example.chatapp.util

import java.text.SimpleDateFormat
import java.util.*

object DateFormatter {
    private val fullDateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    
    fun formatMessageTimestamp(timestamp: Long): String {
        val messageDate = Date(timestamp)
        val now = Calendar.getInstance()
        val messageCalendar = Calendar.getInstance().apply { time = messageDate }

        return when {
            // Today
            isSameDay(now, messageCalendar) -> timeFormat.format(messageDate)
            // This week
            isThisWeek(now, messageCalendar) -> dateFormat.format(messageDate)
            // Older
            else -> fullDateFormat.format(messageDate)
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isThisWeek(now: Calendar, messageTime: Calendar): Boolean {
        return now.get(Calendar.WEEK_OF_YEAR) == messageTime.get(Calendar.WEEK_OF_YEAR) &&
                now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR)
    }
} 