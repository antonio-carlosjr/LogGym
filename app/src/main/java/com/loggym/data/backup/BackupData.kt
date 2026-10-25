package com.loggym.data.backup

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import com.loggym.data.local.Converters
import com.loggym.data.seed.NativeExercises
import com.loggym.domain.backup.BackupMode
import com.loggym.domain.backup.BackupSummary
import com.loggym.domain.logic.ExerciseNames
import com.loggym.domain.logic.SetValues
import com.loggym.domain.model.*
import kotlinx.serialization.json.*
import java.time.Duration

internal object BackupData {
    fun rows(db: SupportSQLiteDatabase, mode: BackupMode): Sequence<BackupRow> = sequence {
        for (table in BackupFormat.tables.take(if (mode == BackupMode.FULL) 7 else 4)) {
            val columns = BackupColumns.all.getValue(table)
            db.query("SELECT ${columns.joinToString { "`${it.name}`" }} FROM `$table` ORDER BY id").use { c ->
                while (c.moveToNext()) yield(BackupRow(table, buildJsonObject {
                    columns.forEachIndexed { i, column -> put(column.name, when {
                        c.isNull(i) -> JsonNull
                        column.type == "TEXT" -> JsonPrimitive(c.getString(i))
                        column.type == "REAL" -> JsonPrimitive(c.getDouble(i))
                        else -> JsonPrimitive(c.getLong(i))
                    }) }
                }))
            }
        }
    }
    fun floors(db: SupportSQLiteDatabase): Map<String, Long> = BackupFormat.tables.associateWith { table ->
        db.query("SELECT seq FROM sqlite_sequence WHERE name = ?", arrayOf(table)).use { if (it.moveToFirst()) it.getLong(0) else 0L }
    }
    fun insert(db: SupportSQLiteDatabase, row: BackupRow) {
        val columns = BackupColumns.all.getValue(row.table)
        require(row.values.keys == columns.map { it.name }.toSet()) { "Campos inválidos em ${row.table}" }
        val values: List<Any?> = columns.map { column ->
            val value = row.values.getValue(column.name)
            if (value == JsonNull) { require(column.nullable); null }
            else {
                val primitive = value.jsonPrimitive
                when (column.type) {
                    "TEXT" -> { require(primitive.isString); primitive.content }
                    "REAL" -> { require(!primitive.isString); requireNotNull(primitive.doubleOrNull).also { require(it.isFinite() && it >= 0) } }
                    else -> { require(!primitive.isString); requireNotNull(primitive.longOrNull) }
                }
            }
        }
        fun long(name: String) = row.values[name]?.jsonPrimitive?.longOrNull
        require(long("id")!! > 0 && long("id")!! < Long.MAX_VALUE)
        listOf("position", "rep_min").forEach { key -> require(long(key) == null || long(key)!! in 0..Int.MAX_VALUE.toLong()) }
        listOf("target_sets", "rep_max", "target_duration_seconds", "duration_seconds").forEach { key ->
            require(long(key) == null || long(key)!! in 1..Int.MAX_VALUE.toLong())
        }
        listOf("is_active", "is_custom", "is_completed").forEach { key -> require(long(key) == null || long(key)!! in 0L..1L) }
        listOf("plan_id", "workout_id", "exercise_id", "session_id", "session_exercise_id", "source_plan_id", "source_workout_id").forEach { key ->
            require(long(key) == null || long(key)!! > 0)
        }
        require(long("reps") == null || long("reps")!! in 0..Int.MAX_VALUE.toLong())
        require(long("rest_seconds") == null || long("rest_seconds")!! in 0L..600L)
        for (name in listOf("name", "exercise_name", "plan_name", "workout_name"))
            row.values[name]?.let { require(it.jsonPrimitive.content.isNotBlank()) }
        row.values["muscle_groups"]?.let {
            val keys = it.jsonPrimitive.content
            require(keys.isEmpty() || keys.split(',').all { key -> MuscleGroup.fromKey(key) != null })
        }
        row.values["tracking_type"]?.let { TrackingType.valueOf(it.jsonPrimitive.content) }
        row.values["user_note"]?.takeIf { it != JsonNull }?.let { require(it.jsonPrimitive.content.length <= InputRules.USER_NOTE_MAX_LENGTH) }
        db.execSQL("INSERT INTO `${row.table}` (${columns.joinToString { "`${it.name}`" }}) VALUES (${columns.joinToString { "?" }})", values.toTypedArray())
    }
    fun clearBusiness(db: SupportSQLiteDatabase) { BackupFormat.tables.asReversed().forEach { db.execSQL("DELETE FROM `$it`") } }
    fun restoreFloors(db: SupportSQLiteDatabase, imported: Map<String, Long>, previous: Map<String, Long>) {
        for (table in BackupFormat.tables) {
            val value = maxOf(imported.getValue(table), previous[table] ?: 0L, floors(db).getValue(table))
            db.execSQL("UPDATE sqlite_sequence SET seq = ? WHERE name = ?", arrayOf<Any>(value, table))
            db.execSQL("INSERT INTO sqlite_sequence(name, seq) SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM sqlite_sequence WHERE name = ?)", arrayOf<Any>(table, value, table))
        }
    }
    fun validate(db: SupportSQLiteDatabase, header: BackupHeader, token: String): BackupSummary {
        fun count(sql: String) = db.query(sql).use { it.moveToFirst(); it.getLong(0) }
        require(count("SELECT COUNT(*) FROM pragma_foreign_key_check") == 0L) { "Relacionamentos inválidos" }
        require(count("SELECT COUNT(*) FROM plans WHERE is_active=1") <= 1)
        require(count("SELECT COUNT(*) FROM sessions WHERE status='DRAFT'") <= 1)
        require(count("SELECT COUNT(*) FROM sessions WHERE status NOT IN ('DRAFT','COMPLETED') OR (status='DRAFT' AND finished_at IS NOT NULL) OR (status='COMPLETED' AND (finished_at IS NULL OR finished_at<started_at OR rest_started_at IS NOT NULL OR rest_ends_at IS NOT NULL)) OR (rest_started_at IS NULL)!=(rest_ends_at IS NULL) OR rest_ends_at<rest_started_at OR (rest_ends_at-rest_started_at)>600000") == 0L)
        require(count("SELECT COUNT(*) FROM sessions s WHERE s.status='COMPLETED' AND NOT EXISTS (SELECT 1 FROM session_exercises e JOIN session_sets ss ON ss.session_exercise_id=e.id WHERE e.session_id=s.id AND ss.is_completed=1)") == 0L)
        require(count("SELECT COUNT(*) FROM session_sets ss JOIN session_exercises e ON ss.session_exercise_id=e.id JOIN sessions s ON e.session_id=s.id WHERE s.status='COMPLETED' AND ss.is_completed=0") == 0L)
        for ((table, parent) in listOf("workouts" to "plan_id", "workout_exercises" to "workout_id", "session_exercises" to "session_id", "session_sets" to "session_exercise_id")) {
            require(count("SELECT COUNT(*) FROM (SELECT $parent, position FROM $table GROUP BY $parent, position HAVING COUNT(*)>1)") == 0L) { "Posições duplicadas" }
        }
        db.execSQL("CREATE TEMP TABLE validation_names (name TEXT PRIMARY KEY NOT NULL)")
        val found = mutableSetOf<String>()
        db.query("SELECT * FROM exercises").use { c -> while(c.moveToNext()) {
            db.execSQL("INSERT INTO validation_names(name) VALUES (?)", arrayOf(ExerciseNames.comparisonKey(c.text("name"))))
            if(c.number("is_custom") == 0L) {
                val key=c.optionalText("native_key"); val native=NativeExercises.all.firstOrNull { it.key == key }
                require(native != null && found.add(native.key)) { "Catálogo nativo incompatível" }
                require(c.text("name")==native.name && c.optionalText("description")==native.description && c.text("muscle_groups")==Converters.encodeMuscleGroups(native.muscleGroups) && c.text("tracking_type")==TrackingType.WEIGHT_REPS.key)
            } else require(c.optionalText("native_key")==null)
        } }
        require(found == NativeExercises.all.map { it.key }.toSet())
        db.query("SELECT we.*, e.tracking_type FROM workout_exercises we JOIN exercises e ON e.id=we.exercise_id").use { c -> while(c.moveToNext()) target(c) }
        db.query("SELECT * FROM session_exercises").use { c -> while(c.moveToNext()) target(c) }
        db.query("SELECT ss.*, e.tracking_type FROM session_sets ss JOIN session_exercises e ON e.id=ss.session_exercise_id").use { c -> while(c.moveToNext()) {
            SetValues(c.optionalDouble("weight_kg"),c.optionalNumber("reps")?.toInt(),c.optionalNumber("duration_seconds")?.toInt()).validate(TrackingType.valueOf(c.text("tracking_type")))
        } }
        val references = mapOf("exercises" to "SELECT MAX(exercise_id) FROM session_exercises", "plans" to "SELECT MAX(source_plan_id) FROM sessions", "workouts" to "SELECT MAX(source_workout_id) FROM sessions")
        for(table in BackupFormat.tables) {
            val max=count("SELECT COALESCE(MAX(id),0) FROM $table")
            val weak=references[table]?.let { sql -> db.query(sql).use { it.moveToFirst(); if(it.isNull(0)) 0L else it.getLong(0) } } ?: 0L
            require(header.floors.getValue(table) >= maxOf(max,weak)) { "Piso de identidade inválido" }
        }
        if(header.mode==BackupMode.WITHOUT_HISTORY) require(count("SELECT COUNT(*) FROM sessions")==0L)
        return BackupSummary(token,header.mode,header.createdAt,count("SELECT COUNT(*) FROM exercises WHERE is_custom=1"),
            count("SELECT COUNT(*) FROM plans"),count("SELECT COUNT(*) FROM workouts"),count("SELECT COUNT(*) FROM sessions WHERE status='COMPLETED'"),
            count("SELECT COUNT(*) FROM session_sets"),count("SELECT COUNT(*) FROM sessions WHERE status='DRAFT'")>0)
    }
    private fun target(c: Cursor) {
        val type=TrackingType.valueOf(c.text("tracking_type")); val sets=c.number("target_sets").toInt(); val rest=Duration.ofSeconds(c.number("rest_seconds"))
        if(type==TrackingType.TIME) {
            require(c.optionalNumber("rep_min")==null && c.optionalNumber("rep_max")==null)
            ExerciseTarget.Timed(sets,requireNotNull(c.optionalNumber("target_duration_seconds")).toInt(),rest)
        } else {
            require(c.optionalNumber("target_duration_seconds")==null)
            ExerciseTarget.Repetitions(sets,requireNotNull(c.optionalNumber("rep_min")).toInt(),requireNotNull(c.optionalNumber("rep_max")).toInt(),rest)
        }
    }
    private fun Cursor.text(key:String)=getString(getColumnIndexOrThrow(key))
    private fun Cursor.number(key:String)=getLong(getColumnIndexOrThrow(key))
    private fun Cursor.optionalText(key:String)=getColumnIndexOrThrow(key).let { if(isNull(it)) null else getString(it) }
    private fun Cursor.optionalNumber(key:String)=getColumnIndexOrThrow(key).let { if(isNull(it)) null else getLong(it) }
    private fun Cursor.optionalDouble(key:String)=getColumnIndexOrThrow(key).let { if(isNull(it)) null else getDouble(it) }
}
