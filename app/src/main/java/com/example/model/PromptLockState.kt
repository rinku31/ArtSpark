package com.example.model

data class PromptLockState(
    val lockedCategories: Set<PromptCategory> = emptySet(),
    val lockedValues: Map<PromptCategory, String> = emptyMap()
) {
    fun isLocked(category: PromptCategory): Boolean = category in lockedCategories

    fun toggleLock(category: PromptCategory, currentValue: String): PromptLockState {
        return if (isLocked(category)) {
            copy(
                lockedCategories = lockedCategories - category,
                lockedValues = lockedValues - category
            )
        } else {
            copy(
                lockedCategories = lockedCategories + category,
                lockedValues = lockedValues + (category to currentValue)
            )
        }
    }

    fun clearAll(): PromptLockState {
        return PromptLockState()
    }
}
