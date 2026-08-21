package com.example.data.local

import com.example.model.ArtPrompt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PromptRepository(private val promptDao: PromptDao) {

    val history: Flow<List<ArtPrompt>> = promptDao.getAllHistory().map { list ->
        list.map { it.toDomain() }
    }

    val favorites: Flow<List<ArtPrompt>> = promptDao.getFavorites().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun savePrompt(prompt: ArtPrompt): Long {
        val entity = PromptEntity.fromDomain(prompt)
        return promptDao.insert(entity)
    }

    suspend fun toggleFavorite(prompt: ArtPrompt): Boolean {
        val newStatus = !prompt.isFavorite
        if (prompt.id > 0) {
            promptDao.updateFavoriteStatus(prompt.id, newStatus)
        } else {
            val entity = PromptEntity.fromDomain(prompt.copyWithFavorite(newStatus))
            promptDao.insert(entity)
        }
        return newStatus
    }

    suspend fun deletePrompt(prompt: ArtPrompt) {
        if (prompt.id > 0) {
            promptDao.deleteById(prompt.id)
        }
    }

    suspend fun clearHistory() {
        promptDao.clearHistoryNonFavorites()
    }

    suspend fun clearAll() {
        promptDao.clearAll()
    }
}
