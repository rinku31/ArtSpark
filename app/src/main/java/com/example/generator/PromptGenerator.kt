package com.example.generator

import com.example.model.ArtPrompt
import com.example.model.Difficulty
import com.example.model.PromptCategory
import com.example.model.PromptLockState
import java.util.Calendar
import java.util.TimeZone
import kotlin.random.Random

object PromptGenerator {

    /**
     * Generates an ArtPrompt based on difficulty, lock state, and mode.
     */
    fun generate(
        difficulty: Difficulty = Difficulty.MEDIUM,
        lockState: PromptLockState = PromptLockState(),
        isCreativeGap: Boolean = false,
        enabledCategories: Set<PromptCategory> = PromptCategory.values().toSet(),
        random: Random = Random.Default
    ): ArtPrompt {
        val trait = if (lockState.isLocked(PromptCategory.TRAIT)) {
            lockState.lockedValues[PromptCategory.TRAIT] ?: PromptData.traits.random(random)
        } else if (PromptCategory.TRAIT in enabledCategories) {
            PromptData.traits.random(random)
        } else ""

        val subject = if (lockState.isLocked(PromptCategory.SUBJECT)) {
            lockState.lockedValues[PromptCategory.SUBJECT] ?: PromptData.subjects.random(random)
        } else if (PromptCategory.SUBJECT in enabledCategories) {
            PromptData.subjects.random(random)
        } else "Creature"

        val action = if (lockState.isLocked(PromptCategory.ACTION)) {
            lockState.lockedValues[PromptCategory.ACTION] ?: PromptData.actions.random(random)
        } else if (PromptCategory.ACTION in enabledCategories && difficulty != Difficulty.EASY) {
            PromptData.actions.random(random)
        } else if (difficulty == Difficulty.EASY) {
            // Keep easy prompts lightweight
            listOf("resting in", "observing", "guarding", "exploring").random(random)
        } else ""

        val environment = if (lockState.isLocked(PromptCategory.ENVIRONMENT)) {
            lockState.lockedValues[PromptCategory.ENVIRONMENT] ?: PromptData.environments.random(random)
        } else if (PromptCategory.ENVIRONMENT in enabledCategories) {
            PromptData.environments.random(random)
        } else ""

        val atmosphere = if (lockState.isLocked(PromptCategory.ATMOSPHERE)) {
            lockState.lockedValues[PromptCategory.ATMOSPHERE] ?: PromptData.atmospheres.random(random)
        } else if (PromptCategory.ATMOSPHERE in enabledCategories && difficulty != Difficulty.EASY) {
            PromptData.atmospheres.random(random)
        } else if (difficulty == Difficulty.EASY && random.nextBoolean()) {
            listOf("a warm sunny afternoon", "a starry night", "a peaceful sunset").random(random)
        } else ""

        val style = if (lockState.isLocked(PromptCategory.STYLE)) {
            lockState.lockedValues[PromptCategory.STYLE] ?: PromptData.styles.random(random)
        } else if (PromptCategory.STYLE in enabledCategories) {
            PromptData.styles.random(random)
        } else ""

        val challenge = if (lockState.isLocked(PromptCategory.CHALLENGE)) {
            lockState.lockedValues[PromptCategory.CHALLENGE] ?: PromptData.challenges.random(random)
        } else if (PromptCategory.CHALLENGE in enabledCategories) {
            when (difficulty) {
                Difficulty.EASY -> {
                    // Easier challenges
                    listOf(
                        "Use only three colors",
                        "Draw in 15 minutes",
                        "Soft pastel tones only",
                        "Focus on cute proportions"
                    ).random(random)
                }
                Difficulty.MEDIUM -> PromptData.challenges.random(random)
                Difficulty.HARD -> {
                    // Stricter challenges
                    listOf(
                        "Use your non-dominant hand for the initial sketch",
                        "No erasing allowed + blue ink only",
                        "Continuous line drawing without lifting the pencil",
                        "Dramatic extreme low-angle perspective",
                        "Invert values: white ink on black surface only",
                        "Strictly geometric shapes, no curves"
                    ).random(random)
                }
            }
        } else ""

        var gapTemplate: String? = null
        if (isCreativeGap) {
            val templateRaw = PromptData.creativeGapTemplates.random(random)
            val subClean = subject.lowercase()
            val envClean = environment.removePrefix("an ").removePrefix("a ").lowercase()
            val atmClean = if (atmosphere.isNotBlank()) atmosphere.lowercase() else "a quiet starry night"

            gapTemplate = try {
                when {
                    templateRaw.contains("%s.*%s.*%s".toRegex()) -> {
                        String.format(templateRaw, subClean, envClean, atmClean)
                    }
                    templateRaw.contains("%s.*%s".toRegex()) -> {
                        String.format(templateRaw, subClean, envClean)
                    }
                    else -> {
                        String.format(templateRaw, subClean)
                    }
                }
            } catch (e: Exception) {
                "A $trait $subClean discovers a mysterious ______ in $environment."
            }
        }

        return ArtPrompt(
            id = System.currentTimeMillis() + random.nextInt(1000),
            trait = trait,
            subject = subject,
            action = action,
            environment = environment,
            atmosphere = atmosphere,
            style = style,
            challenge = challenge,
            isCreativeGap = isCreativeGap,
            gapTemplate = gapTemplate,
            difficulty = difficulty,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Generates deterministic Daily Spark based on current calendar date.
     */
    fun generateDailySpark(): ArtPrompt {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        val year = calendar.get(Calendar.YEAR)
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val dailySeed = (year * 1000L + dayOfYear) * 31L + 42L
        val random = Random(dailySeed)

        val trait = PromptData.traits.random(random)
        val subject = PromptData.subjects.random(random)
        val action = PromptData.actions.random(random)
        val environment = PromptData.environments.random(random)
        val atmosphere = PromptData.atmospheres.random(random)
        val style = PromptData.styles.random(random)
        val challenge = PromptData.challenges.random(random)

        return ArtPrompt(
            id = dailySeed,
            trait = trait,
            subject = subject,
            action = action,
            environment = environment,
            atmosphere = atmosphere,
            style = style,
            challenge = challenge,
            isCreativeGap = false,
            difficulty = Difficulty.MEDIUM,
            timestamp = calendar.timeInMillis,
            isDailySpark = true
        )
    }

    /**
     * Creates a similar prompt by preserving subject/style and re-rolling remaining attributes.
     */
    fun createSimilar(basePrompt: ArtPrompt): ArtPrompt {
        val lockState = PromptLockState(
            lockedCategories = setOf(PromptCategory.SUBJECT, PromptCategory.STYLE),
            lockedValues = mapOf(
                PromptCategory.SUBJECT to basePrompt.subject,
                PromptCategory.STYLE to basePrompt.style
            )
        )
        return generate(
            difficulty = basePrompt.difficulty,
            lockState = lockState,
            isCreativeGap = basePrompt.isCreativeGap
        )
    }
}
