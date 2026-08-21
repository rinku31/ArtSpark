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
                "Inside ${decapitalizeFirst(envTrim)}"
            }
            return capitalizeFirst(cleanEnv)
        }

        // Action present and Environment present
        val actLower = actTrim.lowercase(Locale.ROOT)
        val envLower = envTrim.lowercase(Locale.ROOT)

        val connectors = listOf(" inside ", " in ", " at ", " atop ", " under ", " across ", " through ", " from ", " to ", " inside", " in", " at", " atop", " under", " across", " through")
        val actionEndsWithConnector = connectors.any { actLower.endsWith(it) }

        val environmentStartsWithConnector = envLower.startsWith("in ") ||
            envLower.startsWith("inside ") ||
            envLower.startsWith("at ") ||
            envLower.startsWith("under ") ||
            envLower.startsWith("atop ") ||
            envLower.startsWith("through ") ||
            envLower.startsWith("across ") ||
            envLower.startsWith("amidst ") ||
            envLower.startsWith("from ")

        val fullScene = when {
            actionEndsWithConnector && environmentStartsWithConnector -> {
                // Remove the duplicate connector from environment and decapitalize the rest
                val remainingEnv = envTrim.substringAfter(" ")
                "$actTrim ${decapitalizeFirst(remainingEnv)}"
            }
            actionEndsWithConnector -> "$actTrim ${decapitalizeFirst(envTrim)}"
            environmentStartsWithConnector -> "$actTrim ${decapitalizeFirst(envTrim)}"
            else -> "$actTrim inside ${decapitalizeFirst(envTrim)}"
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
            lower.startsWith("at ") -> atm.substring(3).trim()
            lower.startsWith("amidst ") -> atm.substring(7).trim()
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
     * Builds a clean Challenge phrase.
     */
    fun buildChallengePhrase(challenge: String): String {
        val ch = challenge.trim()
        if (ch.isBlank()) return ""
        val clean = if (ch.lowercase(Locale.ROOT).startsWith("challenge: ")) ch.substring(11).trim() else ch
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
                atmLower.startsWith("with ") ||
                atmLower.startsWith("amidst ") ||
                atmLower.startsWith("at ") -> {
                    val connector = atmTrim.substringBefore(" ").lowercase(Locale.ROOT)
                    val rest = atmTrim.substringAfter(" ").trim()
                    "$connector ${decapitalizeFirst(rest)}"
                }
                else -> "during ${decapitalizeFirst(atmTrim)}"
            }
            sentenceParts.add(atmFormatted)
        }

        var sentence = sentenceParts.joinToString(" ")

        // Add style rendered as clause
        if (styTrim.isNotBlank()) {
            val styClean = buildStylePhrase(styTrim)
            val cleanNoArticle = cleanNoun(styClean)
            val styleArticle = determineArticle(cleanNoArticle, capitalize = false)
            val formattedStyle = decapitalizeFirst(cleanNoArticle)
            sentence += ", rendered as $styleArticle $formattedStyle"
        }

        sentence = sentence.trim()
        if (!sentence.endsWith(".")) {
            sentence += "."
        }
        return polishGrammar(sentence)
    }

    /**
     * Builds a full prompt sentence including optional creative challenge.
     */
    fun buildFullPromptSentence(
        trait: String,
        subject: String,
        action: String,
        environment: String,
        atmosphere: String,
        style: String,
        challenge: String = ""
    ): String {
        val narrative = buildFullNarrative(trait, subject, action, environment, atmosphere, style)
        if (challenge.isNotBlank()) {
            val cleanChallenge = buildChallengePhrase(challenge)
            return "$narrative Challenge: $cleanChallenge"
        }
        return narrative
    }

    /**
     * Comprehensive grammar polish utility:
     * - Fixes duplicate words and duplicate articles ("a a", "the the", "in in", etc.)
     * - Fixes phonetic "a" vs "an" across the entire sentence
     * - Fixes capitalization after mid-sentence prepositions & connectors for seamless continuity
     * - Corrects punctuation spacing and comma placement
     * - Fixes sentence-starting capitalization and sentence endings
     */
    fun polishGrammar(sentence: String): String {
        if (sentence.isBlank()) return ""
        var text = sentence.trim()

        // 1. Fix duplicate articles & prepositions (case-insensitive)
        text = text.replace(Regex("(?i)\\b(a|an|the|in|inside|at|during|rendered|as)\\s+\\1\\b"), "$1")
        text = text.replace(Regex("(?i)\\b(a|an)\\s+(a|an)\\b"), "a")
        text = text.replace(Regex("(?i)\\b(in|inside)\\s+(in|inside)\\b"), "inside")

        // 2. Fix mid-sentence uppercase words following connectors (e.g. "inside A", "during Blazing", "with Glowing", "into Dark")
        val connectorsList = "inside|in|into|during|under|with|within|at|atop|through|across|amidst|as|rendered as|between|against|from|along|beside|by"
        val midSentenceConnectorRegex = Regex("(?i)\\b($connectorsList)\\s+([A-Z])([a-zA-Z]*)")
        text = midSentenceConnectorRegex.replace(text) { matchResult ->
            val connector = matchResult.groups[1]?.value ?: ""
            val firstChar = matchResult.groups[2]?.value?.lowercase(Locale.ROOT) ?: ""
            val restOfWord = matchResult.groups[3]?.value ?: ""
            "$connector $firstChar$restOfWord"
        }

        // 3. Fix words following articles mid-sentence (e.g. "a Chaotic" -> "a chaotic", "an Enormous" -> "an enormous")
        val articleFollowerRegex = Regex("(?i)\\b(a|an|the)\\s+([A-Z])([a-zA-Z]*)")
        text = articleFollowerRegex.replace(text) { matchResult ->
            val article = matchResult.groups[1]?.value ?: ""
            val firstChar = matchResult.groups[2]?.value?.lowercase(Locale.ROOT) ?: ""
            val restOfWord = matchResult.groups[3]?.value ?: ""
            "$article $firstChar$restOfWord"
        }

        // 4. Fix improper punctuation spacing
        text = text.replace(Regex("\\s+([,.:;?!])"), "$1") // space before punctuation
        text = text.replace(Regex("([,.:;?!])(?=[A-Za-z0-9])"), "$1 ") // missing space after punctuation
        text = text.replace(Regex(",\\s*,"), ",") // double commas
        text = text.replace(Regex("\\s{2,}"), " ") // duplicate whitespace

        // 5. Fix phonetic "a" vs "an" dynamically across the whole sentence
        val articleRegex = Regex("(?i)\\b(a|an)\\s+([a-zA-Z]+)")
        text = articleRegex.replace(text) { matchResult ->
            val isFirstCapital = matchResult.groups[1]?.value?.first()?.isUpperCase() ?: false
            val followingWord = matchResult.groups[2]?.value ?: ""
            val correctArticle = determineArticle(followingWord, capitalize = isFirstCapital)
            "$correctArticle ${decapitalizeFirst(followingWord)}"
        }

        // 6. Clean leading/trailing spaces & ensure sentence-start capitalization
        text = text.trim()
        if (text.isNotEmpty()) {
            text = text.substring(0, 1).uppercase(Locale.ROOT) + text.substring(1)
        }

        // 7. Ensure ends with valid closing punctuation
        if (text.isNotEmpty() && !text.endsWith(".") && !text.endsWith("!") && !text.endsWith("?")) {
            text += "."
        }

        return text
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
