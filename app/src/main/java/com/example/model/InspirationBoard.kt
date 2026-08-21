package com.example.model

import com.example.generator.PromptSentenceBuilder

/**
 * Shared Inspiration Board model across Discover, Brainstorm, History, Favorites, and Sharing.
 * Contains all seven core inspiration categories:
 * - Personality / Trait
 * - Subject / Character
 * - Action / Situation / Scene
 * - Environment
 * - Atmosphere & Weather
 * - Art Style
 * - Creative Challenge
 *
 * And when presented as a Creative Gap exercise:
 * - isCreativeGap
 * - gapSentence (e.g. "A curious fox discovers a mysterious ______ hidden beneath an ancient lighthouse.")
 * - blankPosition (e.g. "mysterious discovery")
 * - suggestedFillIns (e.g. ["a miniature pocket universe in a bottle", ...])
 *
 * This structured data is the single source of truth.
 */
data class InspirationBoard(
    val personality: String = "",       // Personality / Trait
    val subject: String = "",           // Subject / Character
    val scene: String = "",             // Action / Situation / Scene
    val environment: String = "",       // Environment
    val atmosphere: String = "",        // Atmosphere & Weather
    val style: String = "",             // Art Style
    val challenge: String = "",         // Creative Challenge
    val isCreativeGap: Boolean = false, // True if this inspiration is a Creative Gap exercise
    val gapSentence: String = "",       // Fill-in-the-blank narrative sentence
    val blankPosition: String = "",     // Role/descriptor of the missing element (e.g. "mysterious discovery")
    val suggestedFillIns: List<String> = emptyList(), // Suggested fill-ins for the gap
    val gapIdeaStarters: List<String> = emptyList()  // Alias / backward compatibility
) {
    // Cross-layer compatibility accessors
    val trait: String get() = personality
    val action: String get() = scene

    /**
     * Resolves the active list of suggested fill-ins / idea starters.
     */
    val displaySuggestedFillIns: List<String>
        get() = when {
            suggestedFillIns.isNotEmpty() -> suggestedFillIns
            gapIdeaStarters.isNotEmpty() -> gapIdeaStarters
            else -> emptyList()
        }

    val isComplete: Boolean
        get() = subject.isNotBlank() || personality.isNotBlank() || scene.isNotBlank() ||
                environment.isNotBlank() || atmosphere.isNotBlank() || style.isNotBlank() ||
                challenge.isNotBlank() || gapSentence.isNotBlank()

    /**
     * Checks if the blank has been filled in.
     */
    val hasFilledGap: Boolean
        get() = !isCreativeGap || (gapSentence.isNotBlank() && !gapSentence.contains("______") && !gapSentence.contains("____"))

    /**
     * Builds the generated human-readable prompt summary from this Inspiration Board.
     */
    val generatedPrompt: String
        get() = if (isCreativeGap && gapSentence.isNotBlank()) {
            gapSentence
        } else {
            PromptSentenceBuilder.buildFullNarrative(
                trait = personality,
                subject = subject,
                action = scene,
                environment = environment,
                atmosphere = atmosphere,
                style = style
            )
        }

    /**
     * Backward-compatible summary sentence method.
     */
    fun summarySentence(): String = generatedPrompt

    /**
     * Structured phrase for the Subject section.
     */
    val subjectPhrase: String
        get() = PromptSentenceBuilder.buildSubjectPhrase(personality, subject, capitalize = true)

    /**
     * Structured phrase for the Scene section.
     */
    val scenePhrase: String
        get() = PromptSentenceBuilder.buildScenePhrase(scene, environment)

    /**
     * Structured phrase for the Atmosphere section.
     */
    val atmospherePhrase: String
        get() = PromptSentenceBuilder.buildAtmospherePhrase(atmosphere)

    /**
     * Structured phrase for the Style section.
     */
    val stylePhrase: String
        get() = PromptSentenceBuilder.buildStylePhrase(style)

    /**
     * Structured phrase for the Challenge section.
     */
    val challengePhrase: String
        get() = challenge.trim()

    /**
     * Retrieves the value of a specific category from this Inspiration Board.
     */
    fun getCategoryValue(category: PromptCategory): String {
        return when (category) {
            PromptCategory.TRAIT -> personality
            PromptCategory.SUBJECT -> subject
            PromptCategory.ACTION -> scene
            PromptCategory.ENVIRONMENT -> environment
            PromptCategory.ATMOSPHERE -> atmosphere
            PromptCategory.STYLE -> style
            PromptCategory.CHALLENGE -> challenge
        }
    }

    /**
     * Returns a new InspirationBoard with a single category updated.
     */
    fun withCategory(category: PromptCategory, value: String): InspirationBoard {
        return when (category) {
            PromptCategory.TRAIT -> copy(personality = value)
            PromptCategory.SUBJECT -> copy(subject = value)
            PromptCategory.ACTION -> copy(scene = value)
            PromptCategory.ENVIRONMENT -> copy(environment = value)
            PromptCategory.ATMOSPHERE -> copy(atmosphere = value)
            PromptCategory.STYLE -> copy(style = value)
            PromptCategory.CHALLENGE -> copy(challenge = value)
        }
    }

    /**
     * Converts or transfers this InspirationBoard into a Discover board:
     * - If the blank has been filled, converts it into a complete Discover inspiration with isCreativeGap = false.
     * - If the blank has not yet been filled, preserves the missing element and allows Discover to continue.
     */
    fun toDiscoverBoard(filledBlankValue: String? = null): InspirationBoard {
        val filled = !filledBlankValue.isNullOrBlank()
        if (filled) {
            val filledValue = filledBlankValue!!.trim()
            val newSentence = if (gapSentence.contains("______")) {
                gapSentence.replace("______", filledValue)
            } else if (gapSentence.contains("____")) {
                gapSentence.replace("____", filledValue)
            } else {
                gapSentence
            }

            // If subject was generic or blank, or if the blank represented the subject/action
            val newSubject = if (subject.isBlank() || subject.equals("mysterious creature", ignoreCase = true)) {
                filledValue
            } else {
                subject
            }

            val newScene = if (scene.isBlank()) {
                "interacting with $filledValue"
            } else {
                scene
            }

            return copy(
                isCreativeGap = false,
                gapSentence = newSentence,
                subject = newSubject,
                scene = newScene
            )
        } else if (hasFilledGap) {
            return copy(isCreativeGap = false)
        } else {
            // Preserve missing element for continued Creative Gap workflow
            return copy(
                isCreativeGap = true,
                gapSentence = gapSentence,
                blankPosition = blankPosition,
                suggestedFillIns = displaySuggestedFillIns,
                gapIdeaStarters = displaySuggestedFillIns
            )
        }
    }

    /**
     * Converts this InspirationBoard directly into an ArtPrompt model.
     */
    fun toArtPrompt(
        id: Long = System.currentTimeMillis(),
        storyHook: String = "",
        isCreativeGap: Boolean = this.isCreativeGap,
        gapTemplate: String? = this.gapSentence.ifBlank { null },
        gapIdeaStarters: List<String> = this.displaySuggestedFillIns,
        blankPosition: String = this.blankPosition,
        difficulty: Difficulty = Difficulty.MEDIUM,
        timestamp: Long = System.currentTimeMillis(),
        isFavorite: Boolean = false,
        isDailySpark: Boolean = false,
        customCategories: Set<PromptCategory> = emptySet()
    ): ArtPrompt {
        val effectiveFillIns = if (gapIdeaStarters.isNotEmpty()) gapIdeaStarters else this.displaySuggestedFillIns
        val effectiveTemplate = gapTemplate ?: this.gapSentence.ifBlank { null }
        val effectiveBoard = this.copy(
            isCreativeGap = isCreativeGap,
            gapSentence = effectiveTemplate.orEmpty(),
            blankPosition = blankPosition,
            suggestedFillIns = effectiveFillIns,
            gapIdeaStarters = effectiveFillIns
        )

        return ArtPrompt(
            id = id,
            board = effectiveBoard,
            storyHook = storyHook,
            isCreativeGap = isCreativeGap,
            gapTemplate = effectiveTemplate,
            gapIdeaStarters = effectiveFillIns,
            blankPosition = blankPosition,
            difficulty = difficulty,
            timestamp = timestamp,
            isFavorite = isFavorite,
            isDailySpark = isDailySpark,
            customCategories = customCategories
        )
    }

    /**
     * Formats the complete AI context prompt with all 7 categories + Creative Gap metadata.
     */
    fun toAiContextString(): String {
        return buildString {
            appendLine("Current Inspiration Board")
            appendLine()
            appendLine("Personality / Trait:")
            appendLine(personality.ifBlank { "(none)" })
            appendLine()
            appendLine("Subject / Character:")
            appendLine(subject.ifBlank { "(none)" })
            appendLine()
            appendLine("Action / Situation / Scene:")
            appendLine(scene.ifBlank { "(none)" })
            appendLine()
            appendLine("Environment:")
            appendLine(environment.ifBlank { "(none)" })
            appendLine()
            appendLine("Atmosphere & Weather:")
            appendLine(atmosphere.ifBlank { "(none)" })
            appendLine()
            appendLine("Art Style:")
            appendLine(style.ifBlank { "(none)" })
            appendLine()
            appendLine("Creative Challenge:")
            appendLine(challenge.ifBlank { "(none)" })
            appendLine()

            if (isCreativeGap || gapSentence.isNotBlank()) {
                appendLine("Creative Gap Exercise (Intentional Blank):")
                appendLine("Gap Sentence: ${gapSentence.ifBlank { generatedPrompt }}")
                if (blankPosition.isNotBlank()) {
                    appendLine("Blank Position / Role: $blankPosition")
                }
                val fillIns = displaySuggestedFillIns
                if (fillIns.isNotEmpty()) {
                    appendLine("Suggested Fill-ins:")
                    fillIns.forEach { appendLine("- $it") }
                }
                appendLine()
            }

            appendLine("Generated Prompt Summary:")
            appendLine(generatedPrompt)
        }
    }

    /**
     * Formats the structured inspiration board text for native sharing.
     */
    fun toShareText(
        storyHook: String = "",
        isDailySpark: Boolean = false
    ): String {
        return buildString {
            append("✨ ARTSPARK INSPIRATION ✨\n\n")
            if (isDailySpark) {
                append("🌟 Daily Spark of the Day\n\n")
            }

            if (isCreativeGap && gapSentence.isNotBlank()) {
                append("CREATIVE GAP EXERCISE:\n")
                append("\"$gapSentence\"\n\n")
                if (blankPosition.isNotBlank()) {
                    append("FOCUS: $blankPosition\n\n")
                }
                val starters = displaySuggestedFillIns
                if (starters.isNotEmpty()) {
                    append("IDEA STARTERS FOR THE BLANK:\n")
                    starters.forEach { append("• $it\n") }
                    append("\n")
                }
                if (stylePhrase.isNotBlank()) {
                    append("STYLE:\n$stylePhrase\n\n")
                }
                if (challengePhrase.isNotBlank()) {
                    append("CHALLENGE:\n$challengePhrase\n\n")
                }
            } else {
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

                if (storyHook.isNotBlank()) {
                    append("STORY HOOK:\n$storyHook\n\n")
                }

                append("Full Prompt:\n\"$generatedPrompt\"\n\n")
            }

            append("Break the block. Make something with ArtSpark!")
        }
    }
}
