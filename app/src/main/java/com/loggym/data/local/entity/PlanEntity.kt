package com.loggym.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "plans",
    indices = [Index(value = ["is_active"])],
)
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "is_active") val isActive: Boolean = false,
    /** Template de origem, apenas informativo: a instância é independente (RF-38). */
    @ColumnInfo(name = "template_key") val templateKey: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)
