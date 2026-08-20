package com.example.model

data class ArtPrompt(
    val id: Long = 0,
    val trait: String = "",
    val subject: String = "",
    val action: String = "",
    val environment: String = "",
    val atmosphere: String = "",
    val style: String = "",
    val challenge: String = "",
    val isCreativeGap: Boolean = false,
    val gapTemplate: String? = null,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isDailySpark: Boolean = false
) {
    /**
     * Builds the main narrative text for the prompt.
     */
    val narrativeText: String
        get() {
            if (isCreativeGap && !gapTemplate.isNullOrBlank()) {
                return gapTemplate
            }

            val parts = mutableListOf<String>()
            val subjectPhrase = buildString {
                if (trait.isNotBlank()) {
                    append("A ")
                    append(trait.lowercase())
                    append(" ")
                    append(subject.lowercase())
                } else if (subject.isNotBlank()) {
                    append("A ")
                    append(subject.lowercase())
                }
            }

            if (subjectPhrase.isNotBlank()) parts.add(subjectPhrase)
            if (action.isNotBlank()) parts.add(action.lowercase())
            if (environment.isNotBlank()) parts.add(environment.lowercase())
            if (atmosphere.isNotBlank()) parts.add("during ${atmosphere.lowercase()}")
            if (style.isNotBlank()) parts.add("in ${style.lowercase()}")

            return if (parts.isNotEmpty()) {
                parts.joinToString(" ") + "."
            } else {
                "An inspiring spark waiting to be drawn."
            }
        }

    /**
     * Formats the prompt text for native Android sharing sheet.
     */
    fun toShareText(): String {
        return buildString {
            append("ARTSPARK ✨\n\n")
            if (isDailySpark) {
                append("🌟 Daily Spark of the Day:\n")
            } else if (isCreativeGap) {
                append("🎨 Creative Gap Idea:\n")
            } else {
                append("Today's Art Idea:\n")
            }
            append("\"$narrativeText\"\n\n")
            if (style.isNotBlank() && isCreativeGap) {
                append("Style:\n\"$style\"\n\n")
            }
            if (challenge.isNotBlank()) {
                append("Challenge:\n\"$challenge\"\n\n")
            }
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
