package com.whirlyfan.breezy

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.compareTo

fun formatTimestamp(timestamp: String): String =
    try {
        val instant = Instant.parse(timestamp)
        val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val now = LocalDateTime.now()

        val minutes = ChronoUnit.MINUTES.between(dateTime, now)
        val hours = ChronoUnit.HOURS.between(dateTime, now)
        val days = ChronoUnit.DAYS.between(dateTime, now)

        when {
            minutes < 1 -> "Just now"
            minutes < 2 -> "1 minute ago"
            minutes < 60 -> "$minutes minutes ago"
            hours < 2 -> "1 hour ago"
            hours < 24 -> "$hours hours ago"
            days < 2 -> "1 day ago"
            days < 7 -> "$days days ago"
            else -> dateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }
    } catch (e: Exception) {
        timestamp
    }

fun formatShortTimeStamp(timestamp: String): String =
    try {
        val instant = Instant.parse(timestamp)
        val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val now = LocalDateTime.now()

        val seconds = ChronoUnit.SECONDS.between(dateTime, now)
        val minutes = ChronoUnit.MINUTES.between(dateTime, now)
        val hours = ChronoUnit.HOURS.between(dateTime, now)
        val days = ChronoUnit.DAYS.between(dateTime, now)
        val weeks = ChronoUnit.WEEKS.between(dateTime, now)
        val months = ChronoUnit.MONTHS.between(dateTime, now)
        val years = ChronoUnit.YEARS.between(dateTime, now)

        when {
            seconds < 60 -> "${seconds}s"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            days < 7 -> "${days}d"
            weeks < 4 -> "${weeks}w"
            months < 12 -> "${months}mo"
            else -> "${years}y"
        }
    } catch (e: Exception) {
        timestamp
    }