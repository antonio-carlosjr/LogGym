package com.loggym.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.loggym.data.local.entity.SessionEntity
import com.loggym.data.local.entity.SessionExerciseEntity
import com.loggym.data.local.entity.SessionSetEntity
import com.loggym.data.local.relation.SessionWithExercises
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * As alterações de séries só atingem rascunhos: o histórico concluído é somente leitura (RF-128).
 */
@Dao
interface SessionDao {
    @Transaction
    @Query("SELECT * FROM sessions WHERE status = 'DRAFT' LIMIT 1")
    fun observeDraft(): Flow<SessionWithExercises?>

    @Query("SELECT id FROM sessions WHERE status = 'DRAFT' LIMIT 1")
    suspend fun getDraftId(): Long?

    @Query("SELECT * FROM sessions WHERE status = 'COMPLETED' ORDER BY started_at DESC")
    fun observeCompletedSessions(): Flow<List<SessionEntity>>

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun observeSession(sessionId: Long): Flow<SessionWithExercises?>

    @Query("SELECT COUNT(*) FROM sessions WHERE id = :sessionId AND status = 'DRAFT'")
    suspend fun countDraft(sessionId: Long): Int

    @Query(
        """
        SELECT COUNT(*) FROM session_exercises se
        INNER JOIN sessions s ON s.id = se.session_id
        WHERE se.id = :sessionExerciseId AND s.status = 'DRAFT'
        """,
    )
    suspend fun countDraftExercise(sessionExerciseId: Long): Int

    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Insert
    suspend fun insertSessionExercise(exercise: SessionExerciseEntity): Long

    @Insert
    suspend fun insertSet(set: SessionSetEntity): Long

    @Insert
    suspend fun insertSets(sets: List<SessionSetEntity>)

    /**
     * Séries executadas na sessão concluída mais recente que contém o exercício (RF-65 a RF-68).
     * Rascunhos são ignorados pelo filtro de status (RF-66, RF-67).
     */
    @Query(
        """
        SELECT * FROM session_sets
        WHERE session_exercise_id = (
            SELECT se.id FROM session_exercises se
            INNER JOIN sessions s ON s.id = se.session_id
            WHERE se.exercise_id = :exerciseId AND s.status = 'COMPLETED'
            ORDER BY s.finished_at DESC, se.position ASC
            LIMIT 1
        )
        AND is_completed = 1
        ORDER BY position ASC
        """,
    )
    suspend fun getLastCompletedSets(exerciseId: Long): List<SessionSetEntity>

    @Query(
        """
        SELECT se.* FROM session_exercises se
        INNER JOIN session_sets ss ON ss.session_exercise_id = se.id
        WHERE ss.id = :setId
        """,
    )
    suspend fun getExerciseOfSet(setId: Long): SessionExerciseEntity?

    @Query("SELECT * FROM session_sets WHERE session_exercise_id = :sessionExerciseId ORDER BY position DESC LIMIT 1")
    suspend fun getLastSet(sessionExerciseId: Long): SessionSetEntity?

    @Query(
        """
        UPDATE session_sets SET weight_kg = :weightKg, reps = :reps
        WHERE id = :setId AND session_exercise_id IN (
            SELECT se.id FROM session_exercises se
            INNER JOIN sessions s ON s.id = se.session_id
            WHERE s.status = 'DRAFT'
        )
        """,
    )
    suspend fun updateSetValues(setId: Long, weightKg: Double?, reps: Int?): Int

    @Query(
        """
        UPDATE session_sets SET is_completed = :completed
        WHERE id = :setId AND session_exercise_id IN (
            SELECT se.id FROM session_exercises se
            INNER JOIN sessions s ON s.id = se.session_id
            WHERE s.status = 'DRAFT'
        )
        """,
    )
    suspend fun updateSetCompleted(setId: Long, completed: Boolean): Int

    @Query(
        """
        DELETE FROM session_sets
        WHERE id = :setId AND is_completed = 0 AND session_exercise_id IN (
            SELECT se.id FROM session_exercises se
            INNER JOIN sessions s ON s.id = se.session_id
            WHERE s.status = 'DRAFT'
        )
        """,
    )
    suspend fun deleteUncompletedSet(setId: Long): Int

    @Query("UPDATE sessions SET rest_started_at = :startedAt, rest_ends_at = :endsAt WHERE id = :sessionId AND status = 'DRAFT'")
    suspend fun updateRest(sessionId: Long, startedAt: Instant?, endsAt: Instant?)

    @Query(
        """
        SELECT COUNT(*) FROM session_sets ss
        INNER JOIN session_exercises se ON se.id = ss.session_exercise_id
        WHERE se.session_id = :sessionId AND ss.is_completed = 1
        """,
    )
    suspend fun countCompletedSets(sessionId: Long): Int

    /** RF-81: séries não marcadas não entram no registro final. */
    @Query(
        """
        DELETE FROM session_sets
        WHERE is_completed = 0
        AND session_exercise_id IN (SELECT id FROM session_exercises WHERE session_id = :sessionId)
        """,
    )
    suspend fun deleteUncompletedSets(sessionId: Long)

    @Query(
        """
        DELETE FROM session_exercises
        WHERE session_id = :sessionId
        AND id NOT IN (SELECT session_exercise_id FROM session_sets)
        """,
    )
    suspend fun deleteExercisesWithoutSets(sessionId: Long)

    @Query(
        """
        UPDATE sessions
        SET status = 'COMPLETED', finished_at = :finishedAt, rest_started_at = NULL, rest_ends_at = NULL
        WHERE id = :sessionId AND status = 'DRAFT'
        """,
    )
    suspend fun markCompleted(sessionId: Long, finishedAt: Instant): Int

    @Query("DELETE FROM sessions WHERE id = :sessionId AND status = 'DRAFT'")
    suspend fun deleteDraft(sessionId: Long): Int
}
