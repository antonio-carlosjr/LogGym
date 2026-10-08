package com.loggym.domain.model

import java.time.Duration

/** Regras de entrada decididas na S1-04 (docs/decisoes/S1-04.md). */
object InputRules {
    /** RF-142: descanso de 0 a 600 s em passos de 15 s. */
    const val REST_MAX_SECONDS = 600L
    const val REST_STEP_SECONDS = 15L

    /** RF-133 / P3: observação curta do usuário. */
    const val USER_NOTE_MAX_LENGTH = 80

    /** RF-140: casas decimais da carga. */
    const val WEIGHT_MAX_DECIMALS = 2

    /** RF-144: volume de cada treino dos planos pré-definidos. */
    val TEMPLATE_EXERCISES_PER_WORKOUT = 4..6
    val TEMPLATE_SETS_PER_WORKOUT = 12..20

    /** RF-143: padrão aplicado apenas quando o usuário escolhe usá-lo. */
    val DEFAULT_TARGET = ExerciseTarget(
        targetSets = 3,
        repMin = 8,
        repMax = 12,
        rest = Duration.ofSeconds(90),
    )
}
