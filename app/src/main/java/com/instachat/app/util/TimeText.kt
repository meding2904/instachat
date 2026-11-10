package com.instachat.app.util

import android.content.Context
import android.text.format.DateFormat
import com.instachat.app.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object TimeText {

    fun zone(): ZoneId = ZoneId.systemDefault()

    fun localDate(timestamp: Long): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(zone()).toLocalDate()

    fun isSameDay(first: Long, second: Long): Boolean =
        localDate(first) == localDate(second)

    fun messageTime(context: Context, timestamp: Long): String {
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        return formatter(pattern).format(
            Instant.ofEpochMilli(timestamp).atZone(zone())
        )
    }

    fun inboxTime(context: Context, timestamp: Long): String {
        val date = localDate(timestamp)
        val today = LocalDate.now(zone())
        return when {
            date == today -> messageTime(context, timestamp)
            date == today.minusDays(1) -> context.getString(R.string.yesterday)
            ChronoUnit.DAYS.between(date, today) in 2..6 ->
                formatter("EEEE").format(date)
            date.year == today.year -> formatter("MMM d").format(date)
            else -> formatter("MMM d, yyyy").format(date)
        }
    }

    fun dateSeparator(context: Context, timestamp: Long): String {
        val date = localDate(timestamp)
        val today = LocalDate.now(zone())
        return when {
            date == today -> context.getString(R.string.today)
            date == today.minusDays(1) -> context.getString(R.string.yesterday)
            date.year == today.year -> formatter("EEEE, MMM d").format(date)
            else -> formatter("MMM d, yyyy").format(date)
        }
    }

    /** Centered phase separator in the reference style, e.g. FRI, 13:12. */
    fun chatSeparator(context: Context, timestamp: Long): String {
        val date = localDate(timestamp)
        val today = LocalDate.now(zone())
        val prefix = when (date) {
            today -> "TODAY"
            today.minusDays(1) -> "YESTERDAY"
            else -> formatter("EEE").format(date).uppercase(Locale.getDefault())
        }
        val time = messageTime(context, timestamp).uppercase(Locale.getDefault())
        return if (prefix == "TODAY" || prefix == "YESTERDAY") {
            "$prefix $time"
        } else {
            "$prefix, $time"
        }
    }

    /** Day name for the Seen receipt, e.g. "Seen Friday". */
    fun seenDayLabel(timestamp: Long): String {
        val date = localDate(timestamp)
        val today = LocalDate.now(zone())
        return when {
            date == today -> "Today"
            date == today.minusDays(1) -> "Yesterday"
            ChronoUnit.DAYS.between(date, today) in 2..6 ->
                formatter("EEEE").format(date)
            date.year == today.year -> formatter("MMM d").format(date)
            else -> formatter("MMM d, yyyy").format(date)
        }
    }

    fun fullDateTime(context: Context, timestamp: Long): String {
        val pattern = if (DateFormat.is24HourFormat(context)) {
            "MMM d, yyyy · HH:mm"
        } else {
            "MMM d, yyyy · h:mm a"
        }
        return formatter(pattern).format(Instant.ofEpochMilli(timestamp).atZone(zone()))
    }

    fun shortDate(timestamp: Long): String =
        formatter("MMM d, yyyy").format(localDate(timestamp))

    fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    fun startOfDay(timestamp: Long): Long =
        localDate(timestamp).atStartOfDay(zone()).toInstant().toEpochMilli()

    fun atTime(date: LocalDate, hour: Int, minute: Int): Long =
        LocalDateTime.of(date, java.time.LocalTime.of(hour, minute))
            .atZone(zone())
            .toInstant()
            .toEpochMilli()

    private fun formatter(pattern: String): DateTimeFormatter =
        DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
}
