package com.example.model

enum class PromptCategory(
    val title: String,
    val iconName: String,
    val description: String
) {
    TRAIT("Personality / Trait", "psychology", "The mood, posture, or character quirk"),
    SUBJECT("Subject / Character", "face", "The central creature, figure, or being"),
    ACTION("Action / Situation", "directions_run", "What they are doing or caught in the middle of"),
    ENVIRONMENT("Environment", "landscape", "The setting, architecture, or terrain"),
    ATMOSPHERE("Atmosphere & Weather", "cloud", "Lighting, weather, or cosmic phenomenon"),
    STYLE("Art Style", "palette", "The artistic medium or illustration technique"),
    CHALLENGE("Creative Challenge", "timer", "A playful artistic limitation or rule")
}
