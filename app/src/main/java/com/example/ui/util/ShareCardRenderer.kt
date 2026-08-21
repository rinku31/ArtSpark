package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.generator.PromptSentenceBuilder
import com.example.model.ArtPrompt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Themes for the exported share card image.
 */
enum class ShareTheme {
    COZY_NIGHT,
    SKETCHBOOK
}

/**
 * Configuration options for rendering high-resolution share cards.
 */
data class ShareCardOptions(
    val theme: ShareTheme = ShareTheme.COZY_NIGHT,
    val inspirationalQuote: String? = null,
    val artistName: String? = null,
    val promptNumber: Long? = null,
    val showDailySparkBadge: Boolean = true,
    val showQrCodePlaceholder: Boolean = false
)

object ShareCardRenderer {

    const val WIDTH = 1080
    const val DEFAULT_HEIGHT = 1480

    private val INSPIRATIONAL_QUOTES = listOf(
        "✨ Every artist imagines this differently. ✨",
        "🎨 Your creativity completes the story. 🎨",
        "🌱 Every masterpiece starts with a spark. 🌱",
        "✏️ Where will this idea take you? ✏️",
        "💡 One prompt. Infinite interpretations. 💡"
    )

    suspend fun generateShareCardUri(
        context: Context,
        prompt: ArtPrompt,
        options: ShareCardOptions = ShareCardOptions()
    ): Uri = withContext(Dispatchers.IO) {
        val calculatedHeight = calculateDynamicHeight(prompt, options)
        val bitmap = Bitmap.createBitmap(WIDTH, calculatedHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        renderCardOnCanvas(canvas, prompt, options, WIDTH.toFloat(), calculatedHeight.toFloat())

        val cacheDir = File(context.cacheDir, "shared_sparks").apply { mkdirs() }
        val prefix = if (options.theme == ShareTheme.SKETCHBOOK) "sketchbook" else "cozynight"
        val shareFile = File(cacheDir, "artspark_${prefix}_${prompt.id}_${System.currentTimeMillis()}.png")
        FileOutputStream(shareFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            shareFile
        )
    }

    fun calculateDynamicHeight(
        prompt: ArtPrompt,
        options: ShareCardOptions = ShareCardOptions()
    ): Int {
        val width = WIDTH.toFloat()
        val cardLeft = 48f
        val cardRight = width - 48f
        val cardWidth = cardRight - cardLeft
        val contentWidth = cardWidth - 72f
        val colWidth = (contentWidth - 36f) / 2f

        var cardInnerHeight = 120f // Top padding + banner offset

        // Difficulty row
        cardInnerHeight += 44f + 16f

        if (prompt.isCreativeGap) {
            // 1. Creative Gap Full Sentence Box
            val gapSentence = prompt.narrativeText
            val sentencePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val sentenceLayout = createStaticLayout(gapSentence, sentencePaint, (contentWidth - 100f).toInt(), maxLines = 6)
            val sentenceBoxHeight = (36f + sentenceLayout.height + 36f).coerceAtLeast(100f)
            cardInnerHeight += sentenceBoxHeight + 24f

            // 2. Lightbulb Twist Banner ("Fill in the blank with your own twist!")
            cardInnerHeight += 58f + 20f

            // 3. Idea Starters Container
            var startersContentHeight = 36f // header
            val starters = prompt.displayGapIdeaStarters.take(3)
            val starterPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            }
            starters.forEach { item ->
                val layout = createStaticLayout("• $item", starterPaint, (contentWidth - 64f).toInt(), maxLines = 3)
                startersContentHeight += layout.height + 14f
            }
            val startersBoxHeight = (28f + startersContentHeight + 24f).coerceAtLeast(130f)
            cardInnerHeight += startersBoxHeight + 24f

            // 4. Style & Challenge tags (if present)
            if (prompt.stylePhrase.isNotBlank() || prompt.challengePhrase.isNotBlank()) {
                cardInnerHeight += 80f + 20f
            }

            // 5. Encouragement Sticky Note
            cardInnerHeight += 150f + 20f

            // Card footer (Created with ArtSpark + Spark pill)
            cardInnerHeight += 110f

            val headerHeight = 130f
            val bottomMargin = 40f
            val totalNeeded = headerHeight + cardInnerHeight + bottomMargin

            return totalNeeded.toInt().coerceAtLeast(DEFAULT_HEIGHT)
        }

        // Subject height
        val subjectPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subjectLayout = createStaticLayout(prompt.subjectPhrase, subjectPaint, contentWidth.toInt())
        cardInnerHeight += 26f + subjectLayout.height + 28f + 20f

        // Left Column height: Scene + Atmosphere + Story Hook Note
        var leftColHeight = 0f
        if (prompt.scenePhrase.isNotBlank()) {
            val scenePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 22f }
            val sceneLayout = createStaticLayout(prompt.scenePhrase, scenePaint, colWidth.toInt())
            leftColHeight += 24f + sceneLayout.height + 24f
        }
        if (prompt.atmospherePhrase.isNotBlank()) {
            val atmPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 22f }
            val atmLayout = createStaticLayout(prompt.atmospherePhrase, atmPaint, colWidth.toInt())
            leftColHeight += 24f + atmLayout.height + 24f
        }
        if (prompt.displayStoryHook.isNotBlank()) {
            val hookPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            }
            val hookLayout = createStaticLayout(prompt.displayStoryHook, hookPaint, (colWidth - 40f).toInt(), maxLines = 5)
            leftColHeight += 40f + hookLayout.height + 40f + 20f
        }

        // Right Column height: Style + Challenge + Paperclip encouragement box
        var rightColHeight = 0f
        if (prompt.stylePhrase.isNotBlank()) {
            val stylePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val styleLayout = createStaticLayout(prompt.stylePhrase, stylePaint, colWidth.toInt())
            rightColHeight += 24f + styleLayout.height + 34f + 24f
        }
        if (prompt.challengePhrase.isNotBlank()) {
            val chalPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 22f }
            val chalLayout = createStaticLayout(prompt.challengePhrase, chalPaint, colWidth.toInt())
            rightColHeight += 24f + chalLayout.height + 34f + 24f
        }
        // Encouragement note box height
        rightColHeight += 150f + 20f

        cardInnerHeight += maxOf(leftColHeight, rightColHeight) + 24f

        // Full sentence quote box
        val polishedSentence = PromptSentenceBuilder.polishGrammar(prompt.narrativeText)
        if (polishedSentence.isNotBlank()) {
            val quotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 21f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            }
            val quoteLayout = createStaticLayout(polishedSentence, quotePaint, (contentWidth - 96f).toInt(), maxLines = 6)
            cardInnerHeight += 36f + quoteLayout.height + 36f + 30f
        }

        // Card footer (Created with ArtSpark + Spark pill)
        cardInnerHeight += 110f

        val headerHeight = 130f
        val bottomMargin = 40f
        val totalNeeded = headerHeight + cardInnerHeight + bottomMargin

        return totalNeeded.toInt().coerceAtLeast(DEFAULT_HEIGHT)
    }

    fun renderCardOnCanvas(
        canvas: Canvas,
        prompt: ArtPrompt,
        options: ShareCardOptions = ShareCardOptions(),
        width: Float = WIDTH.toFloat(),
        height: Float = DEFAULT_HEIGHT.toFloat()
    ) {
        val isSketchbook = options.theme == ShareTheme.SKETCHBOOK
        val footerQuote = options.inspirationalQuote
            ?: INSPIRATIONAL_QUOTES[Random.nextInt(INSPIRATIONAL_QUOTES.size)]
        val polishedSentence = PromptSentenceBuilder.polishGrammar(prompt.narrativeText)

        // -------------------------------------------------------------
        // 1. OUTER DESK / STUDIO BACKGROUND
        // -------------------------------------------------------------
        if (isSketchbook) {
            // Warm artist desk paper gradient (#F4EFE6 to #ECE4D6)
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, 0f, height,
                    intArrayOf(
                        Color.parseColor("#F6F2EB"),
                        Color.parseColor("#F2ECE0"),
                        Color.parseColor("#EAE3D4")
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width, height, bgPaint)

            // Warm subtle radial highlights
            val warmGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    width * 0.85f, 90f, 320f,
                    Color.parseColor("#22D97706"),
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(width * 0.85f, 90f, 320f, warmGlow)

            // Botanical foliage branches in corners (Warm golden sepia)
            val leafColor = Color.parseColor("#44B45309")
            drawBotanicalBranch(canvas, 68f, 72f, scale = 1.0f, rotation = 15f, color = leafColor)
            drawBotanicalBranch(canvas, width - 68f, 72f, scale = 1.0f, rotation = -15f, color = leafColor, flipX = true)
            drawBotanicalBranch(canvas, 50f, height - 70f, scale = 0.9f, rotation = 160f, color = leafColor)
            drawBotanicalBranch(canvas, width - 50f, height - 70f, scale = 0.9f, rotation = -160f, color = leafColor, flipX = true)

            // Floating golden star sparks
            drawStarSpark(canvas, 80f, 90f, 14f, Color.parseColor("#66D97706"))
            drawStarSpark(canvas, width - 90f, 85f, 16f, Color.parseColor("#66D97706"))
            drawStarSpark(canvas, 320f, 48f, 18f, Color.parseColor("#D97706"))
            drawStarSpark(canvas, 368f, 62f, 12f, Color.parseColor("#D97706"))
            drawStarSpark(canvas, 940f, 110f, 14f, Color.parseColor("#66D97706"))
        } else {
            // Cozy Night Dark Obsidian Studio (#0E0E0D to #161614)
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, 0f, height,
                    intArrayOf(
                        Color.parseColor("#0C0C0B"),
                        Color.parseColor("#141412"),
                        Color.parseColor("#0F0F0E")
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width, height, bgPaint)

            // Luminous golden and indigo glows
            val goldGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    width * 0.85f, 80f, 360f,
                    Color.parseColor("#35FFE566"),
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(width * 0.85f, 80f, 360f, goldGlow)

            // Glowing golden botanical foliage in corners
            val leafColor = Color.parseColor("#88F59E0B")
            drawBotanicalBranch(canvas, 68f, 72f, scale = 1.05f, rotation = 15f, color = leafColor)
            drawBotanicalBranch(canvas, width - 68f, 72f, scale = 1.05f, rotation = -15f, color = leafColor, flipX = true)
            drawBotanicalBranch(canvas, 50f, height - 70f, scale = 0.95f, rotation = 160f, color = leafColor)
            drawBotanicalBranch(canvas, width - 50f, height - 70f, scale = 0.95f, rotation = -160f, color = leafColor, flipX = true)

            // Glowing diamond star sparks around header
            drawStarSpark(canvas, 160f, 68f, 14f, Color.parseColor("#CCF59E0B"))
            drawStarSpark(canvas, 332f, 44f, 20f, Color.parseColor("#FFFBEB"))
            drawStarSpark(canvas, 376f, 62f, 14f, Color.parseColor("#F59E0B"))
            drawStarSpark(canvas, width - 130f, 48f, 19f, Color.parseColor("#F59E0B"))
            drawStarSpark(canvas, width - 160f, 86f, 12f, Color.parseColor("#CCF59E0B"))
            drawStarSpark(canvas, width - 40f, 110f, 10f, Color.parseColor("#99F59E0B"))
        }

        // -------------------------------------------------------------
        // 2. HEADER: "ARTSPARK" & CATCHPHRASE
        // -------------------------------------------------------------
        var currentY = 40f
        val brandColor = if (isSketchbook) Color.parseColor("#1F1E1B") else Color.WHITE
        val taglineColor = if (isSketchbook) Color.parseColor("#6E6B65") else Color.parseColor("#D4D4CE")

        // Draw spark logo beside ARTSPARK
        val logoSparkColor = if (isSketchbook) Color.parseColor("#D97706") else Color.parseColor("#FBBF24")
        drawStarSpark(canvas, width / 2f - 145f, currentY + 18f, 19f, logoSparkColor)
        drawStarSpark(canvas, width / 2f - 120f, currentY + 32f, 13f, logoSparkColor)

        // Brand Name "ARTSPARK"
        val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = brandColor
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.12f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ARTSPARK", width / 2f + 16f, currentY + 30f, brandPaint)
        currentY += 46f

        // Catchphrase: "Break the block. Make something."
        val taglinePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = taglineColor
            textSize = 19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Break the block. Make something.", width / 2f, currentY + 16f, taglinePaint)
        currentY += 42f

        // -------------------------------------------------------------
        // 3. MAIN PARCHMENT JOURNAL CARD
        // -------------------------------------------------------------
        val cardLeft = 44f
        val cardRight = width - 44f
        val cardTop = currentY
        val cardBottom = height - 32f
        val cardWidth = cardRight - cardLeft
        val cardHeight = cardBottom - cardTop
        val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)

        val cardBgColor = if (isSketchbook) Color.parseColor("#FFFDF7") else Color.parseColor("#1D1C19")
        val cardBorderColor = if (isSketchbook) Color.parseColor("#EADBCE") else Color.parseColor("#3D3B34")

        // Draw Card Shadow & Background Sheet
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isSketchbook) Color.parseColor("#20000000") else Color.parseColor("#60000000")
        }
        val shadowRect = RectF(cardLeft + 2f, cardTop + 6f, cardRight + 2f, cardBottom + 6f)
        canvas.drawRoundRect(shadowRect, 44f, 44f, shadowPaint)

        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBgColor }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.2f
            color = cardBorderColor
        }
        canvas.drawRoundRect(cardRect, 44f, 44f, cardBgPaint)
        canvas.drawRoundRect(cardRect, 44f, 44f, cardBorderPaint)

        // Subtle inner dashed stitch border around card
        val stitchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.4f
            color = if (isSketchbook) Color.parseColor("#DED4C5") else Color.parseColor("#33312B")
            pathEffect = DashPathEffect(floatArrayOf(7f, 6f), 0f)
        }
        val innerStitchRect = RectF(cardLeft + 12f, cardTop + 12f, cardRight - 12f, cardBottom - 12f)
        canvas.drawRoundRect(innerStitchRect, 36f, 36f, stitchPaint)

        // Inside card content coordinates
        val contentLeft = cardLeft + 36f
        val contentWidth = cardWidth - 72f
        val colWidth = (contentWidth - 36f) / 2f
        val leftColX = contentLeft
        val rightColX = contentLeft + colWidth + 36f

        // -------------------------------------------------------------
        // 4. TOP WATERCOLOR RIBBON BANNER: "✨ TODAY'S SPARK ✨"
        // -------------------------------------------------------------
        val bannerText = when {
            prompt.isDailySpark -> "✨ TODAY'S SPARK ✨"
            prompt.isCreativeGap -> "✨ CREATIVE GAP SPARK ✨"
            else -> "✨ TODAY'S SPARK ✨"
        }
        val bannerY = cardTop + 24f
        val bannerWidth = 360f
        val bannerHeight = 46f
        val bannerLeft = width / 2f - (bannerWidth / 2f)

        // Draw watercolor brush stroke ribbon banner
        val bannerBgColor = if (isSketchbook) Color.parseColor("#FDE68A") else Color.parseColor("#F59E0B")
        drawWatercolorBrushStroke(
            canvas = canvas,
            x = bannerLeft,
            y = bannerY,
            w = bannerWidth,
            h = bannerHeight,
            color = bannerBgColor,
            alpha = if (isSketchbook) 0.85f else 0.80f
        )

        val bannerTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isSketchbook) Color.parseColor("#78350F") else Color.parseColor("#1A1408")
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(bannerText, width / 2f, bannerY + 31f, bannerTextPaint)

        var cardCurrentY = cardTop + 84f

        // -------------------------------------------------------------
        // 5. DIFFICULTY ROW: 🍃 DIFFICULTY  [ MEDIUM ]  - - - - - - - -
        // -------------------------------------------------------------
        val diffTagColor = if (isSketchbook) Color.parseColor("#DC2626") else Color.parseColor("#F87171")
        drawLeafIcon(canvas, contentLeft + 10f, cardCurrentY + 12f, 15f, diffTagColor)

        val diffLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = diffTagColor
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.12f
        }
        canvas.drawText("DIFFICULTY", contentLeft + 26f, cardCurrentY + 18f, diffLabelPaint)

        // Dashed difficulty pill
        val diffValueText = when {
            prompt.isDailySpark -> "DAILY SPARK"
            prompt.isCreativeGap -> "CREATIVE GAP"
            else -> prompt.difficulty.label.uppercase(Locale.ROOT)
        }
        val diffPillX = contentLeft + 152f
        val diffPillPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isSketchbook) Color.parseColor("#78350F") else Color.parseColor("#FCD34D")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        val diffPillTextWidth = diffPillPaint.measureText(diffValueText)
        val diffPillRect = RectF(diffPillX, cardCurrentY, diffPillX + diffPillTextWidth + 24f, cardCurrentY + 28f)
        val diffPillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.4f
            color = if (isSketchbook) Color.parseColor("#E5D0B7") else Color.parseColor("#5C4827")
            pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
        }
        val diffPillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isSketchbook) Color.parseColor("#FEF3C7") else Color.parseColor("#2E2718")
        }
        canvas.drawRoundRect(diffPillRect, 14f, 14f, diffPillBgPaint)
        canvas.drawRoundRect(diffPillRect, 14f, 14f, diffPillBorderPaint)
        canvas.drawText(diffValueText, diffPillX + 12f, cardCurrentY + 19f, diffPillPaint)

        // Trailing dashed line across row
        val lineStartX = diffPillX + diffPillTextWidth + 36f
        val lineEndX = contentLeft + contentWidth
        if (lineEndX > lineStartX) {
            val dashLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.2f
                color = if (isSketchbook) Color.parseColor("#E0D6C8") else Color.parseColor("#38352E")
                pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
            }
            canvas.drawLine(lineStartX, cardCurrentY + 14f, lineEndX, cardCurrentY + 14f, dashLinePaint)
        }

        cardCurrentY += 44f

        if (prompt.isCreativeGap) {
            // -------------------------------------------------------------
            // CREATIVE GAP SHARE CARD LAYOUT
            // -------------------------------------------------------------

            // 1. FULL SENTENCE HERO NARRATIVE CONTAINER
            drawLeafIcon(canvas, contentLeft + 10f, cardCurrentY + 10f, 15f, diffTagColor)
            val gapTagPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = diffTagColor
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.12f
            }
            canvas.drawText("CREATIVE PROMPT", contentLeft + 26f, cardCurrentY + 16f, gapTagPaint)
            cardCurrentY += 28f

            val sentenceBgColor = if (isSketchbook) Color.parseColor("#F7F2E8") else Color.parseColor("#161513")
            val sentenceBorderColor = if (isSketchbook) Color.parseColor("#E8DDCF") else Color.parseColor("#2E2D27")
            val sentenceTextColor = if (isSketchbook) Color.parseColor("#1C1B18") else Color.parseColor("#FFFFFF")
            val quoteMarkColor = if (isSketchbook) Color.parseColor("#C4A882") else Color.parseColor("#7A684C")

            val sentencePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = sentenceTextColor
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val sentenceTextWidth = contentWidth - 100f
            val sentenceLayout = createStaticLayout(prompt.narrativeText, sentencePaint, sentenceTextWidth.toInt(), maxLines = 6)
            val sentenceBoxHeight = (36f + sentenceLayout.height + 36f).coerceAtLeast(100f)
            val sentenceRect = RectF(contentLeft, cardCurrentY, contentLeft + contentWidth, cardCurrentY + sentenceBoxHeight)

            val sBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = sentenceBgColor }
            val sBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.6f
                color = sentenceBorderColor
            }
            canvas.drawRoundRect(sentenceRect, 22f, 22f, sBg)
            canvas.drawRoundRect(sentenceRect, 22f, 22f, sBorder)

            // Large Quotation Mark (Opening “)
            val quoteMarkPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = quoteMarkColor
                textSize = 64f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            }
            canvas.drawText("“", contentLeft + 16f, cardCurrentY + 54f, quoteMarkPaint)

            // Sentence text
            canvas.save()
            canvas.translate(contentLeft + 52f, cardCurrentY + 28f)
            sentenceLayout.draw(canvas)
            canvas.restore()

            // Large Quotation Mark (Closing ”)
            canvas.drawText("”", contentLeft + contentWidth - 36f, cardCurrentY + sentenceBoxHeight - 12f, quoteMarkPaint)

            cardCurrentY += sentenceBoxHeight + 20f

            // 2. BULB ICON WITH "Fill in the blank with your own twist!"
            val twistBgColor = if (isSketchbook) Color.parseColor("#E6FFFA") else Color.parseColor("#0F2A28")
            val twistBorderColor = if (isSketchbook) Color.parseColor("#99F6E4") else Color.parseColor("#134E48")
            val twistTextColor = if (isSketchbook) Color.parseColor("#0F766E") else Color.parseColor("#2DD4BF")
            val twistHeight = 54f
            val twistRect = RectF(contentLeft, cardCurrentY, contentLeft + contentWidth, cardCurrentY + twistHeight)

            val tBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = twistBgColor }
            val tBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.4f
                color = twistBorderColor
            }
            canvas.drawRoundRect(twistRect, 16f, 16f, tBg)
            canvas.drawRoundRect(twistRect, 16f, 16f, tBorder)

            // Bulb Icon & Text
            drawLightbulbIcon(canvas, contentLeft + 28f, cardCurrentY + 27f, 18f, twistTextColor)
            val twistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = twistTextColor
                textSize = 21f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.04f
            }
            canvas.drawText("Fill in the blank with your own twist!", contentLeft + 50f, cardCurrentY + 34f, twistPaint)

            cardCurrentY += twistHeight + 20f

            // 3. IDEA STARTERS FOR THE BLANK
            val startersHeaderColor = if (isSketchbook) Color.parseColor("#D97706") else Color.parseColor("#FBBF24")
            val startersBoxBgColor = if (isSketchbook) Color.parseColor("#FCFBF7") else Color.parseColor("#1B1A17")
            val startersBoxBorderColor = if (isSketchbook) Color.parseColor("#EADBCE") else Color.parseColor("#38352E")
            val starterTextColor = if (isSketchbook) Color.parseColor("#292825") else Color.parseColor("#E4E4DF")

            val starters = prompt.displayGapIdeaStarters.take(3)
            val starterPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = starterTextColor
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            }

            var startersTextHeight = 0f
            val starterLayouts = starters.map { starter ->
                val layout = createStaticLayout("• $starter", starterPaint, (contentWidth - 64f).toInt(), maxLines = 3)
                startersTextHeight += layout.height + 14f
                layout
            }
            val startersBoxHeight = (44f + startersTextHeight + 18f).coerceAtLeast(130f)
            val startersRect = RectF(contentLeft, cardCurrentY, contentLeft + contentWidth, cardCurrentY + startersBoxHeight)

            val stBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = startersBoxBgColor }
            val stBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.4f
                color = startersBoxBorderColor
                pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
            }
            canvas.drawRoundRect(startersRect, 20f, 20f, stBg)
            canvas.drawRoundRect(startersRect, 20f, 20f, stBorder)

            // Header: Star icon + "IDEA STARTERS FOR THE BLANK:"
            drawStarSpark(canvas, contentLeft + 24f, cardCurrentY + 24f, 13f, startersHeaderColor)
            val stHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = startersHeaderColor
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.12f
            }
            canvas.drawText("IDEA STARTERS FOR THE BLANK", contentLeft + 42f, cardCurrentY + 30f, stHeaderPaint)

            var starterItemY = cardCurrentY + 54f
            starterLayouts.forEach { layout ->
                canvas.save()
                canvas.translate(contentLeft + 24f, starterItemY)
                layout.draw(canvas)
                canvas.restore()
                starterItemY += layout.height + 14f
            }

            cardCurrentY += startersBoxHeight + 20f

            // 4. STYLE & CHALLENGE BADGES (if present)
            if (prompt.stylePhrase.isNotBlank() || prompt.challengePhrase.isNotBlank()) {
                val halfWidth = (contentWidth - 20f) / 2f
                if (prompt.stylePhrase.isNotBlank() && prompt.challengePhrase.isNotBlank()) {
                    val styleColor = if (isSketchbook) Color.parseColor("#7C3AED") else Color.parseColor("#A78BFA")
                    drawFeatherQuillIcon(canvas, contentLeft + 10f, cardCurrentY + 12f, 14f, styleColor)
                    val sTag = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = styleColor
                        textSize = 14f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        letterSpacing = 0.10f
                    }
                    canvas.drawText("STYLE: ${prompt.stylePhrase}", contentLeft + 24f, cardCurrentY + 18f, sTag)

                    val chalColor = if (isSketchbook) Color.parseColor("#D97706") else Color.parseColor("#FBBF24")
                    drawStarSpark(canvas, contentLeft + halfWidth + 30f, cardCurrentY + 12f, 11f, chalColor)
                    val cTag = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = chalColor
                        textSize = 14f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        letterSpacing = 0.10f
                    }
                    canvas.drawText("CHALLENGE: ${prompt.challengePhrase}", contentLeft + halfWidth + 44f, cardCurrentY + 18f, cTag)
                } else if (prompt.stylePhrase.isNotBlank()) {
                    val styleColor = if (isSketchbook) Color.parseColor("#7C3AED") else Color.parseColor("#A78BFA")
                    drawFeatherQuillIcon(canvas, contentLeft + 10f, cardCurrentY + 12f, 14f, styleColor)
                    val sTag = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = styleColor
                        textSize = 15f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        letterSpacing = 0.10f
                    }
                    canvas.drawText("STYLE: ${prompt.stylePhrase}", contentLeft + 26f, cardCurrentY + 18f, sTag)
                } else {
                    val chalColor = if (isSketchbook) Color.parseColor("#D97706") else Color.parseColor("#FBBF24")
                    drawStarSpark(canvas, contentLeft + 10f, cardCurrentY + 12f, 12f, chalColor)
                    val cTag = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = chalColor
                        textSize = 15f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        letterSpacing = 0.10f
                    }
                    canvas.drawText("CHALLENGE: ${prompt.challengePhrase}", contentLeft + 26f, cardCurrentY + 18f, cTag)
                }
                cardCurrentY += 36f
            }

            // 5. ENCOURAGEMENT NOTE BOX WITH PAPERCLIP 📎
            val encourBoxHeight = 150f
            drawEncouragementBoxWithPaperclip(
                canvas = canvas,
                x = contentLeft,
                y = cardCurrentY,
                width = contentWidth,
                height = encourBoxHeight,
                isSketchbook = isSketchbook
            )
            cardCurrentY += encourBoxHeight + 20f

        } else {
            // -------------------------------------------------------------
            // 6. SUBJECT SECTION (Hero Title with Foliage Watermark)
            // -------------------------------------------------------------
            drawLeafIcon(canvas, contentLeft + 10f, cardCurrentY + 10f, 15f, diffTagColor)
            val subjectTagPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = diffTagColor
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.12f
            }
            canvas.drawText("SUBJECT", contentLeft + 26f, cardCurrentY + 16f, subjectTagPaint)
            cardCurrentY += 28f

            // Watermark illustration on right side
            val watermarkColor = if (isSketchbook) Color.parseColor("#22D97706") else Color.parseColor("#18FFE566")
            drawBotanicalBranch(canvas, contentLeft + contentWidth - 40f, cardCurrentY + 30f, scale = 0.75f, rotation = 40f, color = watermarkColor)

            // Subject prominent text
            val subjectTextColor = if (isSketchbook) Color.parseColor("#1A1917") else Color.WHITE
            val subjectTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = subjectTextColor
                textSize = 35f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subjectLayout = createStaticLayout(prompt.subjectPhrase, subjectTextPaint, contentWidth.toInt())
            canvas.save()
            canvas.translate(contentLeft, cardCurrentY)
            subjectLayout.draw(canvas)
            canvas.restore()
            cardCurrentY += subjectLayout.height + 22f

            // Divider under Subject
            val dividerColor = if (isSketchbook) Color.parseColor("#EFE7DC") else Color.parseColor("#2B2A26")
            drawDivider(canvas, contentLeft, cardCurrentY, contentWidth, dividerColor)
            cardCurrentY += 22f

            // -------------------------------------------------------------
            // 7. TWO-COLUMN GRID: LEFT & RIGHT COLUMNS
            // -------------------------------------------------------------
            val gridStartY = cardCurrentY
            var leftY = gridStartY
            var rightY = gridStartY

            // ====== LEFT COLUMN ======

            // A. SCENE (Teal Leaf Icon)
            if (prompt.scenePhrase.isNotBlank()) {
                val sceneColor = if (isSketchbook) Color.parseColor("#0D9488") else Color.parseColor("#2DD4BF")
                drawLeafIcon(canvas, leftColX + 8f, leftY + 10f, 15f, sceneColor)

                val sceneTagPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = sceneColor
                    textSize = 15f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    letterSpacing = 0.12f
                }
                canvas.drawText("SCENE", leftColX + 24f, leftY + 16f, sceneTagPaint)
                leftY += 28f

                val sceneTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isSketchbook) Color.parseColor("#292825") else Color.parseColor("#E4E4DF")
                    textSize = 21f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }
                val sceneLayout = createStaticLayout(prompt.scenePhrase, sceneTextPaint, colWidth.toInt())
                canvas.save()
                canvas.translate(leftColX, leftY)
                sceneLayout.draw(canvas)
                canvas.restore()
                leftY += sceneLayout.height + 22f

                drawDivider(canvas, leftColX, leftY, colWidth, dividerColor)
                leftY += 20f
            }

            // B. ATMOSPHERE (Sunburst Icon)
            if (prompt.atmospherePhrase.isNotBlank()) {
                val atmColor = if (isSketchbook) Color.parseColor("#D97706") else Color.parseColor("#FBBF24")
                drawSunRaysIcon(canvas, leftColX + 8f, leftY + 10f, 15f, atmColor)

                val atmTagPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = atmColor
                    textSize = 15f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    letterSpacing = 0.12f
                }
                canvas.drawText("ATMOSPHERE", leftColX + 24f, leftY + 16f, atmTagPaint)
                leftY += 28f

                val atmTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isSketchbook) Color.parseColor("#292825") else Color.parseColor("#E4E4DF")
                    textSize = 21f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }
                val atmLayout = createStaticLayout(prompt.atmospherePhrase, atmTextPaint, colWidth.toInt())
                canvas.save()
                canvas.translate(leftColX, leftY)
                atmLayout.draw(canvas)
                canvas.restore()
                leftY += atmLayout.height + 24f
            }

            // C. STORY HOOK: Realistic Washi-Tape Sticky Note
            if (prompt.displayStoryHook.isNotBlank()) {
                val hookNoteWidth = colWidth
                val hookTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isSketchbook) Color.parseColor("#292723") else Color.parseColor("#F5F3EF")
                    textSize = 19f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                }
                val hookLayout = createStaticLayout(prompt.displayStoryHook, hookTextPaint, (hookNoteWidth - 36f).toInt(), maxLines = 5)
                val hookNoteHeight = 44f + hookLayout.height + 36f

                // Render tilted washi-tape sticky note
                drawStickyNoteWithWashiTape(
                    canvas = canvas,
                    x = leftColX,
                    y = leftY,
                    width = hookNoteWidth,
                    height = hookNoteHeight,
                    hookLayout = hookLayout,
                    isSketchbook = isSketchbook
                )
                leftY += hookNoteHeight + 24f
            }

            // ====== RIGHT COLUMN ======

            // A. STYLE / MEDIUM with Watercolor Swatch Underline
            if (prompt.stylePhrase.isNotBlank()) {
                val styleColor = if (isSketchbook) Color.parseColor("#7C3AED") else Color.parseColor("#A78BFA")
                drawFeatherQuillIcon(canvas, rightColX + 8f, rightY + 10f, 15f, styleColor)

                val styleTagPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = styleColor
                    textSize = 15f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    letterSpacing = 0.12f
                }
                canvas.drawText("STYLE / MEDIUM", rightColX + 24f, rightY + 16f, styleTagPaint)
                rightY += 28f

                val styleTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isSketchbook) Color.parseColor("#1F1E1B") else Color.WHITE
                    textSize = 22f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val styleLayout = createStaticLayout(prompt.stylePhrase, styleTextPaint, colWidth.toInt())
                canvas.save()
                canvas.translate(rightColX, rightY)
                styleLayout.draw(canvas)
                canvas.restore()
                rightY += styleLayout.height + 10f

                // Purple / Lilac Watercolor brush stroke underline
                val swatchColor = if (isSketchbook) Color.parseColor("#C4B5FD") else Color.parseColor("#8B5CF6")
                drawWatercolorBrushStroke(
                    canvas = canvas,
                    x = rightColX,
                    y = rightY,
                    w = colWidth * 0.90f,
                    h = 16f,
                    color = swatchColor,
                    alpha = if (isSketchbook) 0.65f else 0.55f
                )
                rightY += 28f
            }

            // B. CHALLENGE with Golden Swatch Underline
            if (prompt.challengePhrase.isNotBlank()) {
                val chalColor = if (isSketchbook) Color.parseColor("#D97706") else Color.parseColor("#FBBF24")
                drawStarSpark(canvas, rightColX + 8f, rightY + 10f, 12f, chalColor)

                val chalTagPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = chalColor
                    textSize = 15f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    letterSpacing = 0.12f
                }
                canvas.drawText("CHALLENGE", rightColX + 24f, rightY + 16f, chalTagPaint)
                rightY += 28f

                val chalTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isSketchbook) Color.parseColor("#1F1E1B") else Color.WHITE
                    textSize = 21f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }
                val chalLayout = createStaticLayout(prompt.challengePhrase, chalTextPaint, colWidth.toInt())
                canvas.save()
                canvas.translate(rightColX, rightY)
                chalLayout.draw(canvas)
                canvas.restore()
                rightY += chalLayout.height + 10f

                // Golden / Yellow Watercolor brush stroke underline
                val goldSwatchColor = if (isSketchbook) Color.parseColor("#FDE68A") else Color.parseColor("#F59E0B")
                drawWatercolorBrushStroke(
                    canvas = canvas,
                    x = rightColX,
                    y = rightY,
                    w = colWidth * 0.75f,
                    h = 14f,
                    color = goldSwatchColor,
                    alpha = if (isSketchbook) 0.70f else 0.60f
                )
                rightY += 32f
            }

            // C. INSPIRATIONAL NOTE BOX WITH PAPERCLIP 📎
            val encourBoxHeight = 150f
            drawEncouragementBoxWithPaperclip(
                canvas = canvas,
                x = rightColX,
                y = rightY,
                width = colWidth,
                height = encourBoxHeight,
                isSketchbook = isSketchbook
            )
            rightY += encourBoxHeight + 20f

            // Synchronize row Y to max of left and right columns
            cardCurrentY = maxOf(leftY, rightY) + 16f

            // -------------------------------------------------------------
            // 8. FULL SENTENCE NARRATIVE QUOTE CONTAINER WITH LARGE QUOTES
            // -------------------------------------------------------------
            if (polishedSentence.isNotBlank()) {
                val quoteBgColor = if (isSketchbook) Color.parseColor("#F7F2E8") else Color.parseColor("#161513")
                val quoteBorderColor = if (isSketchbook) Color.parseColor("#E8DDCF") else Color.parseColor("#2E2D27")
                val quoteTextColor = if (isSketchbook) Color.parseColor("#262420") else Color.parseColor("#EBE8E1")
                val quoteMarkColor = if (isSketchbook) Color.parseColor("#C4A882") else Color.parseColor("#7A684C")

                val quoteTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = quoteTextColor
                    textSize = 21f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                }
                val quoteTextWidth = contentWidth - 100f
                val quoteLayout = createStaticLayout(polishedSentence, quoteTextPaint, quoteTextWidth.toInt(), maxLines = 6)
                val quoteBoxHeight = (36f + quoteLayout.height + 36f).coerceAtLeast(84f)
                val quoteRect = RectF(contentLeft, cardCurrentY, contentLeft + contentWidth, cardCurrentY + quoteBoxHeight)

                val quoteBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = quoteBgColor }
                val quoteBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 1.4f
                    color = quoteBorderColor
                }
                canvas.drawRoundRect(quoteRect, 22f, 22f, quoteBg)
                canvas.drawRoundRect(quoteRect, 22f, 22f, quoteBorder)

                // Large Quotation Mark (Opening “)
                val quoteMarkPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = quoteMarkColor
                    textSize = 58f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                }
                canvas.drawText("“", contentLeft + 16f, cardCurrentY + 48f, quoteMarkPaint)

                // Quote text
                canvas.save()
                canvas.translate(contentLeft + 50f, cardCurrentY + 24f)
                quoteLayout.draw(canvas)
                canvas.restore()

                // Large Quotation Mark (Closing ”)
                canvas.drawText("”", contentLeft + contentWidth - 36f, cardCurrentY + quoteBoxHeight - 12f, quoteMarkPaint)

                cardCurrentY += quoteBoxHeight + 20f
            }
        }

        // -------------------------------------------------------------
        // 9. FOOTER: ♡ Created with ArtSpark + Floating Quote Ribbon
        // -------------------------------------------------------------
        val heartColor = if (isSketchbook) Color.parseColor("#B45309") else Color.parseColor("#F59E0B")
        drawHeartIcon(canvas, width / 2f, cardCurrentY + 6f, 13f, heartColor)
        cardCurrentY += 22f

        // ✨ Created with ArtSpark ✨
        val footerSparkColor = if (isSketchbook) Color.parseColor("#D97706") else Color.parseColor("#FBBF24")
        drawStarSpark(canvas, width / 2f - 130f, cardCurrentY + 12f, 13f, footerSparkColor)
        drawStarSpark(canvas, width / 2f + 130f, cardCurrentY + 12f, 13f, footerSparkColor)

        val footerBrandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isSketchbook) Color.parseColor("#1F1E1B") else Color.WHITE
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Created with ArtSpark", width / 2f, cardCurrentY + 18f, footerBrandPaint)
        cardCurrentY += 28f

        // "You bring the art. We bring the spark."
        val footerSubPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isSketchbook) Color.parseColor("#6E6B65") else Color.parseColor("#A8A7A1")
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("You bring the art. We bring the spark.", width / 2f, cardCurrentY + 14f, footerSubPaint)
        cardCurrentY += 32f

        // Bottom floating quote ribbon capsule
        val pillWidth = 520f
        val pillHeight = 38f
        val pillX = width / 2f - (pillWidth / 2f)
        val pillRect = RectF(pillX, cardCurrentY, pillX + pillWidth, cardCurrentY + pillHeight)

        val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isSketchbook) Color.parseColor("#FEF3C7") else Color.parseColor("#262218")
        }
        val pillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.4f
            color = if (isSketchbook) Color.parseColor("#FDE68A") else Color.parseColor("#6B5024")
        }
        canvas.drawRoundRect(pillRect, 19f, 19f, pillBgPaint)
        canvas.drawRoundRect(pillRect, 19f, 19f, pillBorderPaint)

        val rotatingQuotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isSketchbook) Color.parseColor("#78350F") else Color.parseColor("#FCD34D")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(footerQuote, width / 2f, cardCurrentY + 24f, rotatingQuotePaint)
    }

    // -------------------------------------------------------------
    // ARTISTIC DRAWING & HELPER FUNCTIONS
    // -------------------------------------------------------------

    private fun drawStickyNoteWithWashiTape(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        hookLayout: StaticLayout,
        isSketchbook: Boolean
    ) {
        canvas.save()
        // Tilt the sticky note slightly by -2 degrees
        canvas.rotate(-1.8f, x + width / 2f, y + height / 2f)

        // Drop shadow for sticky note
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isSketchbook) Color.parseColor("#18000000") else Color.parseColor("#40000000")
        }
        val noteRect = RectF(x, y, x + width, y + height)
        val shadowRect = RectF(x + 2f, y + 4f, x + width + 2f, y + height + 4f)
        canvas.drawRoundRect(shadowRect, 14f, 14f, shadowPaint)

        // Note background (Warm parchment or cozy dark note)
        val noteBgColor = if (isSketchbook) Color.parseColor("#FCF7EB") else Color.parseColor("#272520")
        val noteBorderColor = if (isSketchbook) Color.parseColor("#EADBCE") else Color.parseColor("#3D3A31")

        val noteBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = noteBgColor }
        val noteBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            color = noteBorderColor
        }
        canvas.drawRoundRect(noteRect, 14f, 14f, noteBg)
        canvas.drawRoundRect(noteRect, 14f, 14f, noteBorder)

        // 💡 STORY HOOK Tag
        val hookTagColor = if (isSketchbook) Color.parseColor("#6366F1") else Color.parseColor("#818CF8")
        drawLightbulbIcon(canvas, x + 16f, y + 16f, 13f, hookTagColor)

        val hookTagPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = hookTagColor
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.12f
        }
        canvas.drawText("STORY HOOK", x + 34f, y + 22f, hookTagPaint)

        // Hook italic text
        canvas.save()
        canvas.translate(x + 16f, y + 36f)
        hookLayout.draw(canvas)
        canvas.restore()

        // Subtle sketch leaves on bottom right of sticky note
        val sketchColor = if (isSketchbook) Color.parseColor("#22B45309") else Color.parseColor("#22FCD34D")
        drawBotanicalBranch(canvas, x + width - 24f, y + height - 20f, scale = 0.5f, rotation = -30f, color = sketchColor)

        canvas.restore()

        // Washi tape strip at the top-right corner of note
        val tapeWidth = 68f
        val tapeHeight = 22f
        val tapeX = x + width - 50f
        val tapeY = y - 8f

        canvas.save()
        canvas.rotate(22f, tapeX + tapeWidth / 2f, tapeY + tapeHeight / 2f)

        val tapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isSketchbook) Color.parseColor("#C4B5FD") else Color.parseColor("#818CF8")
            alpha = 210
        }
        val tapeRect = RectF(tapeX, tapeY, tapeX + tapeWidth, tapeY + tapeHeight)
        canvas.drawRoundRect(tapeRect, 4f, 4f, tapePaint)

        canvas.restore()
    }

    private fun drawEncouragementBoxWithPaperclip(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        isSketchbook: Boolean
    ) {
        val boxRect = RectF(x, y, x + width, y + height)
        val boxBgColor = if (isSketchbook) Color.parseColor("#FAF7F0") else Color.parseColor("#22201C")
        val boxBorderColor = if (isSketchbook) Color.parseColor("#DFD7CA") else Color.parseColor("#38352E")

        val boxBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = boxBgColor }
        val boxBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.4f
            color = boxBorderColor
            pathEffect = DashPathEffect(floatArrayOf(6f, 5f), 0f)
        }
        canvas.drawRoundRect(boxRect, 18f, 18f, boxBg)
        canvas.drawRoundRect(boxRect, 18f, 18f, boxBorder)

        // Encouragement text
        val titleColor = if (isSketchbook) Color.parseColor("#1F1E1B") else Color.WHITE
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = titleColor
            textSize = 19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Go create something amazing! ♡", x + width / 2f, y + 42f, titlePaint)

        val subColor = if (isSketchbook) Color.parseColor("#57534E") else Color.parseColor("#D6D3D1")
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = subColor
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Trust your spark.", x + width / 2f, y + 74f, subPaint)
        canvas.drawText("We can’t wait to see what you make.", x + width / 2f, y + 98f, subPaint)

        // Cute hand-drawn brush smile/accent line
        val smilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.4f
            color = if (isSketchbook) Color.parseColor("#DC2626") else Color.parseColor("#F87171")
            strokeCap = Paint.Cap.ROUND
        }
        val smilePath = Path().apply {
            moveTo(x + width / 2f - 40f, y + 122f)
            quadTo(x + width / 2f, y + 128f, x + width / 2f + 40f, y + 122f)
        }
        canvas.drawPath(smilePath, smilePaint)

        // Metallic Paperclip 📎 on the top-right
        drawPaperclip(canvas, x + width - 24f, y - 6f, isSketchbook)
    }

    private fun drawPaperclip(canvas: Canvas, x: Float, y: Float, isSketchbook: Boolean) {
        val clipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.8f
            color = if (isSketchbook) Color.parseColor("#78716C") else Color.parseColor("#D6D3D1")
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        // Realistic folded wire paperclip path
        val path = Path().apply {
            moveTo(x, y + 36f)
            lineTo(x, y + 12f)
            arcTo(RectF(x, y, x + 18f, y + 18f), 180f, 180f, false)
            lineTo(x + 18f, y + 46f)
            arcTo(RectF(x - 6f, y + 36f, x + 18f, y + 54f), 0f, 180f, false)
            lineTo(x - 6f, y + 20f)
        }
        canvas.drawPath(path, clipPaint)
    }

    private fun drawWatercolorBrushStroke(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        color: Int,
        alpha: Float = 0.7f
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
            style = Paint.Style.FILL
        }

        val path = Path().apply {
            moveTo(x, y + h * 0.4f)
            quadTo(x + w * 0.25f, y, x + w * 0.5f, y + h * 0.2f)
            quadTo(x + w * 0.75f, y + h * 0.1f, x + w, y + h * 0.35f)
            quadTo(x + w * 0.95f, y + h * 0.75f, x + w * 0.85f, y + h)
            quadTo(x + w * 0.5f, y + h * 0.85f, x + w * 0.2f, y + h * 0.95f)
            quadTo(x + w * 0.05f, y + h * 0.7f, x, y + h * 0.4f)
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawBotanicalBranch(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        scale: Float = 1.0f,
        rotation: Float = 0f,
        color: Int,
        flipX: Boolean = false
    ) {
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(rotation)
        if (flipX) canvas.scale(-1f, 1f)
        canvas.scale(scale, scale)

        val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 2.2f
            strokeCap = Paint.Cap.ROUND
        }
        val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        // Stem curve
        val stem = Path().apply {
            moveTo(0f, 60f)
            quadTo(15f, 20f, 0f, -40f)
        }
        canvas.drawPath(stem, stemPaint)

        // Multiple delicate leaves sprouting along stem
        fun drawLeaf(lx: Float, ly: Float, angle: Float, leafSize: Float) {
            canvas.save()
            canvas.translate(lx, ly)
            canvas.rotate(angle)
            val leafPath = Path().apply {
                moveTo(0f, 0f)
                quadTo(leafSize * 0.5f, -leafSize * 0.4f, leafSize, 0f)
                quadTo(leafSize * 0.5f, leafSize * 0.4f, 0f, 0f)
                close()
            }
            canvas.drawPath(leafPath, leafPaint)
            canvas.restore()
        }

        drawLeaf(0f, -40f, -90f, 20f)
        drawLeaf(6f, -24f, -45f, 18f)
        drawLeaf(-4f, -14f, -135f, 18f)
        drawLeaf(9f, 2f, -35f, 20f)
        drawLeaf(-2f, 16f, -140f, 20f)
        drawLeaf(11f, 32f, -25f, 22f)
        drawLeaf(2f, 44f, -150f, 22f)

        canvas.restore()
    }

    private fun drawStarSpark(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        val path = Path().apply {
            moveTo(cx, cy - radius)
            quadTo(cx, cy, cx + radius, cy)
            quadTo(cx, cy, cx, cy + radius)
            quadTo(cx, cy, cx - radius, cy)
            quadTo(cx, cy, cx, cy - radius)
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawLeafIcon(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        val path = Path().apply {
            moveTo(cx - size * 0.4f, cy + size * 0.4f)
            quadTo(cx - size * 0.4f, cy - size * 0.4f, cx + size * 0.4f, cy - size * 0.4f)
            quadTo(cx + size * 0.4f, cy + size * 0.4f, cx - size * 0.4f, cy + size * 0.4f)
            close()
        }
        canvas.drawPath(path, paint)

        // Center vein
        val veinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        canvas.drawLine(cx - size * 0.4f, cy + size * 0.4f, cx + size * 0.4f, cy - size * 0.4f, veinPaint)
    }

    private fun drawSunRaysIcon(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, size * 0.28f, paint)

        val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 2.0f
            strokeCap = Paint.Cap.ROUND
        }
        for (i in 0 until 8) {
            val angle = i * (Math.PI / 4.0)
            val r1 = size * 0.38f
            val r2 = size * 0.58f
            val x1 = (cx + r1 * cos(angle)).toFloat()
            val y1 = (cy + r1 * sin(angle)).toFloat()
            val x2 = (cx + r2 * cos(angle)).toFloat()
            val y2 = (cy + r2 * sin(angle)).toFloat()
            canvas.drawLine(x1, y1, x2, y2, rayPaint)
        }
    }

    private fun drawFeatherQuillIcon(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        val path = Path().apply {
            moveTo(cx - size * 0.4f, cy + size * 0.45f)
            quadTo(cx, cy, cx + size * 0.45f, cy - size * 0.45f)
            quadTo(cx + size * 0.1f, cy - size * 0.1f, cx - size * 0.4f, cy + size * 0.45f)
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawLightbulbIcon(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy - (size * 0.15f), size * 0.38f, paint)
        val baseRect = RectF(cx - (size * 0.20f), cy + (size * 0.10f), cx + (size * 0.20f), cy + (size * 0.42f))
        canvas.drawRoundRect(baseRect, 2.5f, 2.5f, paint)
    }

    private fun drawHeartIcon(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val path = Path().apply {
            moveTo(cx, cy + size * 0.4f)
            cubicTo(cx - size * 0.6f, cy, cx - size * 0.5f, cy - size * 0.5f, cx, cy - size * 0.15f)
            cubicTo(cx + size * 0.5f, cy - size * 0.5f, cx + size * 0.6f, cy, cx, cy + size * 0.4f)
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawDivider(canvas: Canvas, x: Float, y: Float, width: Float, color: Int) {
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = 1.2f
        }
        canvas.drawLine(x, y, x + width, y, dividerPaint)
    }

    private fun createStaticLayout(
        text: String,
        paint: TextPaint,
        width: Int,
        maxLines: Int = 10
    ): StaticLayout {
        val safeWidth = width.coerceAtLeast(10)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, safeWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(5f, 1.2f)
                .setIncludePad(false)
                .setMaxLines(maxLines)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                text,
                paint,
                safeWidth,
                Layout.Alignment.ALIGN_NORMAL,
                1.2f,
                5f,
                false
            )
        }
    }
}
