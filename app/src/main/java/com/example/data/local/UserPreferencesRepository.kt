package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.model.Difficulty
import com.example.model.PromptCategory
import com.example.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultDifficulty: Difficulty = Difficulty.MEDIUM,
    val enabledCategories: Set<PromptCategory> = PromptCategory.values().toSet(),
    val hapticsEnabled: Boolean = true
)

class UserPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("artspark_prefs", Context.MODE_PRIVATE)

    private val _preferences = MutableStateFlow(loadPreferences())
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    private fun loadPreferences(): UserPreferences {
        val themeStr = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val diffStr = prefs.getString("default_difficulty", Difficulty.MEDIUM.name) ?: Difficulty.MEDIUM.name
        val catsStr = prefs.getStringSet("enabled_categories", null)
        val haptics = prefs.getBoolean("haptics_enabled", true)

        val themeMode = try { ThemeMode.valueOf(themeStr) } catch (e: Exception) { ThemeMode.SYSTEM }
        val difficulty = try { Difficulty.valueOf(diffStr) } catch (e: Exception) { Difficulty.MEDIUM }
        val enabledCategories = if (catsStr != null) {
            catsStr.mapNotNull { name ->
                try { PromptCategory.valueOf(name) } catch (e: Exception) { null }
            }.toSet()
        } else {
            PromptCategory.values().toSet()
        }

        return UserPreferences(
            themeMode = themeMode,
            defaultDifficulty = difficulty,
            enabledCategories = enabledCategories,
            hapticsEnabled = haptics
        )
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _preferences.value = _preferences.value.copy(themeMode = mode)
    }

    fun setDefaultDifficulty(difficulty: Difficulty) {
        prefs.edit().putString("default_difficulty", difficulty.name).apply()
        _preferences.value = _preferences.value.copy(defaultDifficulty = difficulty)
    }

    fun toggleCategory(category: PromptCategory) {
        val current = _preferences.value.enabledCategories
        val updated = if (category in current) {
            // Keep at least SUBJECT enabled
            if (current.size > 1) current - category else current
        } else {
            current + category
        }
        val setStrings = updated.map { it.name }.toSet()
        prefs.edit().putStringSet("enabled_categories", setStrings).apply()
        _preferences.value = _preferences.value.copy(enabledCategories = updated)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("haptics_enabled", enabled).apply()
        _preferences.value = _preferences.value.copy(hapticsEnabled = enabled)
    }
}
