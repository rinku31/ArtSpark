package com.example.generator

import java.util.Locale

/**
 * Intelligent sentence and prompt structure builder.
 * Enforces correct English grammar rules including:
 * - Phonetic "a" vs "an" article selection
 * - Comma separation for multiple traits/adjectives
 * - Capitalization and proper punctuation
 * - Natural connector phrasing (inside, during, rendered as)
 * - Structured section extraction for inspiration boards
 */
object PromptSentenceBuilder {

    /**
     * Determines whether "a" or "an" should precede [word].
     */
    fun determineArticle(word: String, capitalize: Boolean = false): String {
        val trimmed = word.trim().lowercase(Locale.ROOT)
        if (trimmed.isEmpty()) return if (capitalize) "A" else "a"

        // Handle specific silent 'h' or vowel exception words
        val isAn = when {
            trimmed.startsWith("hour") ||
            trimmed.startsWith("honor") ||
            trimmed.startsWith("honest") ||
            trimmed.startsWith("heir") -> true
            // Hard 'u' sounds (university, unicorn, unique, user, european, one, once)
            trimmed.startsWith("unicorn") ||
            trimmed.startsWith("university") ||
            trimmed.startsWith("unique") ||
            trimmed.startsWith("uniform") ||
            trimmed.startsWith("user") ||
            trimmed.startsWith("european") ||
            trimmed.startsWith("one") ||
            trimmed.startsWith("once") -> false
            // Standard vowel rule
            trimmed.first() in listOf('a', 'e', 'i', 'o', 'u') -> true
            else -> false
        }

        return if (isAn) {
            if (capitalize) "An" else "an"
        } else {
            if (capitalize) "A" else "a"
        }
    }

    /**
     * Cleans leading articles ("a ", "an ", "the ") from nouns.
     */
    fun cleanNoun(text: String): String {
        val trimmed = text.trim()
        val lower = trimmed.lowercase(Locale.ROOT)
        return when {
            lower.startsWith("an ") -> trimmed.substring(3).trim()
            lower.startsWith("a ") -> trimmed.substring(2).trim()
            lower.startsWith("the ") -> trimmed.substring(4).trim()
            else -> trimmed
        }
    }

    /**
     * Formats multiple traits with natural commas or conjunctions.
     * e.g. ["awestruck", "inquisitive"] -> "awestruck, inquisitive"
     */
    fun formatTraits(traitInput: String): String {
        val trimmed = traitInput.trim()
        if (trimmed.isEmpty()) return ""

        // If the user entered multiple traits separated by comma or "and"
        val tokens = trimmed.split(Regex("[,&]|\\band\\b", RegexOption.IGNORE_CASE))
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }

        return when (tokens.size) {
            0 -> ""
            1 -> tokens[0]
            2 -> "${tokens[0]}, ${tokens[1]}"
            else -> tokens.joinToString(", ")
        }
    }

    /**
     * Builds a grammatically correct Subject Phrase.
     * e.g.:
     *   trait="awestruck, inquisitive", subject="underwater explorer"
     *   -> "An awestruck, inquisitive underwater explorer"
     */
    fun buildSubjectPhrase(trait: String, subject: String, capitalize: Boolean = true): String {
        val cleanSubj = cleanNoun(subject).lowercase(Locale.ROOT)
        val traitsFormatted = formatTraits(trait)

        if (cleanSubj.isBlank() && traitsFormatted.isBlank()) {
            return if (capitalize) "A mysterious creature" else "a mysterious creature"
        }

        if (cleanSubj.isBlank()) {
            val firstWord = traitsFormatted.split(" ", ",").firstOrNull { it.isNotBlank() } ?: "creature"
            val article = determineArticle(firstWord, capitalize)
            return "$article $traitsFormatted creature"
        }

        if (traitsFormatted.isBlank()) {
            val firstWord = cleanSubj.split(" ").firstOrNull { it.isNotBlank() } ?: "creature"
            val article = determineArticle(firstWord, capitalize)
            return "$article $cleanSubj"
        }

        // Both trait and subject are present
        val firstTraitWord = traitsFormatted.split(" ", ",").firstOrNull { it.isNotBlank() } ?: "mysterious"
        val article = determineArticle(firstTraitWord, capitalize)
        return "$article $traitsFormatted $cleanSubj"
    }

    /**
     * Builds a natural Scene / Action phrase.
     * e.g. action="discovering ancient scrolls", environment="a sunken library"
     * -> "Discovering ancient scrolls inside a sunken library"
     */
    fun buildScenePhrase(action: String, environment: String): String {
        val actTrim = action.trim()
        val envTrim = environment.trim()

        if (actTrim.isBlank() && envTrim.isBlank()) return ""
        if (envTrim.isBlank()) return capitalizeFirst(actTrim)
        if (actTrim.isBlank()) {
            val cleanEnv = if (envTrim.lowercase(Locale.ROOT).startsWith("in ") ||
                envTrim.lowercase(Locale.ROOT).startsWith("inside ") ||
                envTrim.lowercase(Locale.ROOT).startsWith("at ")) {
                envTrim
            } else {
                "Inside $envTrim"
            }
            return capitalizeFirst(cleanEnv)
        }

        // Action present and Environment present
        val actLower = actTrim.lowercase(Locale.ROOT)
        val envLower = envTrim.lowercase(Locale.ROOT)

        val connectors = listOf(" inside ", " in ", " at ", " atop ", " under ", " across ", " through ", " from ", " to ")
        val actionEndsWithConnector = connectors.any { actLower.endsWith(it.trim()) || actLower.endsWith(it) }

        val environmentStartsWithConnector = envLower.startsWith("in ") ||
            envLower.startsWith("inside ") ||
            envLower.startsWith("at ") ||
            envLower.startsWith("under ") ||
            envLower.startsWith("atop ") ||
            envLower.startsWith("through ") ||
            envLower.startsWith("across ")

        val fullScene = when {
            actionEndsWithConnector && environmentStartsWithConnector -> {
                // Remove the duplicate connector from environment
                val cleanEnv = envTrim.substringAfter(" ")
                "$actTrim $cleanEnv"
            }
            actionEndsWithConnector -> "$actTrim $envTrim"
            environmentStartsWithConnector -> "$actTrim $envTrim"
            else -> "$actTrim inside $envTrim"
        }

        return capitalizeFirst(fullScene)
    }

    /**
     * Builds a clean Atmosphere phrase for display.
     */
    fun buildAtmospherePhrase(atmosphere: String): String {
        val atm = atmosphere.trim()
        if (atm.isBlank()) return ""
        val lower = atm.lowercase(Locale.ROOT)
        val clean = when {
            lower.startsWith("during ") -> atm.substring(7).trim()
            lower.startsWith("under ") -> atm.substring(6).trim()
            lower.startsWith("with ") -> atm.substring(5).trim()
            lower.startsWith("in ") -> atm.substring(3).trim()
            else -> atm
        }
        return capitalizeFirst(clean)
    }

    /**
     * Builds a clean Style phrase.
     */
    fun buildStylePhrase(style: String): String {
        val st = style.trim()
        if (st.isBlank()) return ""
        val lower = st.lowercase(Locale.ROOT)
        val clean = when {
            lower.startsWith("rendered as ") -> st.substring(12).trim()
            lower.startsWith("rendered in ") -> st.substring(12).trim()
            lower.startsWith("in a ") -> st.substring(5).trim()
            lower.startsWith("in ") -> st.substring(3).trim()
            else -> st
        }
        return capitalizeFirst(clean)
    }

    /**
     * Builds a complete narrative prompt sentence following proper grammar templates:
     * "{Subject Phrase} {verb/action} {environment} during {atmosphere}, rendered as {style}."
     */
    fun buildFullNarrative(
        trait: String,
        subject: String,
        action: String,
        environment: String,
        atmosphere: String,
        style: String
    ): String {
        val subjectPhrase = buildSubjectPhrase(trait, subject, capitalize = true)
        val actTrim = action.trim()
        val envTrim = environment.trim()
        val atmTrim = atmosphere.trim()
        val styTrim = style.trim()

        val sentenceParts = mutableListOf<String>()
        sentenceParts.add(subjectPhrase)

        // Add action and environment
        if (actTrim.isNotBlank() || envTrim.isNotBlank()) {
            val scene = buildScenePhrase(actTrim, envTrim)
            // make first letter lowercase when following subject
            sentenceParts.add(decapitalizeFirst(scene))
        }

        // Add atmosphere
        if (atmTrim.isNotBlank()) {
            val atmLower = atmTrim.lowercase(Locale.ROOT)
            val atmFormatted = when {
                atmLower.startsWith("during ") ||
                atmLower.startsWith("under ") ||
                atmLower.startsWith("in ") ||
                atmLower.startsWith("with ") -> atmTrim
                else -> "during $atmTrim"
            }
            sentenceParts.add(atmFormatted)
        }

        var sentence = sentenceParts.joinToString(" ")

        // Add style rendered as clause
        if (styTrim.isNotBlank()) {
            val styClean = buildStylePhrase(styTrim)
            val styleArticle = determineArticle(styClean, capitalize = false)
            sentence += ", rendered as $styleArticle ${styClean.lowercase(Locale.ROOT)}"
        }

        sentence = sentence.trim()
        if (!sentence.endsWith(".")) {
            sentence += "."
        }
        return sentence
    }

    /**
     * Generates a contextual story hook to spark the artist's imagination.
     */
    fun generateStoryHook(
        subject: String,
        action: String,
        environment: String,
        seed: Long = System.currentTimeMillis()
    ): String {
        val subClean = cleanNoun(subject).lowercase(Locale.ROOT)
        val envClean = cleanNoun(environment).lowercase(Locale.ROOT)

        val hooks = listOf(
            "What secrets are hidden inside the forgotten scrolls?",
            "What ancient mystery is about to be unveiled?",
            "Why did this $subClean journey so far into $envClean?",
            "What happens when the starlight begins to fade?",
            "Who left this glowing artifact behind?",
            "What forgotten promise was made here centuries ago?",
            "What truth lies waiting just beyond the threshold?",
            "Who is watching from the shadows above?",
            "What magical transformation will occur at dawn?",
            "What unspoken story does their quiet expression tell?"
        )

        val index = (Math.abs(seed) % hooks.size).toInt()
        return hooks[index]
    }

    private fun capitalizeFirst(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ""
        return trimmed.substring(0, 1).uppercase(Locale.ROOT) + trimmed.substring(1)
    }

    private fun decapitalizeFirst(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ""
        return trimmed.substring(0, 1).lowercase(Locale.ROOT) + trimmed.substring(1)
    }
}
