package com.loggym.domain.backup

import kotlinx.coroutines.flow.StateFlow

enum class BackupMode { FULL, WITHOUT_HISTORY }
enum class BackupStep { IDLE, PREPARING, DESTINATION, WRITING, VALIDATING, PREVIEW, RESTORING, SUCCESS, ERROR }
enum class BackupFailure { OPERATION, ROLLED_BACK, INCOMPATIBLE_VERSION }
data class BackupSummary(val token: String, val mode: BackupMode, val createdAt: Long,
    val customExercises: Long, val plans: Long, val workouts: Long, val sessions: Long,
    val sets: Long, val hasDraft: Boolean)
data class BackupState(val step: BackupStep = BackupStep.IDLE, val token: String? = null,
    val filename: String? = null, val summary: BackupSummary? = null, val error: BackupFailure? = null) {
    val busy get() = step in listOf(BackupStep.PREPARING, BackupStep.WRITING, BackupStep.VALIDATING, BackupStep.RESTORING)
}
interface BackupRepository {
    val state: StateFlow<BackupState>
    suspend fun recover()
    fun prepareExport(mode: BackupMode)
    fun writeExport(token: String, uri: String?)
    fun validateImport(uri: String)
    fun confirmImport(token: String)
    fun cancel()
}
