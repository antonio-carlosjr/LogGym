package com.loggym.ui.util

import java.time.Instant
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

fun Instant.formatTime(): String = timeFormatter.format(this)

fun Instant.formatSessionDate(zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(zone).format(this)

fun sessionDurationSeconds(startedAt: Instant, finishedAt: Instant?): Long? =
    finishedAt?.let { Duration.between(startedAt, it).takeUnless { duration -> duration.isNegative }?.seconds }
