package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.generator.PromptData
import com.example.model.ArtPrompt
import com.example.model.PromptLockState
import com.example.ui.theme.CleanBorder
import com.example.ui.theme.CleanDivider
import com.example.ui.theme.CleanInkBlack
import com.example.ui.theme.CleanStoneGray
import com.example.ui.theme.CoralRed
import com.example.ui.theme.IrisPurple
import com.example.ui.theme.MintTeal
import com.example.ui.theme.SparkYellow

@Composable
fun PromptCard(
    prompt: ArtPrompt,
    lockState: PromptLockState,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var showGapInspiration by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .testTag("prompt_card"),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            ),
            border = BorderStroke(1.dp, CleanBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                // Top Row: Section Tag & Locked Status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (prompt.isCreativeGap) "CREATIVE GAP PROMPT" else "YOUR PROMPT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (prompt.customCategories.isNotEmpty()) {
                            Surface(
                                color = MintTeal.copy(alpha = 0.15f),
                                shape = CircleShape
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Customized",
                                        tint = MintTeal,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${prompt.customCategories.size} custom",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MintTeal
                                    )
                                }
                            }
                        }

                        if (lockState.lockedCategories.isNotEmpty()) {
                            Surface(
                                color = CoralRed.copy(alpha = 0.15f),
                                shape = CircleShape
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = "Locks active",
                                        tint = CoralRed,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${lockState.lockedCategories.size} locked",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = CoralRed
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(prompt.narrativeText))
                            },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("copy_prompt_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy prompt",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = onShare,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("share_prompt_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Share prompt",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Main Prompt Text
                if (prompt.isCreativeGap && !prompt.gapTemplate.isNullOrBlank()) {
                    CreativeGapContent(
                        template = prompt.gapTemplate,
                        showInspiration = showGapInspiration,
                        onToggleInspiration = { showGapInspiration = !showGapInspiration }
                    )
                } else {
                    CleanMinimalPromptContent(prompt = prompt)
                }

                // Divider line
                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Challenge & Style Block
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (prompt.challenge.isNotBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "CHALLENGE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(SparkYellow)
                                )
                                Text(
                                    text = prompt.challenge,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (prompt.style.isNotBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "MEDIUM / STYLE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(IrisPurple)
                                )
                                Text(
                                    text = prompt.style,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = IrisPurple
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Difficulty Badge at top-left matching -top-3 left-8
        Surface(
            shape = CircleShape,
            color = MintTeal,
            shadowElevation = 2.dp,
            modifier = Modifier
                .offset(x = 24.dp, y = 0.dp)
        ) {
            Text(
                text = if (prompt.isCreativeGap) "GAP" else prompt.difficulty.label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                ),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun CleanMinimalPromptContent(prompt: ArtPrompt) {
    val promptText = buildAnnotatedString {
        append("A ")

        // Subject + Trait in Bold Coral Red (#FF6B6B)
        withStyle(
            SpanStyle(
                color = CoralRed,
                fontWeight = FontWeight.Black
            )
        ) {
            val traitSubj = buildString {
                if (prompt.trait.isNotBlank()) {
                    append(prompt.trait.lowercase())
                    append(" ")
                }
                append(prompt.subject.lowercase())
            }
            append(traitSubj)
        }

        // Action and Environment
        if (prompt.action.isNotBlank() || prompt.environment.isNotBlank()) {
            append(" ")
            if (prompt.action.isNotBlank()) {
                append(prompt.action.lowercase())
                append(" ")
            }
            if (prompt.environment.isNotBlank()) {
                withStyle(
                    SpanStyle(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium
                    )
                ) {
                    append(prompt.environment.lowercase())
                }
            }
        }

        // Atmosphere with Teal underline (#4ECDC4)
        if (prompt.atmosphere.isNotBlank()) {
            append(" during ")
            withStyle(
                SpanStyle(
                    color = MintTeal,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(prompt.atmosphere.lowercase())
            }
        }

        // Style in Bold Purple (#6C5CE7)
        if (prompt.style.isNotBlank()) {
            append(" in a ")
            withStyle(
                SpanStyle(
                    color = IrisPurple,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(prompt.style.lowercase())
            }
        }

        append(".")
    }

    Text(
        text = promptText,
        style = MaterialTheme.typography.headlineMedium.copy(
            fontSize = 21.sp,
            lineHeight = 29.sp,
            fontWeight = FontWeight.Normal
        ),
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun CreativeGapContent(
    template: String,
    showInspiration: Boolean,
    onToggleInspiration: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val parts = template.split("______")

        val annotatedString = buildAnnotatedString {
            if (parts.size >= 2) {
                append(parts[0])
                withStyle(
                    SpanStyle(
                        color = MintTeal,
                        fontWeight = FontWeight.Black,
                        background = MintTeal.copy(alpha = 0.15f)
                    )
                ) {
                    append(" ______ ")
                }
                append(parts[1])
            } else {
                append(template)
            }
        }

        Text(
            text = annotatedString,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 29.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MintTeal.copy(alpha = 0.08f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lightbulb,
                    contentDescription = null,
                    tint = MintTeal,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Fill in the blank with your own twist!",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MintTeal
                )
            }

            Surface(
                onClick = onToggleInspiration,
                shape = RoundedCornerShape(6.dp),
                color = MintTeal.copy(alpha = 0.18f)
            ) {
                Text(
                    text = if (showInspiration) "Hide Ideas" else "Spark Ideas",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MintTeal,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }

        if (showInspiration) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Idea starters for the blank:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val suggestions = remember { PromptData.gapInspirationIdeas.shuffled().take(3) }
                suggestions.forEach { idea ->
                    Text(
                        text = "• $idea",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontStyle = FontStyle.Italic
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
