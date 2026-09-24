package com.loggym.domain.repository

import com.loggym.domain.model.Exercise
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun observeAll(): Flow<List<Exercise>>

    suspend fun createCustom(name: String, muscleGroup: String?): Long

    /** Retorna false se o exercício não existir ou for nativo (RF-18). */
    suspend fun updateCustom(exerciseId: Long, name: String, muscleGroup: String?): Boolean

    /** Quantidade de divisões que usam o exercício, para o aviso do RF-21. */
    suspend fun countPlanUsages(exerciseId: Long): Int

    /** Retorna false se o exercício não existir ou for nativo (RF-19). */
    suspend fun deleteCustom(exerciseId: Long): Boolean
}
