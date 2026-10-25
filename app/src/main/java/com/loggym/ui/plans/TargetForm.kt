package com.loggym.ui.plans

import com.loggym.domain.model.TrackingType
import com.loggym.domain.model.ExerciseTarget
import com.loggym.domain.model.InputRules
import java.time.Duration

enum class TargetFormError { MISSING_SETS, INVALID_SETS, MISSING_REPS, INVALID_REPS, MIN_GREATER_THAN_MAX, INVALID_DURATION, INVALID_REST }

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
    val restSeconds: String = "",
    val trackingType: TrackingType = TrackingType.WEIGHT_REPS,
    val durationSeconds: String = "",
) {
    fun withDefault(): TargetForm = from(if (trackingType == TrackingType.TIME) ExerciseTarget.Timed(3, 30, Duration.ofSeconds(90)) else InputRules.DEFAULT_TARGET)

    val restValue: Long? get() = if (restSeconds.isBlank()) 0 else restSeconds.toLongOrNull()
        ?.takeIf { it in 0..InputRules.REST_MAX_SECONDS }

    /** Os botões somam 15, sem arredondar a entrada manual. */
    fun adjustRest(steps: Int): TargetForm = restValue?.let {
        copy(restSeconds = (it + steps * InputRules.REST_STEP_SECONDS)
            .coerceIn(0, InputRules.REST_MAX_SECONDS).toString())
    } ?: this

    /** RF-50 a RF-53. Não há limite superior (RF-141). */
    fun validate(): TargetFormResult {
        if (sets.isBlank()) return TargetFormResult.Invalid(TargetFormError.MISSING_SETS)
        val setsValue = sets.toIntOrNull()?.takeIf { it > 0 }
            ?: return TargetFormResult.Invalid(TargetFormError.INVALID_SETS)
        val rest = restValue ?: return TargetFormResult.Invalid(TargetFormError.INVALID_REST)
        if (trackingType == TrackingType.TIME) {
            val duration = durationSeconds.toIntOrNull()?.takeIf { it > 0 }
                ?: return TargetFormResult.Invalid(TargetFormError.INVALID_DURATION)
            return TargetFormResult.Valid(ExerciseTarget.Timed(setsValue, duration, Duration.ofSeconds(rest)))
        }
        if (repMin.isBlank() || repMax.isBlank()) return TargetFormResult.Invalid(TargetFormError.MISSING_REPS)
        val min = repMin.toIntOrNull()?.takeIf { it > 0 } ?: return TargetFormResult.Invalid(TargetFormError.INVALID_REPS)
        val max = repMax.toIntOrNull()?.takeIf { it > 0 } ?: return TargetFormResult.Invalid(TargetFormError.INVALID_REPS)
        if (min > max) return TargetFormResult.Invalid(TargetFormError.MIN_GREATER_THAN_MAX)
        return TargetFormResult.Valid(ExerciseTarget.Repetitions(setsValue, min, max, Duration.ofSeconds(rest)))
    }

    companion object {
        /** Limite de dígitos só para o layout (pendência P8). */
        const val MAX_DIGITS = 4

        fun from(target: ExerciseTarget) = TargetForm(
            sets = target.targetSets.toString(),
            repMin = target.repMin?.toString().orEmpty(),
            repMax = target.repMax?.toString().orEmpty(),
            durationSeconds = target.durationSeconds?.toString().orEmpty(),
            trackingType = target.trackingType,
            restSeconds = target.rest.seconds.toString(),
        )

        /** Mantém só dígitos, até [MAX_DIGITS]. */
        fun digitsOnly(input: String): String = input.filter(Char::isDigit).take(MAX_DIGITS)
    }
}
