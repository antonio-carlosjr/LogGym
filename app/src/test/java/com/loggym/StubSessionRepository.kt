package com.loggym

import com.loggym.domain.logic.SetValues
import com.loggym.domain.model.*
import com.loggym.domain.repository.SessionRepository
import kotlinx.coroutines.flow.flowOf

/** Falha em operações não previstas: testes de consulta nunca devem produzir escritas. */
open class StubSessionRepository : SessionRepository {
    override fun observeDraft() = flowOf<Session?>(null)
    override fun observeSession(sessionId: Long) = flowOf<Session?>(null)
    override fun observeHistory() = flowOf(emptyList<SessionSummary>())
    override suspend fun getExerciseHistoryPage(exerciseId: Long, trackingType: TrackingType, cursor: ExerciseHistoryCursor?): ExerciseHistoryPage = error("Consulta não configurada")
    override suspend fun startSession(workoutId: Long): StartSessionResult = error("Escrita inesperada")
    override suspend fun updateSetValues(setId: Long, values: SetValues): Unit = error("Escrita inesperada")
    override suspend fun saveSetAndSetCompleted(setId: Long, values: SetValues, completed: Boolean): Unit = error("Escrita inesperada")
    override suspend fun addSet(sessionExerciseId: Long): Unit = error("Escrita inesperada")
    override suspend fun removeSet(setId: Long): Boolean = error("Escrita inesperada")
    override suspend fun clearExerciseSets(sessionExerciseId: Long): Boolean = error("Escrita inesperada")
    override suspend fun skipRest(sessionId: Long): Unit = error("Escrita inesperada")
    override suspend fun finishSession(sessionId: Long): FinishSessionResult = error("Escrita inesperada")
    override suspend fun discardSession(sessionId: Long): Boolean = error("Escrita inesperada")
}
