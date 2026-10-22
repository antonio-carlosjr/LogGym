package com.loggym.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Preserves v1 installations, including drafts and completed sessions. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""ALTER TABLE exercises ADD COLUMN tracking_type TEXT NOT NULL DEFAULT 'WEIGHT_REPS'""")
        db.execSQL("""ALTER TABLE session_sets ADD COLUMN duration_seconds INTEGER""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `workout_exercises_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `workout_id` INTEGER NOT NULL, `exercise_id` INTEGER NOT NULL, `position` INTEGER NOT NULL, `target_sets` INTEGER NOT NULL, `rep_min` INTEGER, `rep_max` INTEGER, `rest_seconds` INTEGER NOT NULL, `target_duration_seconds` INTEGER, FOREIGN KEY(`workout_id`) REFERENCES `workouts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )""")
        db.execSQL("""INSERT INTO `workout_exercises_new` (`id`, `workout_id`, `exercise_id`, `position`, `target_sets`, `rep_min`, `rep_max`, `rest_seconds`) SELECT `id`, `workout_id`, `exercise_id`, `position`, `target_sets`, `rep_min`, `rep_max`, `rest_seconds` FROM `workout_exercises`""")
        db.execSQL("""DROP TABLE `workout_exercises`""")
        db.execSQL("""ALTER TABLE `workout_exercises_new` RENAME TO `workout_exercises`""")
        db.execSQL("""CREATE INDEX IF NOT EXISTS `index_workout_exercises_workout_id` ON `workout_exercises` (`workout_id`)""")
        db.execSQL("""CREATE INDEX IF NOT EXISTS `index_workout_exercises_exercise_id` ON `workout_exercises` (`exercise_id`)""")
        db.execSQL("""CREATE TEMP TABLE session_sets_backup AS SELECT * FROM session_sets""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `session_exercises_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `session_id` INTEGER NOT NULL, `exercise_id` INTEGER, `exercise_name` TEXT NOT NULL, `muscle_groups` TEXT NOT NULL, `position` INTEGER NOT NULL, `target_sets` INTEGER NOT NULL, `rep_min` INTEGER, `rep_max` INTEGER, `rest_seconds` INTEGER NOT NULL, `target_duration_seconds` INTEGER, `tracking_type` TEXT NOT NULL DEFAULT 'WEIGHT_REPS', FOREIGN KEY(`session_id`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )""")
        db.execSQL("""INSERT INTO `session_exercises_new` (`id`, `session_id`, `exercise_id`, `exercise_name`, `muscle_groups`, `position`, `target_sets`, `rep_min`, `rep_max`, `rest_seconds`) SELECT `id`, `session_id`, `exercise_id`, `exercise_name`, `muscle_groups`, `position`, `target_sets`, `rep_min`, `rep_max`, `rest_seconds` FROM `session_exercises`""")
        db.execSQL("""DROP TABLE `session_exercises`""")
        db.execSQL("""ALTER TABLE `session_exercises_new` RENAME TO `session_exercises`""")
        db.execSQL("""CREATE INDEX IF NOT EXISTS `index_session_exercises_session_id` ON `session_exercises` (`session_id`)""")
        db.execSQL("""CREATE INDEX IF NOT EXISTS `index_session_exercises_exercise_id` ON `session_exercises` (`exercise_id`)""")
        db.execSQL("""INSERT OR REPLACE INTO session_sets SELECT * FROM session_sets_backup""")
        db.execSQL("""DROP TABLE session_sets_backup""")
    }
}
