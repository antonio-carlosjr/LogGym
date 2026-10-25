package com.loggym.domain.model

import java.time.Instant

data class ExerciseHistoryCursor(val startedAt: Instant, val sessionId: Long)
data class ExerciseHistoryEntry(val session: SessionSummary, val occurrences: List<SessionExercise>)
data class ExerciseHistoryPage(val entries: List<ExerciseHistoryEntry>, val nextCursor: ExerciseHistoryCursor?)
