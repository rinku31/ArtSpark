package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.generator.PromptData
import com.example.model.ArtPrompt
import com.example.model.CategorySelectionMode
import com.example.model.PromptCategory
import com.example.model.PromptLockState
import com.example.model.getCategoryValue
import com.example.ui.theme.CoralRed
import com.example.ui.theme.IrisPurple
import com.example.ui.theme.MintTeal
import com.example.ui.theme.SparkYellow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LockControlsCard(
    prompt: ArtPrompt,
    lockState: PromptLockState,
    isOpen: Boolean,
    onToggleOpen: () -> Unit,
    onToggleCategoryLock: (PromptCategory) -> Unit,
    onSetCategoryMode: (PromptCategory, CategorySelectionMode) -> Unit = { _, _ -> },
    onSetSelectedValue: (PromptCategory, String) -> Unit = { _, _ -> },
    onSetCustomValue: (PromptCategory, String) -> Unit = { _, _ -> },
    onUnlockAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mainCategories = listOf(
        PromptCategory.SUBJECT,
        PromptCategory.TRAIT,
        PromptCategory.ENVIRONMENT,
        PromptCategory.ATMOSPHERE,
        PromptCategory.STYLE,
        PromptCategory.CHALLENGE
    )

    val activeLocksCount = lockState.lockedCategories.size

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Quick Category Status Pills Horizontal Wrap Bar
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            mainCategories.forEach { category ->
                val mode = lockState.getMode(category)
                val isCustom = lockState.isCustom(category)
                val isLocked = lockState.isLocked(category)

                val categoryShortName = when (category) {
                    PromptCategory.TRAIT -> "Trait"
                    PromptCategory.SUBJECT -> "Subject"
                    PromptCategory.ACTION -> "Action"
                    PromptCategory.ENVIRONMENT -> "Environment"
                    PromptCategory.ATMOSPHERE -> "Atmosphere"
                    PromptCategory.STYLE -> "Style"
                    PromptCategory.CHALLENGE -> "Challenge"
                }

                val pillBorderColor = when {
                    isCustom -> MintTeal
                    isLocked -> CoralRed
                    else -> MaterialTheme.colorScheme.outline
                }

                val pillBadgeBg = when {
                    isCustom -> MintTeal
                    isLocked -> CoralRed
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }

                val pillIcon = when {
                    isCustom -> Icons.Rounded.Edit
                    isLocked -> Icons.Rounded.Lock
                    else -> Icons.Rounded.Casino
                }

                val pillIconTint = when {
                    isCustom || isLocked -> Color.White
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    onClick = { onToggleCategoryLock(category) },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, pillBorderColor),
                    shadowElevation = 1.dp,
                    modifier = Modifier.testTag("lock_pill_${category.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = categoryShortName.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(pillBadgeBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = pillIcon,
                                contentDescription = if (isLocked) "Fixed" else "Random",
                                tint = pillIconTint,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
        }

        // Expandable Detail Drawer for Category Configuration
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("lock_controls_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Toggle Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleOpen() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (activeLocksCount > 0) CoralRed.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (activeLocksCount > 0) Icons.Rounded.Tune else Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = if (activeLocksCount > 0) CoralRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Category Controls & Locks",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (activeLocksCount == 0) "Customize Subject, Style, Trait & more"
                                else "$activeLocksCount categories fixed during rerolls",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp
                                ),
                                color = if (activeLocksCount > 0) CoralRed
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (activeLocksCount > 0) {
                            TextButton(
                                onClick = onUnlockAll,
                                modifier = Modifier.testTag("unlock_all_button")
                            ) {
                                Text(
                                    text = "Reset All",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = CoralRed
                                )
                            }
                        }

                        IconButton(
                            onClick = onToggleOpen,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (isOpen) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Expandable Category List
                AnimatedVisibility(
                    visible = isOpen,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val allCategories = listOf(
                            PromptCategory.SUBJECT,
                            PromptCategory.TRAIT,
                            PromptCategory.ENVIRONMENT,
                            PromptCategory.ATMOSPHERE,
                            PromptCategory.STYLE,
                            PromptCategory.CHALLENGE,
                            PromptCategory.ACTION
                        )

                        allCategories.forEach { category ->
                            CategoryEditorCard(
                                category = category,
                                prompt = prompt,
                                lockState = lockState,
                                onSetMode = { mode -> onSetCategoryMode(category, mode) },
                                onSelectPreset = { value -> onSetSelectedValue(category, value) },
                                onUpdateCustom = { value -> onSetCustomValue(category, value) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryEditorCard(
    category: PromptCategory,
    prompt: ArtPrompt,
    lockState: PromptLockState,
    onSetMode: (CategorySelectionMode) -> Unit,
    onSelectPreset: (String) -> Unit,
    onUpdateCustom: (String) -> Unit
) {
    val mode = lockState.getMode(category)
    val customValue = lockState.getCustomValue(category)
    val selectedValue = lockState.getSelectedValue(category).ifBlank { prompt.getCategoryValue(category) }
    var isDropdownOpen by remember { mutableStateOf(false) }

    val categoryIcon: ImageVector = when (category) {
        PromptCategory.TRAIT -> Icons.Rounded.Psychology
        PromptCategory.SUBJECT -> Icons.Rounded.Face
        PromptCategory.ACTION -> Icons.Rounded.DirectionsRun
        PromptCategory.ENVIRONMENT -> Icons.Rounded.Landscape
        PromptCategory.ATMOSPHERE -> Icons.Rounded.Cloud
        PromptCategory.STYLE -> Icons.Rounded.Palette
        PromptCategory.CHALLENGE -> Icons.Rounded.Timer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("category_editor_${category.name.lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            1.dp,
            when (mode) {
                CategorySelectionMode.CUSTOM -> MintTeal.copy(alpha = 0.5f)
                CategorySelectionMode.CHOOSE -> CoralRed.copy(alpha = 0.5f)
                CategorySelectionMode.RANDOM -> MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Category Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = when (mode) {
                            CategorySelectionMode.CUSTOM -> MintTeal
                            CategorySelectionMode.CHOOSE -> CoralRed
                            CategorySelectionMode.RANDOM -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = category.title.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Mode Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (mode) {
                        CategorySelectionMode.CUSTOM -> MintTeal.copy(alpha = 0.15f)
                        CategorySelectionMode.CHOOSE -> CoralRed.copy(alpha = 0.15f)
                        CategorySelectionMode.RANDOM -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = when (mode) {
                            CategorySelectionMode.CUSTOM -> "✏️ CUSTOM"
                            CategorySelectionMode.CHOOSE -> "🔒 FIXED"
                            CategorySelectionMode.RANDOM -> "🎲 RANDOM"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = when (mode) {
                            CategorySelectionMode.CUSTOM -> MintTeal
                            CategorySelectionMode.CHOOSE -> CoralRed
                            CategorySelectionMode.RANDOM -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Mode Selector Tabs: [ Random | Choose | Custom ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CategorySelectionMode.values().forEach { m ->
                    val isSelected = mode == m
                    Surface(
                        onClick = { onSetMode(m) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) {
                            when (m) {
                                CategorySelectionMode.RANDOM -> SparkYellow
                                CategorySelectionMode.CHOOSE -> CoralRed
                                CategorySelectionMode.CUSTOM -> MintTeal
                            }
                        } else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .testTag("mode_${category.name.lowercase()}_${m.name.lowercase()}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = when (m) {
                                    CategorySelectionMode.RANDOM -> "🎲 Random"
                                    CategorySelectionMode.CHOOSE -> "📋 Choose"
                                    CategorySelectionMode.CUSTOM -> "✏️ Custom"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) {
                                    when (m) {
                                        CategorySelectionMode.RANDOM -> Color(0xFF1A1A1A)
                                        CategorySelectionMode.CHOOSE -> Color.White
                                        CategorySelectionMode.CUSTOM -> Color(0xFF141413)
                                    }
                                } else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Dynamic Content for Selected Mode
            when (mode) {
                CategorySelectionMode.RANDOM -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Rerolls randomly on every tap",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontStyle = FontStyle.Italic
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val currentVal = prompt.getCategoryValue(category)
                        if (currentVal.isNotBlank()) {
                            Text(
                                text = "Current: $currentVal",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                CategorySelectionMode.CHOOSE -> {
                    Surface(
                        onClick = { isDropdownOpen = true },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, CoralRed.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dropdown_${category.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedValue.ifBlank { "Select an option..." },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Rounded.ExpandMore,
                                contentDescription = "Open selection",
                                tint = CoralRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                CategorySelectionMode.CUSTOM -> {
                    val focusManager = LocalFocusManager.current
                    OutlinedTextField(
                        value = customValue,
                        onValueChange = { onUpdateCustom(it) },
                        placeholder = {
                            Text(
                                text = "Type your own ${category.title.lowercase()}...",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (customValue.isNotBlank()) {
                                IconButton(
                                    onClick = { onUpdateCustom("") },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MintTeal,
                            unfocusedBorderColor = MintTeal.copy(alpha = 0.5f),
                            cursorColor = MintTeal
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_input_${category.name.lowercase()}")
                    )
                }
            }
        }
    }

    // Modal Preset Selector Dialog with Search
    if (isDropdownOpen) {
        PresetOptionDialog(
            category = category,
            currentValue = selectedValue,
            options = PromptData.getOptionsForCategory(category),
            onSelect = { option ->
                onSelectPreset(option)
                isDropdownOpen = false
            },
            onDismiss = { isDropdownOpen = false }
        )
    }
}

@Composable
private fun PresetOptionDialog(
    category: PromptCategory,
    currentValue: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredOptions = remember(searchQuery, options) {
        if (searchQuery.isBlank()) options
        else options.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .testTag("preset_dialog_${category.name.lowercase()}")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Dialog Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Choose ${category.title}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${options.size} options available",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search presets...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = CoralRed,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                // Options List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredOptions) { option ->
                        val isSelected = option.equals(currentValue, ignoreCase = true)
                        Surface(
                            onClick = { onSelect(option) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) CoralRed.copy(alpha = 0.12f) else Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) CoralRed.copy(alpha = 0.4f) else Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("preset_option_$option")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    ),
                                    color = if (isSelected) CoralRed else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = "Selected",
                                        tint = CoralRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
