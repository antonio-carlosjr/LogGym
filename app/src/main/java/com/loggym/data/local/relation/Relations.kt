package com.loggym.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.loggym.data.local.entity.ExerciseEntity
import com.loggym.data.local.entity.PlanEntity
import com.loggym.data.local.entity.SessionEntity
import com.loggym.data.local.entity.SessionExerciseEntity
import com.loggym.data.local.entity.SessionSetEntity
import com.loggym.data.local.entity.WorkoutEntity
import com.loggym.data.local.entity.WorkoutExerciseEntity

data class WorkoutExerciseWithExercise(
    @Embedded val workoutExercise: WorkoutExerciseEntity,
    @Relation(parentColumn = "exercise_id", entityColumn = "id")
    val exercise: ExerciseEntity,
)

data class WorkoutWithExercises(
    @Embedded val workout: WorkoutEntity,
    @Relation(
        entity = WorkoutExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "workout_id",
    )
    val exercises: List<WorkoutExerciseWithExercise>,
)

data class PlanWithWorkouts(
    @Embedded val plan: PlanEntity,
    @Relation(
        entity = WorkoutEntity::class,
        parentColumn = "id",
        entityColumn = "plan_id",
    )
    val workouts: List<WorkoutWithExercises>,
)

data class SessionExerciseWithSets(
    @Embedded val exercise: SessionExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "session_exercise_id")
    val sets: List<SessionSetEntity>,
)

data class SessionWithExercises(
    @Embedded val session: SessionEntity,
    @Relation(
        entity = SessionExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "session_id",
    )
    val exercises: List<SessionExerciseWithSets>,
)
