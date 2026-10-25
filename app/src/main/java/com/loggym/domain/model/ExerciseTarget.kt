package com.loggym.domain.model

import java.time.Duration

sealed interface ExerciseTarget {
    val targetSets: Int
    val rest: Duration
    val trackingType: TrackingType
    val repMin: Int? get() = null
    val repMax: Int? get() = null
    val durationSeconds: Int? get() = null
    val isRestTimerEnabled: Boolean get() = !rest.isZero

    data class Repetitions(
        override val targetSets: Int,
        override val repMin: Int,
        override val repMax: Int,
        override val rest: Duration,
    ) : ExerciseTarget {
        override val trackingType = TrackingType.WEIGHT_REPS
        init {
            validateCommon(targetSets, rest)
            require(repMin >= 0 && repMax > 0 && repMax >= repMin) { "Faixa de repetições inválida." }
        }
    }

    data class Timed(
        override val targetSets: Int,
        override val durationSeconds: Int,
        override val rest: Duration,
    ) : ExerciseTarget {
        override val trackingType = TrackingType.TIME
        init {
            validateCommon(targetSets, rest)
            require(durationSeconds > 0) { "A duração deve ser positiva." }
        }
    }
}

private fun validateCommon(sets: Int, rest: Duration) {
    require(sets > 0) { "A quantidade de séries deve ser positiva." }
    require(!rest.isNegative && rest.nano == 0 && rest.seconds <= InputRules.REST_MAX_SECONDS) {
        "Descanso inteiro entre 0 e 600 s."
    }
}

fun ExerciseTarget.summary(): String = when (this) {
    is ExerciseTarget.Repetitions -> if (repMin == repMax) "$targetSets × $repMax" else "$targetSets × $repMin–$repMax"
    is ExerciseTarget.Timed -> "$targetSets × $durationSeconds s"
}
