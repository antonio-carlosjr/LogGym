package com.loggym.domain.model

enum class ThemeMode(val key: String) {
    SYSTEM("system"), LIGHT("light"), DARK("dark");
    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }
    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

data class UserSettings(val themeMode: ThemeMode = ThemeMode.SYSTEM, val prefillEnabled: Boolean = true,
    val autoCompleteEnabled: Boolean = false)
