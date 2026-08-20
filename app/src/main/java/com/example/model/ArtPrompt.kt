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
    val isDailySpark: Boolean = false,
    val customCategories: Set<PromptCategory> = emptySet()
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
                val subTrim = subject.trim()
                val traitTrim = trait.trim()
                if (traitTrim.isNotBlank() && subTrim.isNotBlank()) {
                    if (subTrim.startsWith("a ", ignoreCase = true) ||
                        subTrim.startsWith("an ", ignoreCase = true) ||
                        subTrim.startsWith("the ", ignoreCase = true)) {
                        append("$traitTrim $subTrim")
                    } else {
                        val firstChar = traitTrim.firstOrNull()?.lowercaseChar() ?: 'a'
                        val article = if (firstChar in listOf('a', 'e', 'i', 'o', 'u')) "An" else "A"
                        append("$article ${traitTrim.lowercase()} $subTrim")
                    }
                } else if (subTrim.isNotBlank()) {
                    if (subTrim.startsWith("a ", ignoreCase = true) ||
                        subTrim.startsWith("an ", ignoreCase = true) ||
                        subTrim.startsWith("the ", ignoreCase = true)) {
                        append(subTrim)
                    } else {
                        val firstChar = subTrim.firstOrNull()?.lowercaseChar() ?: 'a'
                        val article = if (firstChar in listOf('a', 'e', 'i', 'o', 'u')) "An" else "A"
                        append("$article $subTrim")
                    }
                }
            }

            if (subjectPhrase.isNotBlank()) parts.add(subjectPhrase)
            if (action.isNotBlank()) parts.add(action.trim())
            if (environment.isNotBlank()) {
                val env = environment.trim()
                if (env.startsWith("in ", ignoreCase = true) ||
                    env.startsWith("at ", ignoreCase = true) ||
                    env.startsWith("under ", ignoreCase = true) ||
                    env.startsWith("on ", ignoreCase = true) ||
                    env.startsWith("inside ", ignoreCase = true) ||
                    env.startsWith("through ", ignoreCase = true)) {
                    parts.add(env)
                } else {
                    parts.add("in $env")
                }
            }
            if (atmosphere.isNotBlank()) {
                val atm = atmosphere.trim()
                if (atm.startsWith("during ", ignoreCase = true) ||
                    atm.startsWith("under ", ignoreCase = true) ||
                    atm.startsWith("in ", ignoreCase = true) ||
                    atm.startsWith("with ", ignoreCase = true)) {
                    parts.add(atm)
                } else {
                    parts.add("during $atm")
                }
            }
            if (style.isNotBlank()) {
                val st = style.trim()
                if (st.startsWith("in ", ignoreCase = true) ||
                    st.startsWith("using ", ignoreCase = true)) {
                    parts.add(st)
                } else {
                    parts.add("in $st")
                }
            }

            return if (parts.isNotEmpty()) {
                val joined = parts.joinToString(" ")
                if (joined.endsWith(".")) joined else "$joined."
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
