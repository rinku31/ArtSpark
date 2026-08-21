package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ArtPrompt
import com.example.model.Difficulty
import com.example.ui.components.DailySparkCard
import com.example.ui.components.LockControlsCard
import com.example.ui.components.PromptCard
import com.example.ui.components.ShareBottomSheet
import com.example.ui.components.SparkHeader
import com.example.ui.theme.CleanBorder
import com.example.ui.theme.CleanCanvasBackground
import com.example.ui.theme.CleanInkBlack
import com.example.ui.theme.CleanMutedText
import com.example.ui.theme.CleanPillBorder
import com.example.ui.theme.CoralRed
import com.example.ui.theme.IrisPurple
import com.example.ui.theme.MintTeal
import com.example.ui.theme.SparkYellow
import com.example.ui.viewmodel.ArtSparkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    viewModel: ArtSparkViewModel,
    onOpenSettings: () -> Unit,
    onNavigateToBrainstorm: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentPrompt by viewModel.currentPrompt.collectAsStateWithLifecycle()
    val dailySpark by viewModel.dailySpark.collectAsStateWithLifecycle()
    val lockState by viewModel.lockState.collectAsStateWithLifecycle()
    val isCreativeGap by viewModel.isCreativeGapMode.collectAsStateWithLifecycle()
    val difficulty by viewModel.selectedDifficulty.collectAsStateWithLifecycle()
    val isAdvancedOpen by viewModel.isAdvancedOpen.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    var sharePromptTarget by remember { mutableStateOf<ArtPrompt?>(null) }

    fun triggerHaptic() {
        if (!prefs.hapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(25)
            }
        } catch (e: Exception) {
            // Gracefully ignore if vibrator is unavailable
        }
    }

    fun sharePrompt(prompt: ArtPrompt) {
        sharePromptTarget = prompt
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("discover_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // App Header
        item {
            SparkHeader(onOpenSettings = onOpenSettings)
        }

        // Daily Spark Card
        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                DailySparkCard(
                    dailySpark = dailySpark,
                    onLoadToWorkspace = {
                        triggerHaptic()
                        viewModel.loadPromptToWorkspace(dailySpark) {}
                    },
                    onShare = { sharePrompt(dailySpark) }
                )
            }
        }

        // Mode & Difficulty Selection Bar
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                // Mode Toggle Row (Standard vs Creative Gap)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ModeTabItem(
                        title = "Classic Spark",
                        icon = Icons.Rounded.Palette,
                        isSelected = !isCreativeGap,
                        activeColor = MaterialTheme.colorScheme.onSurface,
                        onClick = {
                            if (isCreativeGap) {
                                triggerHaptic()
                                viewModel.setCreativeGapMode(false)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    ModeTabItem(
                        title = "Creative Gap ✨",
                        icon = Icons.Rounded.AutoAwesome,
                        isSelected = isCreativeGap,
                        activeColor = MintTeal,
                        onClick = {
                            if (!isCreativeGap) {
                                triggerHaptic()
                                viewModel.setCreativeGapMode(true)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Difficulty Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Complexity:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Difficulty.values().forEach { diff ->
                        val isSelected = difficulty == diff
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                triggerHaptic()
                                viewModel.setDifficulty(diff)
                                viewModel.reroll()
                            },
                            label = {
                                Text(
                                    text = diff.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SparkYellow,
                                selectedLabelColor = Color(0xFF141413),
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) SparkYellow else MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("difficulty_${diff.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // Main Generated Prompt Card with Animated Transition
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                AnimatedContent(
                    targetState = currentPrompt,
                    transitionSpec = {
                        (slideInVertically { height -> height / 4 } + fadeIn(tween(220)))
                            .togetherWith(slideOutVertically { height -> -height / 4 } + fadeOut(tween(180)))
                    },
                    label = "prompt_animation"
                ) { promptState ->
                    PromptCard(
                        prompt = promptState,
                        lockState = lockState,
                        onToggleFavorite = {
                            triggerHaptic()
                            viewModel.toggleCurrentFavorite()
                        },
                        onShare = { sharePrompt(promptState) }
                    )
                }
            }
        }

        // Category Controls and Locks
        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                LockControlsCard(
                    prompt = currentPrompt,
                    lockState = lockState,
                    isOpen = isAdvancedOpen,
                    onToggleOpen = {
                        triggerHaptic()
                        viewModel.toggleAdvanced()
                    },
                    onToggleCategoryLock = { cat ->
                        triggerHaptic()
                        viewModel.toggleCategoryLock(cat)
                    },
                    onSetCategoryMode = { cat, mode ->
                        triggerHaptic()
                        viewModel.setCategoryMode(cat, mode)
                    },
                    onSetSelectedValue = { cat, value ->
                        triggerHaptic()
                        viewModel.setCategorySelectedValue(cat, value)
                    },
                    onSetCustomValue = { cat, value ->
                        triggerHaptic()
                        viewModel.setCategoryCustomValue(cat, value)
                    },
                    onUnlockAll = {
                        triggerHaptic()
                        viewModel.unlockAll()
                    }
                )
            }
        }

        // Primary Action Controls (Save + Reroll Split Row)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left 1x: Save button
                    OutlinedButton(
                        onClick = {
                            triggerHaptic()
                            viewModel.toggleCurrentFavorite()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("save_action_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, if (currentPrompt.isFavorite) CoralRed else MaterialTheme.colorScheme.onSurface),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (currentPrompt.isFavorite) CoralRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                            contentColor = if (currentPrompt.isFavorite) CoralRed else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (currentPrompt.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = if (currentPrompt.isFavorite) "Saved" else "Save",
                                tint = if (currentPrompt.isFavorite) CoralRed else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentPrompt.isFavorite) "Saved" else "Save",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                        }
                    }

                    // Right 2x: Reroll Spark button
                    Button(
                        onClick = {
                            triggerHaptic()
                            viewModel.reroll()
                        },
                        modifier = Modifier
                            .weight(2f)
                            .height(56.dp)
                            .testTag("reroll_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 6.dp
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Casino,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reroll",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Brainstorm This Action Button
                Surface(
                    onClick = {
                        triggerHaptic()
                        viewModel.startBrainstormWithPrompt(currentPrompt, onNavigateToBrainstorm)
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("brainstorm_this_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = IrisPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "✨ Brainstorm This Idea with AI",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Feeling stuck? Roll again or brainstorm with AI.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    sharePromptTarget?.let { promptToShare ->
        ShareBottomSheet(
            prompt = promptToShare,
            onDismiss = { sharePromptTarget = null }
        )
    }
}

@Composable
private fun ModeTabItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.outlineVariant else Color.Transparent
        ),
        modifier = modifier.height(38.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
