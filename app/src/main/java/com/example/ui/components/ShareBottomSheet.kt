package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ArtPrompt
import com.example.ui.theme.CleanBorder
import com.example.ui.theme.CleanCanvasBackground
import com.example.ui.theme.CleanCharcoalText
import com.example.ui.theme.CleanInkBlack
import com.example.ui.theme.CleanMutedText
import com.example.ui.theme.CleanSurface
import com.example.ui.theme.CoralRed
import com.example.ui.theme.DarkMinimalBackground
import com.example.ui.theme.DarkMinimalSurface
import com.example.ui.theme.IrisPurple
import com.example.ui.theme.MintTeal
import com.example.ui.theme.SparkYellow
import com.example.ui.util.ShareCardOptions
import com.example.ui.util.ShareCardRenderer
import com.example.ui.util.ShareTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    prompt: ArtPrompt,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var generatingTheme by remember { mutableStateOf<ShareTheme?>(null) }
    var copiedToClipboard by remember { mutableStateOf(false) }

    fun shareImage(theme: ShareTheme) {
        if (generatingTheme != null) return
        generatingTheme = theme
        scope.launch {
            try {
                val options = ShareCardOptions(theme = theme)
                val imageUri = ShareCardRenderer.generateShareCardUri(context, prompt, options)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    putExtra(Intent.EXTRA_TEXT, prompt.toShareText())
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Your Spark"))
                onDismiss()
            } catch (e: Exception) {
                Toast.makeText(context, "Could not generate card: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                generatingTheme = null
            }
        }
    }

    fun shareText() {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, prompt.toShareText())
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share ArtSpark Prompt"))
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState())
                .testTag("share_bottom_sheet")
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Header: Title & Subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Share Your Spark",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            letterSpacing = (-0.3).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Choose a style for your inspiration card.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Mini Prompt Preview Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                val gapSentence = prompt.gapTemplate
                if (prompt.isCreativeGap && !gapSentence.isNullOrBlank()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MintTeal)
                            )
                            Text(
                                text = "CREATIVE GAP",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp
                                ),
                                color = MintTeal
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = gapSentence,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "💡 Fill in the blank with your own twist!",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                color = MintTeal
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Idea starters for the blank:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        prompt.displayGapIdeaStarters.take(3).forEach { starter ->
                            Text(
                                text = "• $starter",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SparkYellow)
                            )
                            Text(
                                text = prompt.subjectPhrase,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (prompt.scenePhrase.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = prompt.scenePhrase,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // OPTION 1: 🌙 Cozy Night
            StyleShareCard(
                emoji = "🌙",
                title = "Cozy Night",
                description = "Midnight studio canvas with glowing stars, golden foliage, and illuminated parchment notes.",
                perfectForList = listOf("Instagram Stories", "Discord", "Dark Mode Feeds", "Messenger"),
                previewColor = Color(0xFF161513),
                previewBorderColor = Color(0xFF38352E),
                accentColor = SparkYellow,
                badge = "MIDNIGHT CANVAS",
                isLoading = generatingTheme == ShareTheme.COZY_NIGHT,
                onClick = { shareImage(ShareTheme.COZY_NIGHT) },
                testTag = "share_option_cozy_night"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // OPTION 2: ☀️ Sketchbook
            StyleShareCard(
                emoji = "☀️",
                title = "Sketchbook",
                description = "Warm artist desk with watercolor swatches, washi tape, and paperclipped encouragement.",
                perfectForList = listOf("Pinterest", "Instagram", "Notion", "Print & Journal"),
                previewColor = Color(0xFFFFFDF7),
                previewBorderColor = Color(0xFFEADBCE),
                accentColor = Color(0xFFD97706),
                badge = "ARTIST JOURNAL",
                isLoading = generatingTheme == ShareTheme.SKETCHBOOK,
                onClick = { shareImage(ShareTheme.SKETCHBOOK) },
                testTag = "share_option_sketchbook"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // OPTION 3: 📄 Text Only
            StyleShareCard(
                emoji = "📄",
                title = "Text Only",
                description = "Share only the prompt as text.",
                perfectForList = listOf("Clipboard", "Notes", "Quick Message"),
                previewColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                previewBorderColor = MaterialTheme.colorScheme.outlineVariant,
                accentColor = MintTeal,
                badge = "PLAIN TEXT",
                isLoading = false,
                onClick = { shareText() },
                testTag = "share_option_text_only"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Copy Prompt to Clipboard
            Surface(
                onClick = {
                    clipboardManager.setText(AnnotatedString(prompt.toShareText()))
                    copiedToClipboard = true
                    Toast.makeText(context, "Prompt copied to clipboard!", Toast.LENGTH_SHORT).show()
                    scope.launch {
                        kotlinx.coroutines.delay(1500)
                        copiedToClipboard = false
                    }
                },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("copy_prompt_action")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (copiedToClipboard) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        tint = if (copiedToClipboard) MintTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (copiedToClipboard) "Copied to Clipboard!" else "Copy Prompt Text Only",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StyleShareCard(
    emoji: String,
    title: String,
    description: String,
    perfectForList: List<String>,
    previewColor: Color,
    previewBorderColor: Color,
    accentColor: Color,
    badge: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Preview Swatch Box
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(previewColor)
                            .background(BorderStroke(1.dp, previewBorderColor).let { Color.Transparent })
                            .clip(RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = accentColor
                            )
                        } else {
                            Text(text = emoji, fontSize = 18.sp)
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Surface(
                                shape = CircleShape,
                                color = accentColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = badge,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.8.sp
                                    ),
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // "Perfect for:" section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Perfect for: ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Text(
                    text = perfectForList.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            }
        }
    }
}
