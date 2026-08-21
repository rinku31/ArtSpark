package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ClassicSpark
import com.example.model.CreativeGap
import com.example.model.Difficulty
import com.example.model.DiscoverPrompt

@Entity(tableName = "prompts")
data class PromptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trait: String,
    val subject: String,
    val action: String,
    val environment: String,
    val atmosphere: String,
    val style: String,
    val challenge: String,
    val storyHook: String = "",
    val isCreativeGap: Boolean,
    val gapTemplate: String?,
    val blankPosition: String = "",
    val suggestedFillIns: String = "",
    val difficulty: String,
    val timestamp: Long,
    val isFavorite: Boolean,
    val isDailySpark: Boolean
) {
    fun toDomain(): DiscoverPrompt {
        val diff = try {
            Difficulty.valueOf(difficulty)
        } catch (e: Exception) {
            Difficulty.MEDIUM
        }
        val fillInsList = if (suggestedFillIns.isNotBlank()) {
            suggestedFillIns.split("|||").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }

        return if (isCreativeGap) {
            CreativeGap(
                id = id,
                difficulty = diff,
                gapSentence = gapTemplate.orEmpty(),
                gapSuggestions = fillInsList,
                style = style,
                challenge = challenge,
                timestamp = timestamp,
                isFavorite = isFavorite,
                isDailySpark = isDailySpark
            )
        } else {
            ClassicSpark(
                id = id,
                difficulty = diff,
                personalityTrait = trait,
                subjectCharacter = subject,
                actionSituationScene = action,
                environment = environment,
                atmosphereWeather = atmosphere,
                artStyle = style,
                creativeChallenge = challenge,
                storyHook = storyHook,
                timestamp = timestamp,
                isFavorite = isFavorite,
                isDailySpark = isDailySpark
            )
        }
    }

    companion object {
        fun fromDomain(prompt: DiscoverPrompt): PromptEntity {
            return when (prompt) {
                is CreativeGap -> {
                    PromptEntity(
                        id = if (prompt.id > 1000000000L) 0 else prompt.id,
                        trait = "",
                        subject = "",
                        action = "",
                        environment = "",
                        atmosphere = "",
                        style = prompt.style,
                        challenge = prompt.challenge,
                        storyHook = "",
                        isCreativeGap = true,
                        gapTemplate = prompt.gapSentence,
                        blankPosition = "",
                        suggestedFillIns = prompt.gapSuggestions.joinToString("|||"),
                        difficulty = prompt.difficulty.name,
                        timestamp = prompt.timestamp,
                        isFavorite = prompt.isFavorite,
                        isDailySpark = prompt.isDailySpark
                    )
                }
                is ClassicSpark -> {
                    PromptEntity(
                        id = if (prompt.id > 1000000000L) 0 else prompt.id,
                        trait = prompt.personalityTrait,
                        subject = prompt.subjectCharacter,
                        action = prompt.actionSituationScene,
                        environment = prompt.environment,
                        atmosphere = prompt.atmosphereWeather,
                        style = prompt.artStyle,
                        challenge = prompt.creativeChallenge,
                        storyHook = prompt.storyHook,
                        isCreativeGap = false,
                        gapTemplate = null,
                        blankPosition = "",
                        suggestedFillIns = "",
                        difficulty = prompt.difficulty.name,
                        timestamp = prompt.timestamp,
                        isFavorite = prompt.isFavorite,
                        isDailySpark = prompt.isDailySpark
                    )
                }
            }
        }
    }
}
