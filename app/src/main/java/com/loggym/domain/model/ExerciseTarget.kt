package com.loggym.domain.model

import java.time.Duration

/**
 * Prescrição de um exercício dentro de um treino (seção 6).
 * Valida as regras RF-50, RF-52, RF-53, RF-55/56 e RF-142 na construção.
 * Não há limite superior para séries e repetições (RF-141).
 */
data class ExerciseTarget(
    val targetSets: Int,
    val repMin: Int,
    val repMax: Int,
    val rest: Duration,
) {
    init {
        require(targetSets > 0) { "A meta de séries deve ser um inteiro positivo (RF-50)." }
        require(repMin > 0 && repMax > 0) { "Repetições devem ser inteiros positivos (RF-53)." }
        require(repMin <= repMax) { "Repetição mínima não pode exceder a máxima (RF-52)." }
        require(!rest.isNegative) { "O descanso não pode ser negativo (RF-55)." }
        require(rest.seconds <= InputRules.REST_MAX_SECONDS) { "O descanso máximo é de 600 s (RF-142)." }
        require(rest.nano == 0 && rest.seconds % InputRules.REST_STEP_SECONDS == 0L) {
            "O descanso deve variar em passos de 15 s (RF-142)."
        }
    }

    /** RF-56: descanso zero desativa o cronômetro deste exercício. */
    val isRestTimerEnabled: Boolean get() = !rest.isZero
}
