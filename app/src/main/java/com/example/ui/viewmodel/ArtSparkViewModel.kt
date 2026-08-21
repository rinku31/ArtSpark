package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiApiClient
import com.example.data.api.GeminiResult
import com.example.data.local.ArtSparkDatabase
import com.example.data.local.PromptRepository
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.generator.PromptGenerator
import com.example.model.ArtPrompt
import com.example.model.ArtSparkIdea
import com.example.model.BrainstormMessage
import com.example.model.BrainstormUiState
import com.example.model.CategorySelectionMode
import com.example.model.Difficulty
import com.example.model.MessageSender
import com.example.model.PromptCategory
import com.example.model.PromptLockState
import com.example.model.QuickAiAction
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
    private val geminiClient: GeminiApiClient = GeminiApiClient(application)

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

    // Brainstorming State
    private val _brainstormState = MutableStateFlow(BrainstormUiState())
    val brainstormState: StateFlow<BrainstormUiState> = _brainstormState.asStateFlow()

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

    // ==========================================
    // Brainstorm Session & AI Actions
    // ==========================================

    fun startNewBrainstorm(seedPrompt: ArtPrompt? = null) {
        if (seedPrompt != null) {
            val initialAiMsg = BrainstormMessage(
                sender = MessageSender.AI,
                text = "Let's work on your current idea:\n\n**${seedPrompt.narrativeText}**\n\nWhat would you like to change or explore?",
                quickPills = listOf("Expand details", "Simplify for sketch", "Twist setting", "Change style", "Make variations"),
                idea = ArtSparkIdea(
                    subject = seedPrompt.subject,
                    trait = seedPrompt.trait,
                    action = seedPrompt.action,
                    environment = seedPrompt.environment,
                    atmosphere = seedPrompt.atmosphere,
                    style = seedPrompt.style,
                    challenge = seedPrompt.challenge
                ),
                isContextSummary = true
            )
            _brainstormState.value = BrainstormUiState(
                messages = listOf(initialAiMsg),
                currentIdea = initialAiMsg.idea,
                activeSeedPrompt = seedPrompt
            )
        } else {
            _brainstormState.value = BrainstormUiState(
                messages = emptyList(),
                currentIdea = null,
                activeSeedPrompt = null
            )
        }
    }

    fun startBrainstormWithPrompt(prompt: ArtPrompt, onNavigateToBrainstorm: () -> Unit) {
        startNewBrainstorm(seedPrompt = prompt)
        onNavigateToBrainstorm()
    }

    fun sendBrainstormMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _brainstormState.value.isLoading) return

        val userMessage = BrainstormMessage(
            sender = MessageSender.USER,
            text = trimmed
        )

        val updatedMessages = _brainstormState.value.messages + userMessage
        _brainstormState.value = _brainstormState.value.copy(
            messages = updatedMessages,
            isLoading = true,
            errorMessage = null,
            isOffline = false,
            isApiKeyMissing = false
        )

        executeBrainstormRequest(updatedMessages)
    }

    fun sendQuickPill(pillText: String) {
        sendBrainstormMessage(pillText)
    }

    fun sendQuickAiAction(action: QuickAiAction) {
        val currentIdea = _brainstormState.value.currentIdea
        val userPrompt = if (currentIdea != null && currentIdea.isComplete) {
            "${action.title}: ${action.promptInstruction}"
        } else if (_brainstormState.value.activeSeedPrompt != null) {
            "${action.title}: ${action.promptInstruction}"
        } else {
            "${action.title}: Give me an inspiring concept with this focus."
        }
        sendBrainstormMessage(userPrompt)
    }

    fun retryLastBrainstormMessage() {
        val currentMessages = _brainstormState.value.messages.filter { !it.isError }
        if (currentMessages.isEmpty()) return

        _brainstormState.value = _brainstormState.value.copy(
            messages = currentMessages,
            isLoading = true,
            errorMessage = null,
            isOffline = false,
            isApiKeyMissing = false
        )

        executeBrainstormRequest(currentMessages)
    }

    private fun executeBrainstormRequest(messages: List<BrainstormMessage>) {
        viewModelScope.launch {
            val currentState = _brainstormState.value
            val result = geminiClient.brainstorm(
                messages = messages,
                currentIdea = currentState.currentIdea,
                seedPrompt = currentState.activeSeedPrompt
            )

            when (result) {
                is GeminiResult.Success -> {
                    val aiMessage = BrainstormMessage(
                        sender = MessageSender.AI,
                        text = result.replyText,
                        quickPills = result.quickPills,
                        idea = result.idea
                    )
                    _brainstormState.value = _brainstormState.value.copy(
                        messages = _brainstormState.value.messages + aiMessage,
                        isLoading = false,
                        currentIdea = result.idea ?: _brainstormState.value.currentIdea,
                        errorMessage = null,
                        isOffline = false,
                        isApiKeyMissing = false
                    )
                }
                is GeminiResult.Error -> {
                    _brainstormState.value = _brainstormState.value.copy(
                        isLoading = false,
                        errorMessage = result.message,
                        isOffline = result.isOffline,
                        isApiKeyMissing = result.isApiKeyMissing
                    )
                }
            }
        }
    }

    fun applyBrainstormIdeaToWorkspace(idea: ArtSparkIdea, onNavigateToDiscover: () -> Unit) {
        var newLockState = PromptLockState()
        val customCats = mutableSetOf<PromptCategory>()

        fun applyCategory(category: PromptCategory, rawValue: String) {
            val value = rawValue.trim()
            if (value.isNotBlank() && !value.equals("random", ignoreCase = true) && !value.equals("none", ignoreCase = true)) {
                newLockState = newLockState.setCustomValue(category, value)
                customCats.add(category)
            } else {
                newLockState = newLockState.setCategoryMode(category, CategorySelectionMode.RANDOM)
            }
        }

        applyCategory(PromptCategory.SUBJECT, idea.subject)
        applyCategory(PromptCategory.TRAIT, idea.trait)
        applyCategory(PromptCategory.ACTION, idea.action)
        applyCategory(PromptCategory.ENVIRONMENT, idea.environment)
        applyCategory(PromptCategory.ATMOSPHERE, idea.atmosphere)
        applyCategory(PromptCategory.STYLE, idea.style)
        applyCategory(PromptCategory.CHALLENGE, idea.challenge)

        _lockState.value = newLockState

        val updatedPrompt = ArtPrompt(
            id = System.currentTimeMillis(),
            trait = if (newLockState.isLocked(PromptCategory.TRAIT)) newLockState.getCustomValue(PromptCategory.TRAIT).ifBlank { newLockState.getSelectedValue(PromptCategory.TRAIT) } else "",
            subject = if (newLockState.isLocked(PromptCategory.SUBJECT)) newLockState.getCustomValue(PromptCategory.SUBJECT).ifBlank { newLockState.getSelectedValue(PromptCategory.SUBJECT) } else "Creature",
            action = if (newLockState.isLocked(PromptCategory.ACTION)) newLockState.getCustomValue(PromptCategory.ACTION).ifBlank { newLockState.getSelectedValue(PromptCategory.ACTION) } else "",
            environment = if (newLockState.isLocked(PromptCategory.ENVIRONMENT)) newLockState.getCustomValue(PromptCategory.ENVIRONMENT).ifBlank { newLockState.getSelectedValue(PromptCategory.ENVIRONMENT) } else "",
            atmosphere = if (newLockState.isLocked(PromptCategory.ATMOSPHERE)) newLockState.getCustomValue(PromptCategory.ATMOSPHERE).ifBlank { newLockState.getSelectedValue(PromptCategory.ATMOSPHERE) } else "",
            style = if (newLockState.isLocked(PromptCategory.STYLE)) newLockState.getCustomValue(PromptCategory.STYLE).ifBlank { newLockState.getSelectedValue(PromptCategory.STYLE) } else "",
            challenge = if (newLockState.isLocked(PromptCategory.CHALLENGE)) newLockState.getCustomValue(PromptCategory.CHALLENGE).ifBlank { newLockState.getSelectedValue(PromptCategory.CHALLENGE) } else "",
            isCreativeGap = false,
            difficulty = _selectedDifficulty.value,
            timestamp = System.currentTimeMillis(),
            customCategories = customCats
        )

        _currentPrompt.value = updatedPrompt
        _rerollTrigger.value += 1

        viewModelScope.launch {
            val savedId = repository.savePrompt(updatedPrompt)
            _currentPrompt.value = updatedPrompt.copy(id = savedId)
        }

        onNavigateToDiscover()
    }
}

