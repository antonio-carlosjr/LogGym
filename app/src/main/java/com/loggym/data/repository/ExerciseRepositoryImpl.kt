package com.loggym.data.repository

import com.loggym.data.local.dao.ExerciseDao
import com.loggym.data.local.entity.ExerciseEntity
import com.loggym.data.mapper.toDomain
import com.loggym.domain.model.Exercise
import com.loggym.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao,
) : ExerciseRepository {

    override fun observeAll(): Flow<List<Exercise>> =
        exerciseDao.observeAll().map { exercises -> exercises.map { it.toDomain() } }

    override suspend fun createCustom(name: String, muscleGroup: String?): Long =
        exerciseDao.insert(
            ExerciseEntity(
                name = requireName(name),
                muscleGroup = muscleGroup?.trim()?.ifBlank { null },
                isCustom = true,
                nativeKey = null,
            ),
        )

    override suspend fun updateCustom(exerciseId: Long, name: String, muscleGroup: String?): Boolean =
        exerciseDao.updateCustom(exerciseId, requireName(name), muscleGroup?.trim()?.ifBlank { null }) > 0

    override suspend fun countPlanUsages(exerciseId: Long): Int = exerciseDao.countPlanUsages(exerciseId)

    override suspend fun deleteCustom(exerciseId: Long): Boolean = exerciseDao.deleteCustom(exerciseId) > 0
}
