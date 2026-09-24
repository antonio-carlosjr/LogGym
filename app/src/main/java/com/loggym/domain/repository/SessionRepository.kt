package com.loggym.domain.repository

import com.loggym.domain.model.FinishSessionResult
import com.loggym.domain.model.Session
import com.loggym.domain.model.SessionSummary
import com.loggym.domain.model.StartSessionResult
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    /** RF-101: rascunho em andamento, se existir. */
    fun observeDraft(): Flow<Session?>

    /** RF-116/RF-117: apenas sessões finalizadas. */
    fun observeHistory(): Flow<List<SessionSummary>>

    fun observeSession(sessionId: Long): Flow<Session?>

    /** RF-58 a RF-72: cria o rascunho com snapshot e séries pré-preenchidas. */
    suspend fun startSession(workoutId: Long): StartSessionResult

    suspend fun updateSetValues(setId: Long, weightKg: Double?, reps: Int?)

    /** RF-80/RF-82; ao concluir, reinicia o cronômetro de descanso (RF-87, RF-91). */
    suspend fun setSetCompleted(setId: Long, completed: Boolean)

    /** RF-83: adiciona uma série extra apenas nesta sessão. */
    suspend fun addSet(sessionExerciseId: Long)

    /** RF-84: remove uma série ainda não concluída. */
    suspend fun removeSet(setId: Long): Boolean

    /** RF-92: encerra manualmente o descanso. */
    suspend fun skipRest(sessionId: Long)

    /** RF-107 a RF-115: converte o rascunho em registro definitivo de forma atômica. */
    suspend fun finishSession(sessionId: Long): FinishSessionResult

    /** RF-103/RF-105: o rascunho descartado não entra no histórico. */
    suspend fun discardSession(sessionId: Long): Boolean
}
