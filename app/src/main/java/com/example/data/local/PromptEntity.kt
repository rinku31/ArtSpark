package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ArtPrompt
import com.example.model.Difficulty

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
    val isCreativeGap: Boolean,
    val gapTemplate: String?,
    val difficulty: String,
    val timestamp: Long,
    val isFavorite: Boolean,
    val isDailySpark: Boolean
) {
    fun toDomain(): ArtPrompt {
        val diff = try {
            Difficulty.valueOf(difficulty)
        } catch (e: Exception) {
            Difficulty.MEDIUM
        }
        return ArtPrompt(
            id = id,
            trait = trait,
            subject = subject,
            action = action,
            environment = environment,
            atmosphere = atmosphere,
            style = style,
            challenge = challenge,
            isCreativeGap = isCreativeGap,
            gapTemplate = gapTemplate,
            difficulty = diff,
            timestamp = timestamp,
            isFavorite = isFavorite,
            isDailySpark = isDailySpark
        )
    }

    companion object {
        fun fromDomain(prompt: ArtPrompt): PromptEntity {
            return PromptEntity(
                id = if (prompt.id > 1000000000L) 0 else prompt.id,
                trait = prompt.trait,
                subject = prompt.subject,
                action = prompt.action,
                environment = prompt.environment,
                atmosphere = prompt.atmosphere,
                style = prompt.style,
                challenge = prompt.challenge,
                isCreativeGap = prompt.isCreativeGap,
                gapTemplate = prompt.gapTemplate,
                difficulty = prompt.difficulty.name,
                timestamp = prompt.timestamp,
                isFavorite = prompt.isFavorite,
                isDailySpark = prompt.isDailySpark
            )
        }
    }
}
