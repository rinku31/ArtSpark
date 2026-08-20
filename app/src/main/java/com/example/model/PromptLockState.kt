package com.example.model

data class PromptLockState(
    val lockedCategories: Set<PromptCategory> = emptySet(),
    val lockedValues: Map<PromptCategory, String> = emptyMap(),
    val categoryModes: Map<PromptCategory, CategorySelectionMode> = emptyMap(),
    val selectedValues: Map<PromptCategory, String> = emptyMap(),
    val customValues: Map<PromptCategory, String> = emptyMap()
) {
    fun getMode(category: PromptCategory): CategorySelectionMode {
        return categoryModes[category] ?: if (category in lockedCategories) {
            CategorySelectionMode.CHOOSE
        } else {
            CategorySelectionMode.RANDOM
        }
    }

    fun isLocked(category: PromptCategory): Boolean {
        val mode = getMode(category)
        return mode == CategorySelectionMode.CHOOSE ||
                (mode == CategorySelectionMode.CUSTOM && customValues[category]?.isNotBlank() == true) ||
                category in lockedCategories
    }

    fun isCustom(category: PromptCategory): Boolean {
        return getMode(category) == CategorySelectionMode.CUSTOM && customValues[category]?.isNotBlank() == true
    }

    fun getSelectedValue(category: PromptCategory): String {
        return selectedValues[category] ?: lockedValues[category].orEmpty()
    }

    fun getCustomValue(category: PromptCategory): String {
        return customValues[category].orEmpty()
    }

    fun getEffectiveValue(category: PromptCategory): String? {
        val mode = getMode(category)
        return when (mode) {
            CategorySelectionMode.CUSTOM -> {
                val custom = customValues[category]
                if (!custom.isNullOrBlank()) custom else null
            }
            CategorySelectionMode.CHOOSE -> {
                selectedValues[category] ?: lockedValues[category]
            }
            CategorySelectionMode.RANDOM -> null
        }
    }

    fun setCategoryMode(category: PromptCategory, mode: CategorySelectionMode, fallbackValue: String = ""): PromptLockState {
        val newModes = categoryModes + (category to mode)
        val isNowFixed = when (mode) {
            CategorySelectionMode.RANDOM -> false
            CategorySelectionMode.CHOOSE -> true
            CategorySelectionMode.CUSTOM -> customValues[category]?.isNotBlank() == true
        }

        val newLockedCats = if (isNowFixed) lockedCategories + category else lockedCategories - category
        val newSelectedValues = if (mode == CategorySelectionMode.CHOOSE && !selectedValues.containsKey(category) && fallbackValue.isNotBlank()) {
            selectedValues + (category to fallbackValue)
        } else selectedValues

        return copy(
            categoryModes = newModes,
            lockedCategories = newLockedCats,
            selectedValues = newSelectedValues
        )
    }

    fun setSelectedValue(category: PromptCategory, value: String): PromptLockState {
        return copy(
            categoryModes = categoryModes + (category to CategorySelectionMode.CHOOSE),
            lockedCategories = lockedCategories + category,
            selectedValues = selectedValues + (category to value),
            lockedValues = lockedValues + (category to value)
        )
    }

    fun setCustomValue(category: PromptCategory, value: String): PromptLockState {
        val trimmed = value.trim()
        val isNotEmpty = trimmed.isNotBlank()
        val newLocked = if (isNotEmpty) lockedCategories + category else lockedCategories - category
        return copy(
            categoryModes = categoryModes + (category to CategorySelectionMode.CUSTOM),
            customValues = customValues + (category to value),
            lockedCategories = newLocked
        )
    }

    fun toggleLock(category: PromptCategory, currentValue: String): PromptLockState {
        return if (isLocked(category)) {
            // Unlock to RANDOM
            copy(
                lockedCategories = lockedCategories - category,
                lockedValues = lockedValues - category,
                categoryModes = categoryModes + (category to CategorySelectionMode.RANDOM),
                selectedValues = selectedValues - category
            )
        } else {
            // Lock with current value
            copy(
                lockedCategories = lockedCategories + category,
                lockedValues = lockedValues + (category to currentValue),
                categoryModes = categoryModes + (category to CategorySelectionMode.CHOOSE),
                selectedValues = selectedValues + (category to currentValue)
            )
        }
    }

    fun clearAll(): PromptLockState {
        return PromptLockState()
    }
}

