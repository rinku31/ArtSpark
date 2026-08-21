package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
import com.example.model.ArtPrompt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object ShareCardRenderer {

    private const val WIDTH = 1080
    private const val HEIGHT = 1350

    /**
     * Renders a dedicated 1080x1350 high-resolution branded share card Bitmap for [prompt]
     * and saves it to the app cache directory, returning the shared File Uri.
     */
    suspend fun generateShareCardUri(context: Context, prompt: ArtPrompt): Uri = withContext(Dispatchers.IO) {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        renderCardOnCanvas(canvas, prompt)

        val cacheDir = File(context.cacheDir, "shared_sparks").apply { mkdirs() }
        val shareFile = File(cacheDir, "artspark_spark_${prompt.id}_${System.currentTimeMillis()}.png")
        FileOutputStream(shareFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            shareFile
        )
    }

    /**
     * Renders the complete visual design onto the canvas.
     */
    fun renderCardOnCanvas(canvas: Canvas, prompt: ArtPrompt) {
        val width = WIDTH.toFloat()
        val height = HEIGHT.toFloat()

        // 1. Dark canvas background with rich depth
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, height,
                intArrayOf(
                    Color.parseColor("#121211"),
                    Color.parseColor("#181816"),
                    Color.parseColor("#141413")
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        // 2. Subtle decorative glow orbs in background
        val yellowGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width * 0.85f, height * 0.12f, 320f,
                Color.parseColor("#33FFE566"),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(width * 0.85f, height * 0.12f, 320f, yellowGlowPaint)

        val purpleGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width * 0.15f, height * 0.85f, 350f,
                Color.parseColor("#2A6C5CE7"),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(width * 0.15f, height * 0.85f, 350f, purpleGlowPaint)

        // 3. Decorative subtle star sparks in background
        drawSparkIcon(canvas, 120f, 140f, 18f, Color.parseColor("#44FFE566"))
        drawSparkIcon(canvas, 960f, 260f, 14f, Color.parseColor("#334ECDC4"))
        drawSparkIcon(canvas, 940f, 1180f, 20f, Color.parseColor("#44A29BFE"))
        drawSparkIcon(canvas, 140f, 1200f, 16f, Color.parseColor("#33FFE566"))

        // 4. Header: Logo, Brand Name & Tagline
        var currentY = 70f

        // ArtSpark Spark Icon
        drawSparkIcon(canvas, width / 2f - 130f, currentY + 18f, 24f, Color.parseColor("#FFE566"))

        // Brand Name "ARTSPARK"
        val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.12f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ARTSPARK", width / 2f + 16f, currentY + 28f, brandPaint)
        currentY += 46f

        // Tagline: "Break the block. Make something."
        val taglinePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A0A09B")
            textSize = 19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Break the block. Make something.", width / 2f, currentY + 14f, taglinePaint)
        currentY += 44f

        // 5. Large Heading Banner: ✨ TODAY'S SPARK / INSPIRATION
        val bannerText = if (prompt.isDailySpark) "✨ TODAY'S SPARK" else "✨ ART INSPIRATION"
        val bannerRect = RectF(width / 2f - 190f, currentY, width / 2f + 190f, currentY + 48f)
        val bannerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2A2718")
        }
        val bannerStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.parseColor("#FFE566").let { Color.argb(120, Color.red(it), Color.green(it), Color.blue(it)) }
        }
        canvas.drawRoundRect(bannerRect, 24f, 24f, bannerBgPaint)
        canvas.drawRoundRect(bannerRect, 24f, 24f, bannerStrokePaint)

        val bannerTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFE566")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(bannerText, width / 2f, currentY + 32f, bannerTextPaint)
        currentY += 68f

        // 6. Main Structured Inspiration Card
        val cardLeft = 60f
        val cardRight = width - 60f
        val cardTop = currentY
        val cardBottom = height - 150f
        val cardWidth = cardRight - cardLeft

        val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)
        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1C1A")
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = Color.parseColor("#333330")
        }
        canvas.drawRoundRect(cardRect, 32f, 32f, cardBgPaint)
        canvas.drawRoundRect(cardRect, 32f, 32f, cardBorderPaint)

        // Inside the Card
        var contentY = cardTop + 36f
        val contentLeft = cardLeft + 36f
        val contentWidth = cardWidth - 72f

        // Category Tag Paint
        fun createTagPaint(colorHex: String): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(colorHex)
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.14f
        }

        // --- SECTION: SUBJECT ---
        val subjectTagPaint = createTagPaint("#FF6B6B")
        canvas.drawText("SUBJECT", contentLeft, contentY, subjectTagPaint)
        contentY += 14f

        val subjectText = prompt.subjectPhrase
        val subjectTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        contentY = drawWrappedText(canvas, subjectText, contentLeft, contentY, contentWidth, subjectTextPaint)
        contentY += 18f

        // Divider
        drawSectionDivider(canvas, contentLeft, contentY, contentWidth)
        contentY += 18f

        // --- SECTION: SCENE ---
        if (prompt.scenePhrase.isNotBlank()) {
            val sceneTagPaint = createTagPaint("#4ECDC4")
            canvas.drawText("SCENE", contentLeft, contentY, sceneTagPaint)
            contentY += 12f

            val sceneTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E4E4DF")
                textSize = 23f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            contentY = drawWrappedText(canvas, prompt.scenePhrase, contentLeft, contentY, contentWidth, sceneTextPaint)
            contentY += 18f

            drawSectionDivider(canvas, contentLeft, contentY, contentWidth)
            contentY += 18f
        }

        // --- SECTION: ATMOSPHERE ---
        if (prompt.atmospherePhrase.isNotBlank()) {
            val atmTagPaint = createTagPaint("#FFD166")
            canvas.drawText("ATMOSPHERE", contentLeft, contentY, atmTagPaint)
            contentY += 12f

            val atmTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E4E4DF")
                textSize = 21f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            contentY = drawWrappedText(canvas, prompt.atmospherePhrase, contentLeft, contentY, contentWidth, atmTextPaint)
            contentY += 18f

            drawSectionDivider(canvas, contentLeft, contentY, contentWidth)
            contentY += 18f
        }

        // --- SECTION: STYLE & CHALLENGE (Dual pill/cards) ---
        val pillWidth = (contentWidth - 20f) / 2f
        val pillHeight = 100f
        val pillY = contentY

        // Style Box (Left)
        if (prompt.stylePhrase.isNotBlank()) {
            val styleRect = RectF(contentLeft, pillY, contentLeft + pillWidth, pillY + pillHeight)
            val styleBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#222030") }
            val styleBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
                color = Color.parseColor("#44A29BFE")
            }
            canvas.drawRoundRect(styleRect, 18f, 18f, styleBg)
            canvas.drawRoundRect(styleRect, 18f, 18f, styleBorder)

            val styleTag = createTagPaint("#A29BFE")
            canvas.drawText("STYLE / MEDIUM", contentLeft + 16f, pillY + 28f, styleTag)

            val styleBody = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#EDEBF7")
                textSize = 19f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            drawWrappedText(canvas, prompt.stylePhrase, contentLeft + 16f, pillY + 40f, pillWidth - 32f, styleBody, maxLines = 2)
        }

        // Challenge Box (Right)
        if (prompt.challengePhrase.isNotBlank()) {
            val chalRect = RectF(contentLeft + pillWidth + 20f, pillY, contentLeft + contentWidth, pillY + pillHeight)
            val chalBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#29281E") }
            val chalBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
                color = Color.parseColor("#44FFE566")
            }
            canvas.drawRoundRect(chalRect, 18f, 18f, chalBg)
            canvas.drawRoundRect(chalRect, 18f, 18f, chalBorder)

            val chalTag = createTagPaint("#FFE566")
            canvas.drawText("CHALLENGE", contentLeft + pillWidth + 36f, pillY + 28f, chalTag)

            val chalBody = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F7F5EB")
                textSize = 19f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            drawWrappedText(canvas, prompt.challengePhrase, contentLeft + pillWidth + 36f, pillY + 40f, pillWidth - 32f, chalBody, maxLines = 2)
        }
        contentY += pillHeight + 18f

        // --- SECTION: STORY HOOK (if space and hook present) ---
        if (prompt.displayStoryHook.isNotBlank() && contentY < cardBottom - 70f) {
            val hookRect = RectF(contentLeft, contentY, contentLeft + contentWidth, contentY + 68f)
            val hookBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#232320") }
            canvas.drawRoundRect(hookRect, 14f, 14f, hookBg)

            val hookTag = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFE566")
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.1f
            }
            canvas.drawText("💡 STORY HOOK", contentLeft + 16f, contentY + 24f, hookTag)

            val hookTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#D5D5CF")
                textSize = 17f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            }
            drawWrappedText(canvas, prompt.displayStoryHook, contentLeft + 16f, contentY + 36f, contentWidth - 32f, hookTextPaint, maxLines = 1)
        }

        // 7. Footer
        val footerY = height - 90f
        val footerBrandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFE566")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Created with ArtSpark", width / 2f, footerY, footerBrandPaint)

        val footerTaglinePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8E8E89")
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("You bring the art. We bring the spark.", width / 2f, footerY + 26f, footerTaglinePaint)
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Float,
        paint: TextPaint,
        maxLines: Int = 10
    ): Float {
        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(4f, 1.15f)
                .setIncludePad(false)
                .setMaxLines(maxLines)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                text,
                paint,
                width.toInt(),
                Layout.Alignment.ALIGN_NORMAL,
                1.15f,
                4f,
                false
            )
        }

        canvas.save()
        canvas.translate(x, y)
        staticLayout.draw(canvas)
        canvas.restore()

        return y + staticLayout.height
    }

    private fun drawSectionDivider(canvas: Canvas, x: Float, y: Float, width: Float) {
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2B2B28")
            strokeWidth = 1.5f
        }
        canvas.drawLine(x, y, x + width, y, dividerPaint)
    }

    private fun drawSparkIcon(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int) {
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
}
