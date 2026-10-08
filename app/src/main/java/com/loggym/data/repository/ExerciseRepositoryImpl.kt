package com.loggym.data.repository

import androidx.room.withTransaction
import com.loggym.data.local.LogGymDatabase
import com.loggym.data.local.dao.ExerciseDao
import com.loggym.data.local.entity.ExerciseEntity
import com.loggym.data.mapper.toDomain
import com.loggym.domain.logic.ExerciseNames
import com.loggym.domain.model.Exercise
import com.loggym.domain.model.InputRules
import com.loggym.domain.repository.ExerciseDraft
import com.loggym.domain.repository.ExerciseRepository
import com.loggym.domain.repository.SaveExerciseResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExerciseRepositoryImpl @Inject constructor(
    private val database: LogGymDatabase,
    private val exerciseDao: ExerciseDao,
) : ExerciseRepository {

    override fun observeAll(): Flow<List<Exercise>> =
        exerciseDao.observeAll().map { exercises -> exercises.map { it.toDomain() } }

    override suspend fun createCustom(draft: ExerciseDraft): SaveExerciseResult = database.withTransaction {
        val name = ExerciseNames.clean(draft.name)
        if (name.isEmpty()) return@withTransaction SaveExerciseResult.BlankName
        if (isNameTaken(name, exceptId = null)) return@withTransaction SaveExerciseResult.DuplicateName
        val id = exerciseDao.insert(
            ExerciseEntity(
                name = name,
                muscleGroups = draft.muscleGroups,
                description = draft.description.cleanOrNull(),
                isCustom = true,
                nativeKey = null,
            ),
        )
        SaveExerciseResult.Saved(id)
    }

    override suspend fun updateCustom(exerciseId: Long, draft: ExerciseDraft): SaveExerciseResult =
        database.withTransaction {
            val name = ExerciseNames.clean(draft.name)
            if (name.isEmpty()) return@withTransaction SaveExerciseResult.BlankName
            if (isNameTaken(name, exceptId = exerciseId)) return@withTransaction SaveExerciseResult.DuplicateName
            val updated = exerciseDao.updateCustom(
                exerciseId = exerciseId,
                name = name,
                muscleGroups = draft.muscleGroups,
                description = draft.description.cleanOrNull(),
            )
            if (updated > 0) SaveExerciseResult.Saved(exerciseId) else SaveExerciseResult.NotEditable
        }

    override suspend fun updateUserNote(exerciseId: Long, note: String?) {
        val cleaned = note.cleanOrNull()
        require(cleaned == null || cleaned.length <= InputRules.USER_NOTE_MAX_LENGTH) {
            "A observação tem no máximo ${InputRules.USER_NOTE_MAX_LENGTH} caracteres (RF-133)."
        }
        exerciseDao.updateUserNote(exerciseId, cleaned)
    }

    override suspend fun countPlanUsages(exerciseId: Long): Int = exerciseDao.countPlanUsages(exerciseId)

    override suspend fun deleteCustom(exerciseId: Long): Boolean = exerciseDao.deleteCustom(exerciseId) > 0

    /** RF-132: compara com nativos e personalizados, exceto o próprio exercício em edição. */
    private suspend fun isNameTaken(name: String, exceptId: Long?): Boolean {
        val key = ExerciseNames.comparisonKey(name)
        return exerciseDao.getAll().any { it.id != exceptId && ExerciseNames.comparisonKey(it.name) == key }
    }

    private fun String?.cleanOrNull(): String? = this?.trim()?.ifEmpty { null }
}
