package com.jiqu.app.ui.theme

enum class ThemeMode(val preferenceValue: String, val displayName: String) {
    SYSTEM("system", "跟随系统"),
    LIGHT("light", "浅色模式"),
    DARK("dark", "深色模式");

    companion object {
        fun fromPreference(value: String?): ThemeMode =
            entries.firstOrNull { it.preferenceValue == value } ?: SYSTEM
    }
}
