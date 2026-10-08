package com.loggym.ui.plans

import com.loggym.domain.model.ExerciseTarget
import com.loggym.domain.model.InputRules
import java.time.Duration

enum class TargetFormError { MISSING_SETS, INVALID_SETS, MISSING_REPS, INVALID_REPS, MIN_GREATER_THAN_MAX }

sealed interface TargetFormResult {
    data class Valid(val target: ExerciseTarget) : TargetFormResult

    data class Invalid(val error: TargetFormError) : TargetFormResult
}

/**
 * Campos da prescrição de um exercício na divisão (seção 6).
 * Começa vazio; o padrão só é aplicado se o usuário escolher (RF-143).
 */
data class TargetForm(
    val sets: String = "",
    val repMin: String = "",
    val repMax: String = "",
    val restSeconds: Long = 0,
) {
    fun withDefault(): TargetForm = from(InputRules.DEFAULT_TARGET)

    /** RF-142: passos de 15 s entre 0 e 600 s. */
    fun adjustRest(steps: Int): TargetForm = copy(
        restSeconds = (restSeconds + steps * InputRules.REST_STEP_SECONDS).coerceIn(0, InputRules.REST_MAX_SECONDS),
    )

    /** RF-50 a RF-53. Não há limite superior (RF-141). */
    fun validate(): TargetFormResult {
        if (sets.isBlank()) return TargetFormResult.Invalid(TargetFormError.MISSING_SETS)
        val setsValue = sets.toIntOrNull()?.takeIf { it > 0 }
            ?: return TargetFormResult.Invalid(TargetFormError.INVALID_SETS)
        if (repMin.isBlank() || repMax.isBlank()) return TargetFormResult.Invalid(TargetFormError.MISSING_REPS)
        val min = repMin.toIntOrNull()?.takeIf { it > 0 } ?: return TargetFormResult.Invalid(TargetFormError.INVALID_REPS)
        val max = repMax.toIntOrNull()?.takeIf { it > 0 } ?: return TargetFormResult.Invalid(TargetFormError.INVALID_REPS)
        if (min > max) return TargetFormResult.Invalid(TargetFormError.MIN_GREATER_THAN_MAX)
        return TargetFormResult.Valid(ExerciseTarget(setsValue, min, max, Duration.ofSeconds(restSeconds)))
    }

    companion object {
        /** Limite de dígitos só para o layout (pendência P8). */
        const val MAX_DIGITS = 4

        fun from(target: ExerciseTarget) = TargetForm(
            sets = target.targetSets.toString(),
            repMin = target.repMin.toString(),
            repMax = target.repMax.toString(),
            restSeconds = target.rest.seconds,
        )

        /** Mantém só dígitos, até [MAX_DIGITS]. */
        fun digitsOnly(input: String): String = input.filter(Char::isDigit).take(MAX_DIGITS)
    }
}
