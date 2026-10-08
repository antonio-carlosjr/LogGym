package com.loggym.domain.repository

import com.loggym.domain.model.Exercise
import com.loggym.domain.model.MuscleGroup
import kotlinx.coroutines.flow.Flow

/** Dados editáveis de um exercício personalizado. */
data class ExerciseDraft(
    val name: String,
    val muscleGroups: Set<MuscleGroup> = emptySet(),
    val description: String? = null,
)

sealed interface SaveExerciseResult {
    data class Saved(val exerciseId: Long) : SaveExerciseResult

    /** RF-14: todo exercício precisa de nome. */
    data object BlankName : SaveExerciseResult

    /** Exercício inexistente ou nativo (RF-18). */
    data object NotEditable : SaveExerciseResult
}

interface ExerciseRepository {
    fun observeAll(): Flow<List<Exercise>>

    suspend fun createCustom(draft: ExerciseDraft): SaveExerciseResult

    suspend fun updateCustom(exerciseId: Long, draft: ExerciseDraft): SaveExerciseResult

    /** RF-133: observação de até 80 caracteres; texto em branco remove a observação. */
    suspend fun updateUserNote(exerciseId: Long, note: String?)

    /** Quantidade de divisões que usam o exercício, para o aviso do RF-21. */
    suspend fun countPlanUsages(exerciseId: Long): Int

    /** Retorna false se o exercício não existir ou for nativo (RF-19). */
    suspend fun deleteCustom(exerciseId: Long): Boolean
}
