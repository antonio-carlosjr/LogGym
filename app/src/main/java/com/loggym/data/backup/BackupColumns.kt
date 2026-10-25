package com.loggym.data.backup

/** Esquema lógico v1: mapeamento explícito, nunca nomes SQL fornecidos pelo arquivo. */
object BackupColumns {
    data class Column(val name: String, val type: String, val nullable: Boolean)
    val all = mapOf(
        "exercises" to listOf(Column("id", "INTEGER", false), Column("name", "TEXT", false), Column("muscle_groups", "TEXT", false), Column("description", "TEXT", true), Column("user_note", "TEXT", true), Column("is_custom", "INTEGER", false), Column("native_key", "TEXT", true), Column("tracking_type", "TEXT", false)),
        "plans" to listOf(Column("id", "INTEGER", false), Column("name", "TEXT", false), Column("is_active", "INTEGER", false), Column("template_key", "TEXT", true), Column("created_at", "INTEGER", false)),
        "workouts" to listOf(Column("id", "INTEGER", false), Column("plan_id", "INTEGER", false), Column("name", "TEXT", false), Column("position", "INTEGER", false)),
        "workout_exercises" to listOf(Column("id", "INTEGER", false), Column("workout_id", "INTEGER", false), Column("exercise_id", "INTEGER", false), Column("position", "INTEGER", false), Column("target_sets", "INTEGER", false), Column("rep_min", "INTEGER", true), Column("rep_max", "INTEGER", true), Column("target_duration_seconds", "INTEGER", true), Column("rest_seconds", "INTEGER", false)),
        "sessions" to listOf(Column("id", "INTEGER", false), Column("status", "TEXT", false), Column("source_plan_id", "INTEGER", true), Column("plan_name", "TEXT", false), Column("source_workout_id", "INTEGER", true), Column("workout_name", "TEXT", false), Column("started_at", "INTEGER", false), Column("finished_at", "INTEGER", true), Column("rest_started_at", "INTEGER", true), Column("rest_ends_at", "INTEGER", true)),
        "session_exercises" to listOf(Column("id", "INTEGER", false), Column("session_id", "INTEGER", false), Column("exercise_id", "INTEGER", true), Column("exercise_name", "TEXT", false), Column("muscle_groups", "TEXT", false), Column("position", "INTEGER", false), Column("target_sets", "INTEGER", false), Column("rep_min", "INTEGER", true), Column("rep_max", "INTEGER", true), Column("target_duration_seconds", "INTEGER", true), Column("rest_seconds", "INTEGER", false), Column("tracking_type", "TEXT", false)),
        "session_sets" to listOf(Column("id", "INTEGER", false), Column("session_exercise_id", "INTEGER", false), Column("position", "INTEGER", false), Column("weight_kg", "REAL", true), Column("reps", "INTEGER", true), Column("duration_seconds", "INTEGER", true), Column("is_completed", "INTEGER", false))
    )
}
