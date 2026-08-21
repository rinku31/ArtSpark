package com.example.model

import com.example.generator.PromptData
import com.example.generator.PromptSentenceBuilder

enum class PromptType(val displayName: String) {
    CLASSIC_SPARK("Classic Spark"),
    CREATIVE_GAP("Creative Gap")
}

/**
 * Unified sealed interface for all ArtSpark prompts.
 * ClassicSpark and CreativeGap are two distinct prompt entities with different data models and workflows.
 */
sealed interface DiscoverPrompt {
    val id: Long
    val difficulty: Difficulty
    val style: String
    val challenge: String
    val timestamp: Long
    val isFavorite: Boolean
    val isDailySpark: Boolean
    val promptType: PromptType

    val displayPromptText: String
    val narrativeText: String get() = displayPromptText
    val isCreativeGap: Boolean get() = (promptType == PromptType.CREATIVE_GAP)

    val trait: String get() = (this as? ClassicSpark)?.personalityTrait.orEmpty()
    val personality: String get() = (this as? ClassicSpark)?.personalityTrait.orEmpty()
    val subject: String get() = (this as? ClassicSpark)?.subjectCharacter.orEmpty()
    val action: String get() = (this as? ClassicSpark)?.actionSituationScene.orEmpty()
    val scene: String get() = (this as? ClassicSpark)?.actionSituationScene.orEmpty()
    val displayEnvironment: String get() = (this as? ClassicSpark)?.environment.orEmpty()
    val atmosphere: String get() = (this as? ClassicSpark)?.atmosphereWeather.orEmpty()
    val displayStoryHook: String get() = (this as? ClassicSpark)?.displayStoryHook.orEmpty()

    val gapTemplate: String? get() = (this as? CreativeGap)?.gapSentence
    val gapIdeaStarters: List<String> get() = (this as? CreativeGap)?.displayGapSuggestions ?: emptyList()
    val displayGapIdeaStarters: List<String> get() = (this as? CreativeGap)?.displayGapSuggestions ?: emptyList()
    val blankPosition: String get() = ""

    val subjectPhrase: String get() = (this as? ClassicSpark)?.subjectPhrase.orEmpty()
    val scenePhrase: String get() = (this as? ClassicSpark)?.scenePhrase.orEmpty()
    val atmospherePhrase: String get() = (this as? ClassicSpark)?.atmospherePhrase.orEmpty()
    val stylePhrase: String get() = when (this) {
        is ClassicSpark -> this.stylePhrase
        is CreativeGap -> this.stylePhrase
    }
    val challengePhrase: String get() = when (this) {
        is ClassicSpark -> this.challengePhrase
        is CreativeGap -> this.challengePhrase
    }

    fun toShareText(): String
    fun withDifficulty(difficulty: Difficulty): DiscoverPrompt
    fun copyWithFavorite(isFavorite: Boolean): DiscoverPrompt
    fun copyWithId(newId: Long): DiscoverPrompt
}

/**
 * Classic Spark prompt entity:
 * Contains full 7 structured inspiration categories + story hook + generated continuous narrative sentence.
 */
data class ClassicSpark(
    override val id: Long = 0,
    override val difficulty: Difficulty = Difficulty.MEDIUM,
    val personalityTrait: String = "",
    val subjectCharacter: String = "",
    val actionSituationScene: String = "",
    val environment: String = "",
    val atmosphereWeather: String = "",
    val artStyle: String = "",
    val creativeChallenge: String = "",
    val storyHook: String = "",
    val generatedSentence: String = "",
    override val timestamp: Long = System.currentTimeMillis(),
    override val isFavorite: Boolean = false,
    override val isDailySpark: Boolean = false,
    val customCategories: Set<PromptCategory> = emptySet()
) : DiscoverPrompt {
    override val promptType: PromptType get() = PromptType.CLASSIC_SPARK
    override val style: String get() = artStyle
    override val challenge: String get() = creativeChallenge

    override val subjectPhrase: String
        get() = PromptSentenceBuilder.buildSubjectPhrase(personalityTrait, subjectCharacter)

    override val scenePhrase: String
        get() = PromptSentenceBuilder.buildScenePhrase(actionSituationScene, environment)

    override val atmospherePhrase: String
        get() = PromptSentenceBuilder.buildAtmospherePhrase(atmosphereWeather)

    override val stylePhrase: String
        get() = PromptSentenceBuilder.buildStylePhrase(artStyle)

    override val challengePhrase: String
        get() = PromptSentenceBuilder.buildChallengePhrase(creativeChallenge)

    override val displayStoryHook: String
        get() = if (storyHook.isNotBlank()) storyHook else PromptSentenceBuilder.generateStoryHook(subjectCharacter, actionSituationScene, environment, id)

    override val displayPromptText: String
        get() = if (generatedSentence.isNotBlank()) generatedSentence else PromptSentenceBuilder.buildFullPromptSentence(
            personalityTrait, subjectCharacter, actionSituationScene, environment, atmosphereWeather, artStyle, creativeChallenge
        )

    fun getCategoryValue(category: PromptCategory): String = when (category) {
        PromptCategory.TRAIT -> personalityTrait
        PromptCategory.SUBJECT -> subjectCharacter
        PromptCategory.ACTION -> actionSituationScene
        PromptCategory.ENVIRONMENT -> environment
        PromptCategory.ATMOSPHERE -> atmosphereWeather
        PromptCategory.STYLE -> artStyle
        PromptCategory.CHALLENGE -> creativeChallenge
    }

    fun withCategory(category: PromptCategory, value: String, isCustom: Boolean = true): ClassicSpark {
        val newCustom = if (isCustom) customCategories + category else customCategories - category
        return when (category) {
            PromptCategory.TRAIT -> copy(personalityTrait = value, customCategories = newCustom)
            PromptCategory.SUBJECT -> copy(subjectCharacter = value, customCategories = newCustom)
            PromptCategory.ACTION -> copy(actionSituationScene = value, customCategories = newCustom)
            PromptCategory.ENVIRONMENT -> copy(environment = value, customCategories = newCustom)
            PromptCategory.ATMOSPHERE -> copy(atmosphereWeather = value, customCategories = newCustom)
            PromptCategory.STYLE -> copy(artStyle = value, customCategories = newCustom)
            PromptCategory.CHALLENGE -> copy(creativeChallenge = value, customCategories = newCustom)
        }.let {
            it.copy(generatedSentence = PromptSentenceBuilder.buildFullPromptSentence(
                it.personalityTrait, it.subjectCharacter, it.actionSituationScene, it.environment, it.atmosphereWeather, it.artStyle, it.creativeChallenge
            ))
        }
    }

    override fun withDifficulty(difficulty: Difficulty): ClassicSpark = copy(difficulty = difficulty)

    override fun copyWithFavorite(isFavorite: Boolean): ClassicSpark = copy(isFavorite = isFavorite)

    override fun copyWithId(newId: Long): ClassicSpark = copy(id = newId)

    override fun toShareText(): String {
        return buildString {
            if (isDailySpark) {
                appendLine("✨ DAILY SPARK INSPIRATION ✨")
            } else {
                appendLine("✨ ARTSPARK INSPIRATION BOARD ✨")
            }
            appendLine("----------------------------------------")
            if (personalityTrait.isNotBlank() || subjectCharacter.isNotBlank()) {
                appendLine("• SUBJECT: $subjectPhrase")
            }
            if (actionSituationScene.isNotBlank() || environment.isNotBlank()) {
                appendLine("• SCENE: $scenePhrase")
            }
            if (atmosphereWeather.isNotBlank()) {
                appendLine("• ATMOSPHERE: $atmospherePhrase")
            }
            if (artStyle.isNotBlank()) {
                appendLine("• STYLE: $stylePhrase")
            }
            if (creativeChallenge.isNotBlank()) {
                appendLine("• CHALLENGE: $challengePhrase")
            }
            appendLine("----------------------------------------")
            appendLine("Full Prompt:")
            appendLine("\"$displayPromptText\"")
            if (displayStoryHook.isNotBlank()) {
                appendLine("\nStory Hook: $displayStoryHook")
            }
            appendLine("\nGenerated with ArtSpark")
        }
    }

    fun toInspirationBoard(): InspirationBoard = InspirationBoard(
        personality = personalityTrait,
        subject = subjectCharacter,
        scene = actionSituationScene,
        environment = environment,
        atmosphere = atmosphereWeather,
        style = artStyle,
        challenge = creativeChallenge,
        isCreativeGap = false,
        gapSentence = "",
        blankPosition = "",
        suggestedFillIns = emptyList(),
        gapIdeaStarters = emptyList()
    )
}

/**
 * Creative Gap prompt entity:
 * Contains fill-in-the-blank gapSentence, curated gapSuggestions, style, and challenge.
 * There are NO subject, scene, environment, atmosphere categories.
 */
data class CreativeGap(
    override val id: Long = 0,
    override val difficulty: Difficulty = Difficulty.MEDIUM,
    val gapSentence: String = "",
    val gapSuggestions: List<String> = emptyList(),
    override val style: String = "",
    override val challenge: String = "",
    override val timestamp: Long = System.currentTimeMillis(),
    override val isFavorite: Boolean = false,
    override val isDailySpark: Boolean = false
) : DiscoverPrompt {
    override val promptType: PromptType get() = PromptType.CREATIVE_GAP

    val displayGapSuggestions: List<String>
        get() = if (gapSuggestions.isNotEmpty()) gapSuggestions else PromptData.gapInspirationIdeas.take(3)

    override val displayPromptText: String
        get() = gapSentence

    override val stylePhrase: String
        get() = PromptSentenceBuilder.buildStylePhrase(style)

    override val challengePhrase: String
        get() = PromptSentenceBuilder.buildChallengePhrase(challenge)

    override fun withDifficulty(difficulty: Difficulty): CreativeGap = copy(difficulty = difficulty)

    override fun copyWithFavorite(isFavorite: Boolean): CreativeGap = copy(isFavorite = isFavorite)

    override fun copyWithId(newId: Long): CreativeGap = copy(id = newId)

    override fun toShareText(): String {
        return buildString {
            appendLine("✨ ARTSPARK CREATIVE GAP ✨")
            appendLine("----------------------------------------")
            appendLine("\"$gapSentence\"")
            appendLine("----------------------------------------")
            appendLine("Idea Starters for the Blank:")
            displayGapSuggestions.take(3).forEach { starter ->
                appendLine("• $starter")
            }
            if (style.isNotBlank()) appendLine("\n• Suggested Style: $style")
            if (challenge.isNotBlank()) appendLine("• Creative Challenge: $challenge")
            appendLine("\nFill in the blank with your own twist!")
            appendLine("Generated with ArtSpark")
        }
    }
}

/**
 * Top-level extensions on DiscoverPrompt for UI convenience and backwards compatibility.
 */
fun DiscoverPrompt.getCategoryValue(category: PromptCategory): String = when (this) {
    is ClassicSpark -> this.getCategoryValue(category)
    is CreativeGap -> when (category) {
        PromptCategory.STYLE -> this.style
        PromptCategory.CHALLENGE -> this.challenge
        else -> ""
    }
}

val DiscoverPrompt.customCategories: Set<PromptCategory>
    get() = (this as? ClassicSpark)?.customCategories ?: emptySet()

