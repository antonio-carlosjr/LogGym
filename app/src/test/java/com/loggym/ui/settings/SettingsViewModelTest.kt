package com.loggym.ui.settings

import com.loggym.FakeUserPreferences
import com.loggym.MainDispatcherRule
import com.loggym.awaitFirst
import com.loggym.domain.model.ThemeMode
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*

class SettingsViewModelTest {
    @Test fun automaticPreferenceKeepsLastConfirmedValueOnFailureAndCanRetry() = runTest {
        var fail = true
        val preferences = object : FakeUserPreferences() {
            override suspend fun setAutoCompleteEnabled(enabled: Boolean) {
                if (fail) throw java.io.IOException()
                super.setAutoCompleteEnabled(enabled)
            }
        }
        val vm = SettingsViewModel(preferences)
        vm.setAutoComplete(true)
        vm.operationFailed.awaitFirst { it }; vm.busy.awaitFirst { !it }
        assertFalse(vm.uiState.awaitFirst { it.settings != null }.settings!!.autoCompleteEnabled)
        fail = false; vm.setAutoComplete(true)
        assertTrue(vm.uiState.awaitFirst { it.settings?.autoCompleteEnabled == true }.settings!!.autoCompleteEnabled)
    }
    @get:Rule val main = MainDispatcherRule()
    @Test fun failedWritePreservesConfirmedThemeAndAllowsRetry() = runTest {
        var fail = true
        val preferences = object : FakeUserPreferences() {
            override suspend fun setThemeMode(mode: ThemeMode) {
                if (fail) throw java.io.IOException()
                super.setThemeMode(mode)
            }
        }
        val vm = SettingsViewModel(preferences)
        vm.setTheme(ThemeMode.DARK)
        vm.operationFailed.awaitFirst { it }
        vm.busy.awaitFirst { !it }
        assertEquals(ThemeMode.SYSTEM, vm.uiState.awaitFirst { it.settings != null }.settings!!.themeMode)
        fail = false
        vm.setTheme(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, vm.uiState.awaitFirst { it.settings?.themeMode == ThemeMode.DARK }.settings!!.themeMode)
    }
    @Test fun themeModesResolveAgainstSystemAndUnknownKeysUseSystem() {
        assertTrue(ThemeMode.SYSTEM.isDark(true))
        assertFalse(ThemeMode.SYSTEM.isDark(false))
        assertTrue(ThemeMode.DARK.isDark(false))
        assertFalse(ThemeMode.LIGHT.isDark(true))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey("future-mode"))
    }
}
