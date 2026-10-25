package com.loggym.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loggym.domain.model.ThemeMode
import com.loggym.domain.model.UserSettings
import com.loggym.domain.repository.UserPreferencesRepository
import com.loggym.ui.util.UiOperation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class SettingsUiState(val settings: UserSettings? = null, val failed: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(private val preferences: UserPreferencesRepository,
    private val persistence: com.loggym.domain.backup.PersistenceCoordinator = com.loggym.domain.backup.PersistenceCoordinator(),
    private val backup: com.loggym.domain.backup.BackupRepository? = null,
) : ViewModel() {
    val backupState = backup?.state ?: MutableStateFlow(com.loggym.domain.backup.BackupState())
    fun export(mode: com.loggym.domain.backup.BackupMode) { backup?.prepareExport(mode) }
    fun writeExport(token: String, uri: String?) { backup?.writeExport(token, uri) }
    fun importBackup(uri: String) { backup?.validateImport(uri) }
    fun confirmImport(token: String) { backup?.confirmImport(token) }
    fun cancelBackup() { backup?.cancel() }
    private val operations = UiOperation(viewModelScope, persistence)
    val busy = operations.busy
    val operationFailed = operations.failed
    fun dismissError() = operations.dismissError()
    private val refresh = MutableStateFlow(0)
    val uiState = refresh.flatMapLatest {
        preferences.settings.map { SettingsUiState(it) }.catch { emit(SettingsUiState(failed = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())
    fun retry() { refresh.update { it + 1 } }
    fun setTheme(mode: ThemeMode) = operations.launch { preferences.setThemeMode(mode) }
    fun setPrefill(enabled: Boolean) = operations.launch { preferences.setPrefillEnabled(enabled) }
    fun setAutoComplete(enabled: Boolean) = operations.launch { preferences.setAutoCompleteEnabled(enabled) }
}
