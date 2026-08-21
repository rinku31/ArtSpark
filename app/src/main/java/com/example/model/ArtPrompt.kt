package com.example.model

import com.example.generator.PromptSentenceBuilder

data class ArtPrompt(
    val id: Long = 0,
    val trait: String = "",
    val subject: String = "",
    val action: String = "",
    val environment: String = "",
    val atmosphere: String = "",
    val style: String = "",
    val challenge: String = "",
    val storyHook: String = "",
    val isCreativeGap: Boolean = false,
    val gapTemplate: String? = null,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isDailySpark: Boolean = false,
    val customCategories: Set<PromptCategory> = emptySet()
) {
    /**
     * Structured phrase for the Subject section.
     * e.g., "An awestruck, inquisitive underwater explorer"
     */
    val subjectPhrase: String
        get() = PromptSentenceBuilder.buildSubjectPhrase(trait, subject, capitalize = true)

    /**
     * Structured phrase for the Scene section.
     * e.g., "Discovering ancient scrolls inside a sunken library"
     */
    val scenePhrase: String
        get() = PromptSentenceBuilder.buildScenePhrase(action, environment)

    /**
     * Structured phrase for the Atmosphere section.
     * e.g., "Golden sunlight filtering through deep blue water"
     */
    val atmospherePhrase: String
        get() = PromptSentenceBuilder.buildAtmospherePhrase(atmosphere)

    /**
     * Structured phrase for the Style section.
     * e.g., "Chiaroscuro Digital Painting"
     */
    val stylePhrase: String
        get() = PromptSentenceBuilder.buildStylePhrase(style)

    /**
     * Structured phrase for the Challenge section.
     */
    val challengePhrase: String
        get() = challenge.trim()

    /**
     * Computed story hook if not provided explicitly.
     */
    val displayStoryHook: String
        get() = if (storyHook.isNotBlank()) storyHook else PromptSentenceBuilder.generateStoryHook(subject, action, environment, id)

    /**
     * Builds the main narrative text using intelligent grammar templates.
     */
    val narrativeText: String
        get() {
            if (isCreativeGap && !gapTemplate.isNullOrBlank()) {
                return gapTemplate
            }
            return PromptSentenceBuilder.buildFullNarrative(
                trait = trait,
                subject = subject,
                action = action,
                environment = environment,
                atmosphere = atmosphere,
                style = style
            )
        }

    /**
     * Formats the structured prompt text for native sharing.
     */
    fun toShareText(): String {
        return buildString {
            append("✨ ARTSPARK INSPIRATION ✨\n\n")
            if (isDailySpark) {
                append("🌟 Daily Spark of the Day\n\n")
            }

            append("SUBJECT:\n$subjectPhrase\n\n")

            if (scenePhrase.isNotBlank()) {
                append("SCENE:\n$scenePhrase\n\n")
            }

            if (atmospherePhrase.isNotBlank()) {
                append("ATMOSPHERE:\n$atmospherePhrase\n\n")
            }

            if (stylePhrase.isNotBlank()) {
                append("STYLE:\n$stylePhrase\n\n")
            }

            if (challengePhrase.isNotBlank()) {
                append("CHALLENGE:\n$challengePhrase\n\n")
            }

            if (displayStoryHook.isNotBlank()) {
                append("STORY HOOK:\n$displayStoryHook\n\n")
            }

            append("Full Prompt:\n\"$narrativeText\"\n\n")
            append("Break the block. Make something with ArtSpark!")
        }
    }

    /**
     * Retrieves the value of a specific category from this prompt.
     */
    fun getCategoryValue(category: PromptCategory): String {
        return when (category) {
            PromptCategory.TRAIT -> trait
            PromptCategory.SUBJECT -> subject
            PromptCategory.ACTION -> action
            PromptCategory.ENVIRONMENT -> environment
            PromptCategory.ATMOSPHERE -> atmosphere
            PromptCategory.STYLE -> style
            PromptCategory.CHALLENGE -> challenge
        }
    }
}

