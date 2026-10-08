package com.loggym.domain.model

import com.loggym.domain.logic.RestTimer
import java.time.Instant

enum class SessionStatus { DRAFT, COMPLETED }

/**
 * O que efetivamente aconteceu (seção 1). Todos os nomes e metas são snapshots
 * copiados no início da sessão (RF-61, RF-129), nunca referências vivas ao plano.
 */
data class Session(
    val id: Long,
    val status: SessionStatus,
    val planName: String,
    val workoutName: String,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val restTimer: RestTimer?,
    val exercises: List<SessionExercise>,
) {
    val completedSetCount: Int get() = exercises.sumOf { exercise -> exercise.sets.count { it.isCompleted } }
    val pendingSetCount: Int get() = exercises.sumOf { exercise -> exercise.sets.count { !it.isCompleted } }
}

/** Fotografia histórica da prescrição unida à execução (seção 19, conceito 3). */
data class SessionExercise(
    val id: Long,
    /** Referência fraca ao exercício de origem, usada só para o pré-preenchimento (RF-68). */
    val exerciseId: Long?,
    val name: String,
    val muscleGroups: Set<MuscleGroup>,
    val position: Int,
    val target: ExerciseTarget,
    val sets: List<SessionSet>,
)

data class SessionSet(
    val id: Long,
    val position: Int,
    val weightKg: Double?,
    val reps: Int?,
    val isCompleted: Boolean,
)

data class SessionSummary(
    val id: Long,
    val planName: String,
    val workoutName: String,
    val startedAt: Instant,
    val finishedAt: Instant?,
)
