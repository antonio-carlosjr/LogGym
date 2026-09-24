package com.loggym.data.mapper

import com.loggym.data.local.entity.ExerciseEntity
import com.loggym.data.local.entity.PlanEntity
import com.loggym.data.local.entity.SessionEntity
import com.loggym.data.local.entity.SessionSetEntity
import com.loggym.data.local.relation.PlanWithWorkouts
import com.loggym.data.local.relation.SessionExerciseWithSets
import com.loggym.data.local.relation.SessionWithExercises
import com.loggym.data.local.relation.WorkoutWithExercises
import com.loggym.domain.logic.RestTimer
import com.loggym.domain.model.Exercise
import com.loggym.domain.model.ExerciseTarget
import com.loggym.domain.model.Plan
import com.loggym.domain.model.PlanSummary
import com.loggym.domain.model.Session
import com.loggym.domain.model.SessionExercise
import com.loggym.domain.model.SessionSet
import com.loggym.domain.model.SessionSummary
import com.loggym.domain.model.Workout
import com.loggym.domain.model.WorkoutExercise
import java.time.Duration

fun ExerciseEntity.toDomain() = Exercise(
    id = id,
    name = name,
    muscleGroup = muscleGroup,
    isCustom = isCustom,
)

fun PlanEntity.toSummary() = PlanSummary(id = id, name = name, isActive = isActive)

fun PlanWithWorkouts.toDomain() = Plan(
    id = plan.id,
    name = plan.name,
    isActive = plan.isActive,
    workouts = workouts.map { it.toDomain() }.sortedBy { it.position },
)

fun WorkoutWithExercises.toDomain() = Workout(
    id = workout.id,
    planId = workout.planId,
    name = workout.name,
    position = workout.position,
    exercises = exercises
        .map { item ->
            val prescription = item.workoutExercise
            WorkoutExercise(
                id = prescription.id,
                exercise = item.exercise.toDomain(),
                position = prescription.position,
                target = exerciseTarget(prescription.targetSets, prescription.repMin, prescription.repMax, prescription.restSeconds),
            )
        }
        .sortedBy { it.position },
)

fun SessionEntity.toSummary() = SessionSummary(
    id = id,
    planName = planName,
    workoutName = workoutName,
    startedAt = startedAt,
    finishedAt = finishedAt,
)

fun SessionWithExercises.toDomain() = Session(
    id = session.id,
    status = session.status,
    planName = session.planName,
    workoutName = session.workoutName,
    startedAt = session.startedAt,
    finishedAt = session.finishedAt,
    restTimer = restTimerOf(session),
    exercises = exercises.map { it.toDomain() }.sortedBy { it.position },
)

private fun SessionExerciseWithSets.toDomain() = SessionExercise(
    id = exercise.id,
    exerciseId = exercise.exerciseId,
    name = exercise.exerciseName,
    muscleGroup = exercise.muscleGroup,
    position = exercise.position,
    target = exerciseTarget(exercise.targetSets, exercise.repMin, exercise.repMax, exercise.restSeconds),
    sets = sets.map { it.toDomain() }.sortedBy { it.position },
)

private fun SessionSetEntity.toDomain() = SessionSet(
    id = id,
    position = position,
    weightKg = weightKg,
    reps = reps,
    isCompleted = isCompleted,
)

private fun restTimerOf(session: SessionEntity): RestTimer? {
    val startedAt = session.restStartedAt ?: return null
    val endsAt = session.restEndsAt ?: return null
    return RestTimer(startedAt = startedAt, endsAt = endsAt)
}

private fun exerciseTarget(targetSets: Int, repMin: Int, repMax: Int, restSeconds: Int) =
    ExerciseTarget(targetSets, repMin, repMax, Duration.ofSeconds(restSeconds.toLong()))
