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
import com.example.model.BrainstormIdea
import com.example.model.BrainstormMessage
import com.example.model.BrainstormUiState
import com.example.model.CategorySelectionMode
import com.example.model.ClassicSpark
import com.example.model.ClassicSparkIdea
import com.example.model.CreativeGap
import com.example.model.CreativeGapIdea
import com.example.model.Difficulty
import com.example.model.DiscoverPrompt
import com.example.model.MessageSender
import com.example.model.PromptCategory
import com.example.model.PromptLockState
import com.example.model.PromptType
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
    val dailySpark: StateFlow<ClassicSpark> = _dailySpark.asStateFlow()

    private val _currentPrompt = MutableStateFlow<DiscoverPrompt>(
        PromptGenerator.generate(
            difficulty = Difficulty.MEDIUM,
            lockState = PromptLockState(),
            isCreativeGap = false
        )
    )
    val currentPrompt: StateFlow<DiscoverPrompt> = _currentPrompt.asStateFlow()

    private val _isAdvancedOpen = MutableStateFlow(false)
    val isAdvancedOpen: StateFlow<Boolean> = _isAdvancedOpen.asStateFlow()

    private val _rerollTrigger = MutableStateFlow(0)
    val rerollTrigger: StateFlow<Int> = _rerollTrigger.asStateFlow()

    // Brainstorming State
    private val _brainstormState = MutableStateFlow(BrainstormUiState())
    val brainstormState: StateFlow<BrainstormUiState> = _brainstormState.asStateFlow()

    val history: StateFlow<List<DiscoverPrompt>>
    val favorites: StateFlow<List<DiscoverPrompt>>

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
            _currentPrompt.value = _currentPrompt.value.copyWithId(id)
        }
    }

    fun reroll() {
        val nextPrompt = if (_isCreativeGapMode.value) {
            PromptGenerator.generateCreativeGap(
                difficulty = _selectedDifficulty.value
            )
        } else {
            PromptGenerator.generateClassicSpark(
                difficulty = _selectedDifficulty.value,
                lockState = _lockState.value,
                enabledCategories = preferences.value.enabledCategories
            )
        }
        _currentPrompt.value = nextPrompt
        _rerollTrigger.value += 1

        viewModelScope.launch {
            val id = repository.savePrompt(nextPrompt)
            _currentPrompt.value = nextPrompt.copyWithId(id)
        }
    }

    fun toggleCategoryLock(category: PromptCategory) {
        val prompt = _currentPrompt.value
        val currentValue = when (prompt) {
            is ClassicSpark -> prompt.getCategoryValue(category)
            is CreativeGap -> if (category == PromptCategory.STYLE) prompt.style else if (category == PromptCategory.CHALLENGE) prompt.challenge else ""
        }
        _lockState.value = _lockState.value.toggleLock(category, currentValue)
    }

    fun setCategoryMode(category: PromptCategory, mode: CategorySelectionMode) {
        val prompt = _currentPrompt.value
        val currentValue = when (prompt) {
            is ClassicSpark -> prompt.getCategoryValue(category)
            is CreativeGap -> if (category == PromptCategory.STYLE) prompt.style else if (category == PromptCategory.CHALLENGE) prompt.challenge else ""
        }
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
        if (curr is ClassicSpark) {
            val updated = curr.withCategory(category, value, isCustom)
            _currentPrompt.value = updated
            viewModelScope.launch {
                repository.savePrompt(updated)
            }
        }
    }

    fun unlockAll() {
        _lockState.value = _lockState.value.clearAll()
    }

    fun setDifficulty(difficulty: Difficulty) {
        _selectedDifficulty.value = difficulty
        val updated = _currentPrompt.value.withDifficulty(difficulty)
        _currentPrompt.value = updated
        viewModelScope.launch {
            repository.savePrompt(updated)
        }
    }

    fun toggleCreativeGapMode() {
        setCreativeGapMode(!_isCreativeGapMode.value)
    }

    fun setCreativeGapMode(active: Boolean) {
        if (_isCreativeGapMode.value == active) return
        _isCreativeGapMode.value = active
        val current = _currentPrompt.value
        if (active) {
            val gapPrompt = if (current is CreativeGap) current else {
                PromptGenerator.generateCreativeGap(
                    difficulty = _selectedDifficulty.value
                ).copy(
                    style = current.style,
                    challenge = current.challenge
                )
            }
            _currentPrompt.value = gapPrompt
            _rerollTrigger.value += 1
            viewModelScope.launch {
                val id = repository.savePrompt(gapPrompt)
                _currentPrompt.value = gapPrompt.copyWithId(id)
            }
        } else {
            val classicPrompt = if (current is ClassicSpark) current else {
                PromptGenerator.generateClassicSpark(
                    difficulty = _selectedDifficulty.value,
                    lockState = _lockState.value
                ).copy(
                    artStyle = current.style,
                    creativeChallenge = current.challenge
                )
            }
            _currentPrompt.value = classicPrompt
            _rerollTrigger.value += 1
            viewModelScope.launch {
                val id = repository.savePrompt(classicPrompt)
                _currentPrompt.value = classicPrompt.copyWithId(id)
            }
        }
    }

    fun toggleCurrentFavorite() {
        val prompt = _currentPrompt.value
        viewModelScope.launch {
            val newFavStatus = !prompt.isFavorite
            _currentPrompt.value = prompt.copyWithFavorite(newFavStatus)
            repository.toggleFavorite(prompt)
        }
    }

    fun toggleFavorite(prompt: DiscoverPrompt) {
        viewModelScope.launch {
            repository.toggleFavorite(prompt)
            if (_currentPrompt.value.id == prompt.id) {
                _currentPrompt.value = _currentPrompt.value.copyWithFavorite(!prompt.isFavorite)
            }
        }
    }

    fun loadAndRerollSimilar(prompt: DiscoverPrompt, onNavigateToDiscover: () -> Unit) {
        _selectedDifficulty.value = prompt.difficulty
        _isCreativeGapMode.value = (prompt.promptType == PromptType.CREATIVE_GAP)
        when (prompt) {
            is ClassicSpark -> {
                _lockState.value = PromptLockState(
                    lockedCategories = setOf(PromptCategory.SUBJECT, PromptCategory.STYLE),
                    lockedValues = mapOf(
                        PromptCategory.SUBJECT to prompt.subjectCharacter,
                        PromptCategory.STYLE to prompt.artStyle
                    )
                )
            }
            is CreativeGap -> {
                _lockState.value = PromptLockState()
            }
        }
        reroll()
        onNavigateToDiscover()
    }

    fun loadPromptToWorkspace(prompt: DiscoverPrompt, onNavigateToDiscover: () -> Unit) {
        _selectedDifficulty.value = prompt.difficulty
        _isCreativeGapMode.value = (prompt.promptType == PromptType.CREATIVE_GAP)
        _currentPrompt.value = prompt
        _rerollTrigger.value += 1
        onNavigateToDiscover()
    }

    fun deletePrompt(prompt: DiscoverPrompt) {
        viewModelScope.launch {
            repository.deletePrompt(prompt)
            if (_currentPrompt.value.id == prompt.id) {
                _currentPrompt.value = _currentPrompt.value.copyWithFavorite(false)
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

    fun startNewBrainstorm(seedPrompt: DiscoverPrompt? = null, promptType: PromptType? = null) {
        val targetType = promptType ?: seedPrompt?.promptType ?: if (_isCreativeGapMode.value) PromptType.CREATIVE_GAP else PromptType.CLASSIC_SPARK

        if (seedPrompt != null) {
            when (seedPrompt) {
                is ClassicSpark -> {
                    val initialIdea = ClassicSparkIdea(
                        difficulty = seedPrompt.difficulty,
                        personalityTrait = seedPrompt.personalityTrait,
                        subjectCharacter = seedPrompt.subjectCharacter,
                        actionSituationScene = seedPrompt.actionSituationScene,
                        environment = seedPrompt.environment,
                        atmosphereWeather = seedPrompt.atmosphereWeather,
                        artStyle = seedPrompt.artStyle,
                        creativeChallenge = seedPrompt.creativeChallenge,
                        storyHook = seedPrompt.storyHook,
                        generatedSentence = seedPrompt.displayPromptText
                    )
                    val promptText = "Let's explore your Classic Spark prompt:\n\n**${seedPrompt.displayPromptText}**\n\nWhat category would you like to refine, twist, or explore?"
                    val pills = listOf("Make atmosphere darker", "Change action / scene", "Try another art style", "Increase difficulty", "Make variations")
                    val initialAiMsg = BrainstormMessage(
                        sender = MessageSender.AI,
                        text = promptText,
                        quickPills = pills,
                        idea = initialIdea,
                        isContextSummary = true
                    )
                    _brainstormState.value = BrainstormUiState(
                        messages = listOf(initialAiMsg),
                        activePromptType = PromptType.CLASSIC_SPARK,
                        currentIdea = initialIdea,
                        activeSeedPrompt = seedPrompt
                    )
                }
                is CreativeGap -> {
                    val initialIdea = CreativeGapIdea(
                        difficulty = seedPrompt.difficulty,
                        gapSentence = seedPrompt.gapSentence,
                        gapSuggestions = seedPrompt.displayGapSuggestions,
                        style = seedPrompt.style,
                        challenge = seedPrompt.challenge
                    )
                    val promptText = "Let's explore your Creative Gap prompt:\n\n**${seedPrompt.gapSentence}**\n\nWhat would you like to fill into the blank, or how shall we develop this fill-in-the-blank prompt?"
                    val pills = listOf("Suggest twists for the blank", "Make it harder", "Suggest art style", "Surprise me with variations")
                    val initialAiMsg = BrainstormMessage(
                        sender = MessageSender.AI,
                        text = promptText,
                        quickPills = pills,
                        idea = initialIdea,
                        isContextSummary = true
                    )
                    _brainstormState.value = BrainstormUiState(
                        messages = listOf(initialAiMsg),
                        activePromptType = PromptType.CREATIVE_GAP,
                        currentIdea = initialIdea,
                        activeSeedPrompt = seedPrompt
                    )
                }
            }
        } else {
            _brainstormState.value = BrainstormUiState(
                messages = emptyList(),
                activePromptType = targetType,
                currentIdea = null,
                activeSeedPrompt = null
            )
        }
    }

    fun startBrainstormWithPrompt(prompt: DiscoverPrompt, onNavigateToBrainstorm: () -> Unit) {
        startNewBrainstorm(seedPrompt = prompt, promptType = prompt.promptType)
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
        val currentType = _brainstormState.value.activePromptType
        val instruction = action.getInstruction(currentType)
        sendBrainstormMessage("${action.title}: $instruction")
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
                seedPrompt = currentState.activeSeedPrompt,
                promptType = currentState.activePromptType
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

    fun applyBrainstormIdeaToWorkspace(idea: BrainstormIdea, onNavigateToDiscover: () -> Unit) {
        // Propagate updated difficulty to Discover
        _selectedDifficulty.value = idea.difficulty

        when (idea) {
            is ClassicSparkIdea -> {
                _isCreativeGapMode.value = false

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

                applyCategory(PromptCategory.TRAIT, idea.personalityTrait)
                applyCategory(PromptCategory.SUBJECT, idea.subjectCharacter)
                applyCategory(PromptCategory.ACTION, idea.actionSituationScene)
                applyCategory(PromptCategory.ENVIRONMENT, idea.environment)
                applyCategory(PromptCategory.ATMOSPHERE, idea.atmosphereWeather)
                applyCategory(PromptCategory.STYLE, idea.artStyle)
                applyCategory(PromptCategory.CHALLENGE, idea.creativeChallenge)

                _lockState.value = newLockState

                val updatedPrompt = idea.toDiscoverPrompt(
                    id = System.currentTimeMillis()
                ).let { spark ->
                    spark.copy(customCategories = customCats)
                }

                _currentPrompt.value = updatedPrompt
                _rerollTrigger.value += 1

                viewModelScope.launch {
                    val savedId = repository.savePrompt(updatedPrompt)
                    _currentPrompt.value = updatedPrompt.copyWithId(savedId)
                }
            }
            is CreativeGapIdea -> {
                _isCreativeGapMode.value = true

                val updatedPrompt = idea.toDiscoverPrompt(
                    id = System.currentTimeMillis()
                )

                _currentPrompt.value = updatedPrompt
                _rerollTrigger.value += 1

                viewModelScope.launch {
                    val savedId = repository.savePrompt(updatedPrompt)
                    _currentPrompt.value = updatedPrompt.copyWithId(savedId)
                }
            }
        }

        onNavigateToDiscover()
    }
}
