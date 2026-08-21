package com.example.model

import java.util.UUID

enum class MessageSender {
    USER,
    AI
}

data class ArtSparkIdea(
    val subject: String = "",
    val trait: String = "",
    val action: String = "",
    val environment: String = "",
    val atmosphere: String = "",
    val style: String = "",
    val challenge: String = ""
) {
    val isComplete: Boolean
        get() = subject.isNotBlank() || trait.isNotBlank() || environment.isNotBlank() || style.isNotBlank()

    fun summarySentence(): String {
        val parts = mutableListOf<String>()
        val subjTrait = buildString {
            if (trait.isNotBlank()) append("${trait.trim()} ")
            if (subject.isNotBlank()) append(subject.trim())
        }.trim()

        if (subjTrait.isNotBlank()) {
            val firstChar = subjTrait.firstOrNull()?.lowercaseChar() ?: 'a'
            val article = if (firstChar in listOf('a', 'e', 'i', 'o', 'u')) "An" else "A"
            parts.add("$article $subjTrait")
        }

        if (action.isNotBlank()) parts.add(action.trim())
        if (environment.isNotBlank()) parts.add(if (environment.startsWith("in ", ignoreCase = true) || environment.startsWith("at ", ignoreCase = true)) environment else "in $environment")
        if (atmosphere.isNotBlank()) parts.add(if (atmosphere.startsWith("during ", ignoreCase = true) || atmosphere.startsWith("under ", ignoreCase = true)) atmosphere else "during $atmosphere")
        if (style.isNotBlank()) parts.add(if (style.startsWith("in ", ignoreCase = true)) style else "in $style")

        return if (parts.isNotEmpty()) parts.joinToString(" ") + "." else "A creative spark."
    }
}

data class BrainstormMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val quickPills: List<String> = emptyList(),
    val idea: ArtSparkIdea? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val isContextSummary: Boolean = false
)

enum class QuickAiAction(
    val title: String,
    val iconEmoji: String,
    val promptInstruction: String
) {
    EXPAND("Expand", "🔍", "Expand on this idea with richer visual textures, props, and detailed setting nuances."),
    SIMPLIFY("Simplify", "✏️", "Simplify this concept into clear, bold shapes that are easy and quick for an artist to sketch."),
    TWIST("Twist", "🌀", "Introduce an unexpected, clever, or surreal twist to this concept."),
    VARIATIONS("Variations", "🎲", "Give me 3 distinct variations or creative spins of this idea."),
    COMPOSITION("Composition", "📐", "Suggest dynamic framing, perspective angles, and focal composition tips."),
    COLOR("Color", "🎨", "Suggest an evocative, harmonious color palette for this piece."),
    STORY("Story", "📖", "Develop a short 1-2 sentence intriguing backstory or character motivation.")
}

data class BrainstormUiState(
    val messages: List<BrainstormMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isOffline: Boolean = false,
    val isApiKeyMissing: Boolean = false,
    val currentIdea: ArtSparkIdea? = null,
    val activeSeedPrompt: ArtPrompt? = null
)
