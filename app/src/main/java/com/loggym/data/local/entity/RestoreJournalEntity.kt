package com.loggym.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "restore_journal")
data class RestoreJournalEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "operation_id") val operationId: String,
    @ColumnInfo(name = "undo_sha256") val undoSha256: String,
    val phase: String,
    @ColumnInfo(name = "protocol_version") val protocolVersion: Int = 1,
)
