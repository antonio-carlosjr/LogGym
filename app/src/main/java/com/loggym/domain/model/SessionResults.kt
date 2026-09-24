package com.loggym.domain.model

sealed interface StartSessionResult {
    data class Started(val sessionId: Long) : StartSessionResult

    /** RF-63/RF-64: já existe uma sessão em andamento. */
    data class DraftAlreadyExists(val draftId: Long) : StartSessionResult

    /** RF-07/RF-34: sem plano ativo não se inicia treino. */
    data object NoActivePlan : StartSessionResult

    /** RF-58: a divisão precisa pertencer ao plano ativo. */
    data object WorkoutNotInActivePlan : StartSessionResult
}

sealed interface FinishSessionResult {
    data object Finished : FinishSessionResult

    /** RF-111: sessão sem séries realizadas não vira treino concluído. */
    data object NoCompletedSets : FinishSessionResult

    data object NotADraft : FinishSessionResult
}
