package com.loggym.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.loggym.domain.model.SessionStatus
import java.time.Instant

/**
 * Rascunho e sessão concluída vivem na mesma tabela, diferenciados por [status].
 * Assim, finalizar é uma única atualização dentro de uma transação (RF-115).
 *
 * Não há chave estrangeira para planos/divisões: alterar ou excluir o plano
 * não pode afetar sessões (RF-62, RF-127). Os nomes são snapshots.
 */
@Entity(
    tableName = "sessions",
    indices = [Index(value = ["status"]), Index(value = ["finished_at"])],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val status: SessionStatus,
    @ColumnInfo(name = "source_plan_id") val sourcePlanId: Long?,
    @ColumnInfo(name = "plan_name") val planName: String,
    @ColumnInfo(name = "source_workout_id") val sourceWorkoutId: Long?,
    @ColumnInfo(name = "workout_name") val workoutName: String,
    @ColumnInfo(name = "started_at") val startedAt: Instant,
    @ColumnInfo(name = "finished_at") val finishedAt: Instant? = null,
    @ColumnInfo(name = "rest_started_at") val restStartedAt: Instant? = null,
    @ColumnInfo(name = "rest_ends_at") val restEndsAt: Instant? = null,
)
