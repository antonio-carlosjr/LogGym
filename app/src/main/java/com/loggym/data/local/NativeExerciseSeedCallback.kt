package com.loggym.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.loggym.data.seed.NativeExercises

/**
 * RF-01/RF-02: `onCreate` só executa quando o arquivo do banco é criado,
 * então a carga dos exercícios nativos acontece uma única vez.
 * O `INSERT OR IGNORE` sobre o índice único de `native_key` é uma proteção extra contra duplicatas.
 */
class NativeExerciseSeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        NativeExercises.all.forEach { exercise ->
            db.execSQL(
                "INSERT OR IGNORE INTO exercises (name, muscle_group, is_custom, native_key) VALUES (?, ?, 0, ?)",
                arrayOf<Any?>(exercise.name, exercise.muscleGroup, exercise.key),
            )
        }
    }
}
