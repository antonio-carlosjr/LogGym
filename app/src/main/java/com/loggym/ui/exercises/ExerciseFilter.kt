package com.loggym.ui.exercises

import com.loggym.domain.logic.ExerciseNames
import com.loggym.domain.model.Exercise
import com.loggym.domain.model.MuscleGroup

/**
 * Busca por nome, ignorando acentos e maiúsculas, e filtro por grupo.
 * Um exercício com vários grupos aparece se qualquer um deles bater (pendência P2).
 */
fun filterExercises(exercises: List<Exercise>, query: String, group: MuscleGroup?): List<Exercise> {
    val key = ExerciseNames.searchKey(query)
    return exercises.filter { exercise ->
        (group == null || group in exercise.muscleGroups) &&
            (key.isEmpty() || ExerciseNames.searchKey(exercise.name).contains(key))
    }
}
