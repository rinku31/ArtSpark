package com.example.model

import java.util.UUID

enum class MessageSender {
    USER,
    AI
}

/**
 * Sealed interface for Brainstorm structured concepts.
 * Refined representations strictly maintain the prompt type of the source prompt.
 */
sealed interface BrainstormIdea {
    val promptType: PromptType
    val difficulty: Difficulty
    val style: String
    val challenge: String
    val isComplete: Boolean get() = when (this) {
        is ClassicSparkIdea -> personalityTrait.isNotBlank() || subjectCharacter.isNotBlank() || actionSituationScene.isNotBlank() || environment.isNotBlank() || artStyle.isNotBlank()
        is CreativeGapIdea -> gapSentence.isNotBlank()
    }

    fun toDiscoverPrompt(
        id: Long = System.currentTimeMillis(),
        isFavorite: Boolean = false,
        isDailySpark: Boolean = false
    ): DiscoverPrompt
}

/**
 * Structured brainstorm idea for Classic Spark (7 categories).
 */
data class ClassicSparkIdea(
    override val difficulty: Difficulty = Difficulty.MEDIUM,
    val personalityTrait: String = "",
    val subjectCharacter: String = "",
    val actionSituationScene: String = "",
    val environment: String = "",
    val atmosphereWeather: String = "",
    val artStyle: String = "",
    val creativeChallenge: String = "",
    val storyHook: String = "",
    val generatedSentence: String = ""
) : BrainstormIdea {
    override val promptType: PromptType get() = PromptType.CLASSIC_SPARK
    override val style: String get() = artStyle
    override val challenge: String get() = creativeChallenge

    val personality: String get() = personalityTrait
    val subject: String get() = subjectCharacter
    val scene: String get() = actionSituationScene
    val atmosphere: String get() = atmosphereWeather

    override fun toDiscoverPrompt(
        id: Long,
        isFavorite: Boolean,
        isDailySpark: Boolean
    ): ClassicSpark {
        val sentence = if (generatedSentence.isNotBlank()) generatedSentence else {
            com.example.generator.PromptSentenceBuilder.buildFullPromptSentence(
                personalityTrait, subjectCharacter, actionSituationScene, environment, atmosphereWeather, artStyle, creativeChallenge
            )
        }
        return ClassicSpark(
            id = id,
            difficulty = difficulty,
            personalityTrait = personalityTrait,
            subjectCharacter = subjectCharacter,
            actionSituationScene = actionSituationScene,
            environment = environment,
            atmosphereWeather = atmosphereWeather,
            artStyle = artStyle,
            creativeChallenge = creativeChallenge,
            storyHook = storyHook,
            generatedSentence = sentence,
            timestamp = System.currentTimeMillis(),
            isFavorite = isFavorite,
            isDailySpark = isDailySpark
        )
    }
}

/**
 * Structured brainstorm idea for Creative Gap.
 * Exclusively contains gapSentence, suggested fill-ins, style, and challenge.
 */
data class CreativeGapIdea(
    override val difficulty: Difficulty = Difficulty.MEDIUM,
    val gapSentence: String = "",
    val gapSuggestions: List<String> = emptyList(),
    override val style: String = "",
    override val challenge: String = ""
) : BrainstormIdea {
    override val promptType: PromptType get() = PromptType.CREATIVE_GAP

    override fun toDiscoverPrompt(
        id: Long,
        isFavorite: Boolean,
        isDailySpark: Boolean
    ): CreativeGap {
        return CreativeGap(
            id = id,
            difficulty = difficulty,
            gapSentence = gapSentence,
            gapSuggestions = gapSuggestions,
            style = style,
            challenge = challenge,
            timestamp = System.currentTimeMillis(),
            isFavorite = isFavorite,
            isDailySpark = isDailySpark
        )
    }
}

/**
 * Backward compatibility alias and factory for ArtSparkIdea.
 */
typealias ArtSparkIdea = BrainstormIdea

fun ArtSparkIdea(
    personality: String = "",
    subject: String = "",
    scene: String = "",
    environment: String = "",
    atmosphere: String = "",
    style: String = "",
    challenge: String = "",
    difficulty: Difficulty = Difficulty.MEDIUM,
    gapSentence: String = "",
    gapSuggestions: List<String> = emptyList(),
    isCreativeGap: Boolean = false
): BrainstormIdea {
    return if (isCreativeGap) {
        CreativeGapIdea(
            difficulty = difficulty,
            gapSentence = gapSentence,
            gapSuggestions = gapSuggestions,
            style = style,
            challenge = challenge
        )
    } else {
        ClassicSparkIdea(
            difficulty = difficulty,
            personalityTrait = personality,
            subjectCharacter = subject,
            actionSituationScene = scene,
            environment = environment,
            atmosphereWeather = atmosphere,
            artStyle = style,
            creativeChallenge = challenge
        )
    }
}

data class BrainstormMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val quickPills: List<String> = emptyList(),
    val idea: BrainstormIdea? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val isContextSummary: Boolean = false
)

enum class QuickAiAction(
    val title: String,
    val iconEmoji: String,
    val promptInstructionClassic: String,
    val promptInstructionGap: String
) {
    EXPAND(
        "Expand",
        "🔍",
        "Expand on this Classic Spark with richer visual textures, scene actions, and detailed atmosphere while keeping all 7 categories.",
        "Expand on this Creative Gap prompt by suggesting vivid fill-in twists and visual details for the blank."
    ),
    SIMPLIFY(
        "Simplify",
        "✏️",
        "Simplify this Classic Spark into clear, bold shapes and an easy action for a quick sketch while keeping all 7 categories.",
        "Simplify this Creative Gap sentence into an accessible, clear fill-in-the-blank prompt."
    ),
    TWIST(
        "Twist",
        "🌀",
        "Introduce an unexpected, clever, or surreal twist to this Classic Spark while keeping all 7 categories.",
        "Introduce an unexpected, clever twist for the gap sentence and its fill-in ideas."
    ),
    VARIATIONS(
        "Variations",
        "🎲",
        "Give me 3 creative variations or alternate directions for this Classic Spark.",
        "Give me 3 alternate gap sentences and fill-in idea starters."
    ),
    INCREASE_DIFFICULTY(
        "Harder",
        "⚡",
        "Increase the artistic difficulty and challenge to HARD for this Classic Spark while keeping all 7 categories.",
        "Increase the artistic difficulty and challenge to HARD for this Creative Gap prompt while keeping the fill-in-the-blank structure."
    ),
    COMPOSITION(
        "Composition",
        "📐",
        "Suggest dynamic framing, perspective angles, and focal composition tips for this scene.",
        "Suggest dynamic framing and composition ideas for visualizing this gap sentence."
    ),
    COLOR(
        "Color",
        "🎨",
        "Suggest an evocative, harmonious color palette and lighting mood for this atmosphere.",
        "Suggest an evocative color palette and mood that complements this gap sentence."
    ),
    STORY(
        "Story",
        "📖",
        "Develop a short 1-2 sentence intriguing backstory or character motivation for this subject and scene.",
        "Develop a short 1-2 sentence narrative setup for this fill-in-the-blank prompt."
    );

    fun getInstruction(promptType: PromptType): String = when (promptType) {
        PromptType.CLASSIC_SPARK -> promptInstructionClassic
        PromptType.CREATIVE_GAP -> promptInstructionGap
    }
}

data class BrainstormUiState(
    val messages: List<BrainstormMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isOffline: Boolean = false,
    val isApiKeyMissing: Boolean = false,
    val activePromptType: PromptType = PromptType.CLASSIC_SPARK,
    val currentIdea: BrainstormIdea? = null,
    val activeSeedPrompt: DiscoverPrompt? = null
)
