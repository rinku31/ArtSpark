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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ArtPrompt
import com.example.model.PromptCategory
import com.example.model.PromptLockState
import com.example.ui.theme.CleanBorder
import com.example.ui.theme.CleanInkBlack
import com.example.ui.theme.CleanPillBorder
import com.example.ui.theme.CleanStoneGray
import com.example.ui.theme.CoralRed
import com.example.ui.theme.SparkYellow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LockControlsCard(
    prompt: ArtPrompt,
    lockState: PromptLockState,
    isOpen: Boolean,
    onToggleOpen: () -> Unit,
    onToggleCategoryLock: (PromptCategory) -> Unit,
    onUnlockAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        PromptCategory.SUBJECT,
        PromptCategory.ENVIRONMENT,
        PromptCategory.ATMOSPHERE,
        PromptCategory.STYLE,
        PromptCategory.CHALLENGE
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Quick Lock Pills Horizontal Wrap Bar (Directly from Design Spec)
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                val isLocked = lockState.isLocked(category)
                val categoryName = when (category) {
                    PromptCategory.TRAIT -> "Trait"
                    PromptCategory.SUBJECT -> "Subject"
                    PromptCategory.ACTION -> "Action"
                    PromptCategory.ENVIRONMENT -> "Environment"
                    PromptCategory.ATMOSPHERE -> "Atmosphere"
                    PromptCategory.STYLE -> "Style"
                    PromptCategory.CHALLENGE -> "Challenge"
                }

                Surface(
                    onClick = { onToggleCategoryLock(category) },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, CleanPillBorder),
                    shadowElevation = 1.dp,
                    modifier = Modifier.testTag("lock_pill_${category.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = categoryName.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = CleanInkBlack
                        )
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isLocked) CoralRed else CleanPillBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.Casino,
                                contentDescription = if (isLocked) "Locked" else "Random",
                                tint = if (isLocked) Color.White else CleanInkBlack,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
        }

        // Expandable Detail Drawer for Fine-tuning / Values preview
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("lock_controls_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, CleanBorder)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Toggle Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleOpen() }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (lockState.lockedCategories.isNotEmpty()) CoralRed.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (lockState.lockedCategories.isNotEmpty()) Icons.Rounded.Lock else Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = if (lockState.lockedCategories.isNotEmpty()) CoralRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Element Lock Details",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (lockState.lockedCategories.isEmpty()) "Tap any pill above to keep elements"
                                else "${lockState.lockedCategories.size} elements kept during rerolls",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp
                                ),
                                color = if (lockState.lockedCategories.isNotEmpty()) CoralRed
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (lockState.lockedCategories.isNotEmpty()) {
                            TextButton(
                                onClick = onUnlockAll,
                                modifier = Modifier.testTag("unlock_all_button")
                            ) {
                                Text(
                                    text = "Unlock All",
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
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (isOpen) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Expandable List of Categories with Detailed Text Preview
                AnimatedVisibility(
                    visible = isOpen,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val allCategories = listOf(
                            PromptCategory.TRAIT,
                            PromptCategory.SUBJECT,
                            PromptCategory.ACTION,
                            PromptCategory.ENVIRONMENT,
                            PromptCategory.ATMOSPHERE,
                            PromptCategory.STYLE,
                            PromptCategory.CHALLENGE
                        )

                        allCategories.forEach { category ->
                            val isLocked = lockState.isLocked(category)
                            val value = prompt.getCategoryValue(category)

                            if (value.isNotBlank()) {
                                CategoryLockRow(
                                    category = category,
                                    value = value,
                                    isLocked = isLocked,
                                    onToggle = { onToggleCategoryLock(category) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryLockRow(
    category: PromptCategory,
    value: String,
    isLocked: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(12.dp),
        color = if (isLocked) CoralRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            if (isLocked) CoralRed.copy(alpha = 0.3f) else CleanBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lock_row_${category.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = if (isLocked) CoralRed else CleanStoneGray
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isLocked) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isLocked) CoralRed else CleanPillBorder
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.Casino,
                        contentDescription = if (isLocked) "Locked" else "Random",
                        tint = if (isLocked) Color.White else CleanInkBlack,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isLocked) "Locked" else "Random",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isLocked) Color.White else CleanInkBlack
                    )
                }
            }
        }
    }
}
