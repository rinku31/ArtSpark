package com.example.model

/**
 * Type alias connecting legacy ArtPrompt to the sealed DiscoverPrompt interface.
 */
typealias ArtPrompt = DiscoverPrompt

/**
 * Factory helper function for creating DiscoverPrompt instances.
 */
fun ArtPrompt(
    id: Long = 0,
    trait: String = "",
    subject: String = "",
    action: String = "",
    environment: String = "",
    atmosphere: String = "",
    style: String = "",
    challenge: String = "",
    storyHook: String = "",
    isCreativeGap: Boolean = false,
    gapTemplate: String? = null,
    gapIdeaStarters: List<String> = emptyList(),
    blankPosition: String = "",
    difficulty: Difficulty = Difficulty.MEDIUM,
    timestamp: Long = System.currentTimeMillis(),
    isFavorite: Boolean = false,
    isDailySpark: Boolean = false,
    customCategories: Set<PromptCategory> = emptySet(),
    board: InspirationBoard? = null
): DiscoverPrompt {
    return if (isCreativeGap) {
        val sentence = gapTemplate ?: board?.gapSentence.orEmpty()
        val suggestions = if (gapIdeaStarters.isNotEmpty()) gapIdeaStarters else board?.displaySuggestedFillIns ?: emptyList()
        CreativeGap(
            id = id,
            difficulty = difficulty,
            gapSentence = sentence,
            gapSuggestions = suggestions,
            style = style.ifBlank { board?.style.orEmpty() },
            challenge = challenge.ifBlank { board?.challenge.orEmpty() },
            timestamp = timestamp,
            isFavorite = isFavorite,
            isDailySpark = isDailySpark
        )
    } else {
        val actualTrait = trait.ifBlank { board?.personality.orEmpty() }
        val actualSubject = subject.ifBlank { board?.subject.orEmpty() }
        val actualAction = action.ifBlank { board?.scene.orEmpty() }
        val actualEnv = environment.ifBlank { board?.environment.orEmpty() }
        val actualAtm = atmosphere.ifBlank { board?.atmosphere.orEmpty() }
        val actualStyle = style.ifBlank { board?.style.orEmpty() }
        val actualChallenge = challenge.ifBlank { board?.challenge.orEmpty() }

        ClassicSpark(
            id = id,
            difficulty = difficulty,
            personalityTrait = actualTrait,
            subjectCharacter = actualSubject,
            actionSituationScene = actualAction,
            environment = actualEnv,
            atmosphereWeather = actualAtm,
            artStyle = actualStyle,
            creativeChallenge = actualChallenge,
            storyHook = storyHook,
            timestamp = timestamp,
            isFavorite = isFavorite,
            isDailySpark = isDailySpark,
            customCategories = customCategories
        )
    }
}
