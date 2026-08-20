package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ArtSparkDatabase
import com.example.data.local.PromptRepository
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.generator.PromptGenerator
import com.example.model.ArtPrompt
import com.example.model.CategorySelectionMode
import com.example.model.Difficulty
import com.example.model.PromptCategory
import com.example.model.PromptLockState
import com.example.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArtSparkViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PromptRepository
    private val prefsRepository: UserPreferencesRepository = UserPreferencesRepository(application)

    val preferences: StateFlow<UserPreferences> = prefsRepository.preferences

    private val _lockState = MutableStateFlow(PromptLockState())
    val lockState: StateFlow<PromptLockState> = _lockState.asStateFlow()

    private val _isCreativeGapMode = MutableStateFlow(false)
    val isCreativeGapMode: StateFlow<Boolean> = _isCreativeGapMode.asStateFlow()

    private val _selectedDifficulty = MutableStateFlow(Difficulty.MEDIUM)
    val selectedDifficulty: StateFlow<Difficulty> = _selectedDifficulty.asStateFlow()

    private val _dailySpark = MutableStateFlow(PromptGenerator.generateDailySpark())
    val dailySpark: StateFlow<ArtPrompt> = _dailySpark.asStateFlow()

    private val _currentPrompt = MutableStateFlow(
        PromptGenerator.generate(
            difficulty = Difficulty.MEDIUM,
            lockState = PromptLockState(),
            isCreativeGap = false
        )
    )
    val currentPrompt: StateFlow<ArtPrompt> = _currentPrompt.asStateFlow()

    private val _isAdvancedOpen = MutableStateFlow(false)
    val isAdvancedOpen: StateFlow<Boolean> = _isAdvancedOpen.asStateFlow()

    private val _rerollTrigger = MutableStateFlow(0)
    val rerollTrigger: StateFlow<Int> = _rerollTrigger.asStateFlow()

    val history: StateFlow<List<ArtPrompt>>
    val favorites: StateFlow<List<ArtPrompt>>

    init {
        val database = ArtSparkDatabase.getDatabase(application)
        repository = PromptRepository(database.promptDao())

        history = repository.history.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        favorites = repository.favorites.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Save initial prompt to history
        viewModelScope.launch {
            val id = repository.savePrompt(_currentPrompt.value)
            _currentPrompt.value = _currentPrompt.value.copy(id = id)
        }
    }

    fun reroll() {
        val nextPrompt = PromptGenerator.generate(
            difficulty = _selectedDifficulty.value,
            lockState = _lockState.value,
            isCreativeGap = _isCreativeGapMode.value,
            enabledCategories = preferences.value.enabledCategories
        )
        _currentPrompt.value = nextPrompt
        _rerollTrigger.value += 1

        viewModelScope.launch {
            val id = repository.savePrompt(nextPrompt)
            _currentPrompt.value = nextPrompt.copy(id = id)
        }
    }

    fun toggleCategoryLock(category: PromptCategory) {
        val prompt = _currentPrompt.value
        val currentValue = prompt.getCategoryValue(category)
        _lockState.value = _lockState.value.toggleLock(category, currentValue)
    }

    fun setCategoryMode(category: PromptCategory, mode: CategorySelectionMode) {
        val currentValue = _currentPrompt.value.getCategoryValue(category)
        _lockState.value = _lockState.value.setCategoryMode(category, mode, fallbackValue = currentValue)
    }

    fun setCategorySelectedValue(category: PromptCategory, value: String) {
        _lockState.value = _lockState.value.setSelectedValue(category, value)
        applyDirectCategoryChange(category, value, isCustom = false)
    }

    fun setCategoryCustomValue(category: PromptCategory, value: String) {
        _lockState.value = _lockState.value.setCustomValue(category, value)
        if (value.isNotBlank()) {
            applyDirectCategoryChange(category, value, isCustom = true)
        }
    }

    private fun applyDirectCategoryChange(category: PromptCategory, value: String, isCustom: Boolean) {
        val curr = _currentPrompt.value
        val newCustomCats = if (isCustom) curr.customCategories + category else curr.customCategories - category
        val updated = when (category) {
            PromptCategory.TRAIT -> curr.copy(trait = value, customCategories = newCustomCats)
            PromptCategory.SUBJECT -> curr.copy(subject = value, customCategories = newCustomCats)
            PromptCategory.ACTION -> curr.copy(action = value, customCategories = newCustomCats)
            PromptCategory.ENVIRONMENT -> curr.copy(environment = value, customCategories = newCustomCats)
            PromptCategory.ATMOSPHERE -> curr.copy(atmosphere = value, customCategories = newCustomCats)
            PromptCategory.STYLE -> curr.copy(style = value, customCategories = newCustomCats)
            PromptCategory.CHALLENGE -> curr.copy(challenge = value, customCategories = newCustomCats)
        }
        _currentPrompt.value = updated
        viewModelScope.launch {
            repository.savePrompt(updated)
        }
    }

    fun unlockAll() {
        _lockState.value = _lockState.value.clearAll()
    }

    fun setDifficulty(difficulty: Difficulty) {
        _selectedDifficulty.value = difficulty
    }

    fun toggleCreativeGapMode() {
        val newMode = !_isCreativeGapMode.value
        _isCreativeGapMode.value = newMode
        reroll()
    }

    fun setCreativeGapMode(active: Boolean) {
        if (_isCreativeGapMode.value != active) {
            _isCreativeGapMode.value = active
            reroll()
        }
    }

    fun toggleCurrentFavorite() {
        val prompt = _currentPrompt.value
        viewModelScope.launch {
            val newFavStatus = !prompt.isFavorite
            _currentPrompt.value = prompt.copy(isFavorite = newFavStatus)
            repository.toggleFavorite(prompt)
        }
    }

    fun toggleFavorite(prompt: ArtPrompt) {
        viewModelScope.launch {
            repository.toggleFavorite(prompt)
            if (_currentPrompt.value.id == prompt.id) {
                _currentPrompt.value = _currentPrompt.value.copy(isFavorite = !prompt.isFavorite)
            }
        }
    }

    fun loadAndRerollSimilar(prompt: ArtPrompt, onNavigateToDiscover: () -> Unit) {
        _selectedDifficulty.value = prompt.difficulty
        _isCreativeGapMode.value = prompt.isCreativeGap
        _lockState.value = PromptLockState(
            lockedCategories = setOf(PromptCategory.SUBJECT, PromptCategory.STYLE),
            lockedValues = mapOf(
                PromptCategory.SUBJECT to prompt.subject,
                PromptCategory.STYLE to prompt.style
            )
        )
        reroll()
        onNavigateToDiscover()
    }

    fun loadPromptToWorkspace(prompt: ArtPrompt, onNavigateToDiscover: () -> Unit) {
        _selectedDifficulty.value = prompt.difficulty
        _isCreativeGapMode.value = prompt.isCreativeGap
        _currentPrompt.value = prompt
        _rerollTrigger.value += 1
        onNavigateToDiscover()
    }

    fun deletePrompt(prompt: ArtPrompt) {
        viewModelScope.launch {
            repository.deletePrompt(prompt)
            if (_currentPrompt.value.id == prompt.id) {
                _currentPrompt.value = _currentPrompt.value.copy(isFavorite = false)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAll()
            reroll()
        }
    }

    fun setAdvancedOpen(open: Boolean) {
        _isAdvancedOpen.value = open
    }

    fun toggleAdvanced() {
        _isAdvancedOpen.value = !_isAdvancedOpen.value
    }

    fun setThemeMode(mode: ThemeMode) {
        prefsRepository.setThemeMode(mode)
    }

    fun setDefaultDifficulty(difficulty: Difficulty) {
        prefsRepository.setDefaultDifficulty(difficulty)
        _selectedDifficulty.value = difficulty
    }

    fun toggleCategoryEnabled(category: PromptCategory) {
        prefsRepository.toggleCategory(category)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        prefsRepository.setHapticsEnabled(enabled)
    }
}
