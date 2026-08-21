package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.generator.PromptGenerator
import com.example.model.ArtPrompt
import com.example.model.ArtSparkIdea
import com.example.model.ClassicSparkIdea
import com.example.model.CreativeGapIdea
import com.example.model.Difficulty
import com.example.model.MessageSender
import com.example.model.PromptCategory
import com.example.model.PromptLockState
import com.example.ui.navigation.NavSection
import com.example.ui.viewmodel.ArtSparkViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ArtSpark", appName)
    }

    @Test
    fun `test prompt generator produces valid prompt`() {
        val prompt = PromptGenerator.generate(difficulty = Difficulty.MEDIUM)
        assertNotNull(prompt)
        assertTrue(prompt.subject.isNotBlank())
        assertTrue(prompt.narrativeText.isNotBlank())
    }

    @Test
    fun `test locked category is preserved on generation`() {
        val lockState = PromptLockState(
            lockedCategories = setOf(PromptCategory.SUBJECT),
            lockedValues = mapOf(PromptCategory.SUBJECT to "Fox")
        )
        val prompt = PromptGenerator.generate(
            difficulty = Difficulty.MEDIUM,
            lockState = lockState
        )
        assertEquals("Fox", prompt.subject)
    }

    @Test
    fun `test daily spark produces deterministic output for today`() {
        val spark1 = PromptGenerator.generateDailySpark()
        val spark2 = PromptGenerator.generateDailySpark()
        assertEquals(spark1.id, spark2.id)
        assertEquals(spark1.subject, spark2.subject)
        assertEquals(spark1.style, spark2.style)
    }

    @Test
    fun `test navigation tabs include brainstorm in order`() {
        val expectedTabs = listOf(
            NavSection.DISCOVER,
            NavSection.BRAINSTORM,
            NavSection.FAVORITES,
            NavSection.HISTORY
        )
        assertEquals(expectedTabs, NavSection.values().toList())
    }

    @Test
    fun `test apply brainstorm idea to workspace sets custom categories and locks`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = ArtSparkViewModel(app)
        val testIdea = ClassicSparkIdea(
            difficulty = Difficulty.HARD,
            personalityTrait = "Sly",
            subjectCharacter = "Neon Chameleon",
            actionSituationScene = "hacking a vending machine",
            environment = "Cyberpunk Alley",
            atmosphereWeather = "Rainy neon reflections",
            artStyle = "Synthwave Pixel Art",
            creativeChallenge = "Limited 4-color palette"
        )

        var navigated = false
        viewModel.applyBrainstormIdeaToWorkspace(testIdea) {
            navigated = true
        }

        assertTrue(navigated)
        val prompt = viewModel.currentPrompt.value
        assertTrue("Prompt should be ClassicSpark", prompt is com.example.model.ClassicSpark)
        assertEquals("Neon Chameleon", prompt.subject)
        assertEquals("Sly", prompt.trait)
        assertEquals("Cyberpunk Alley", prompt.displayEnvironment)
        assertEquals("Synthwave Pixel Art", prompt.style)
        assertEquals(Difficulty.HARD, prompt.difficulty)
        assertEquals(Difficulty.HARD, viewModel.selectedDifficulty.value)
        assertTrue(viewModel.lockState.value.isLocked(PromptCategory.SUBJECT))
    }

    @Test
    fun `test apply creative gap brainstorm idea preserves creative gap mode and difficulty`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = ArtSparkViewModel(app)
        val testGapIdea = com.example.model.CreativeGapIdea(
            difficulty = Difficulty.HARD,
            gapSentence = "An astronaut finds a glowing ______ floating outside the orbital station.",
            gapSuggestions = listOf("pocket watch", "origami crane", "cassette tape"),
            style = "Risograph print",
            challenge = "Duotone cyan and magenta"
        )

        var navigated = false
        viewModel.applyBrainstormIdeaToWorkspace(testGapIdea) {
            navigated = true
        }

        assertTrue(navigated)
        val prompt = viewModel.currentPrompt.value
        assertTrue("Prompt should be CreativeGap", prompt is com.example.model.CreativeGap)
        assertTrue(viewModel.isCreativeGapMode.value)
        assertEquals("An astronaut finds a glowing ______ floating outside the orbital station.", prompt.narrativeText)
        assertEquals(Difficulty.HARD, prompt.difficulty)
        assertEquals(Difficulty.HARD, viewModel.selectedDifficulty.value)
        assertEquals("Risograph print", prompt.style)
        assertEquals("Duotone cyan and magenta", prompt.challenge)
    }

    @Test
    fun `test creative gap prompt initializes brainstorm session with gap template and pills`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = ArtSparkViewModel(app)

        val creativeGapPrompt = ArtPrompt(
            id = 9999L,
            trait = "Curious",
            subject = "little fox explorer",
            action = "",
            environment = "Sunken crystal library",
            atmosphere = "Bioluminescent glow",
            style = "Storybook Watercolor",
            challenge = "Soft pastel tones",
            isCreativeGap = true,
            gapTemplate = "A curious little fox explorer discovers a mysterious ______ in a sunken crystal library."
        )

        var navigated = false
        viewModel.startBrainstormWithPrompt(creativeGapPrompt) {
            navigated = true
        }

        assertTrue(navigated)
        val state = viewModel.brainstormState.value
        assertEquals(1, state.messages.size)
        val firstMessage = state.messages.first()
        assertTrue(firstMessage.text.contains("Creative Gap prompt"))
        assertTrue(firstMessage.text.contains("discovers a mysterious ______"))
        assertTrue(firstMessage.quickPills.any { it.contains("blank", ignoreCase = true) || it.contains("gap", ignoreCase = true) })
    }

    @Test
    fun `test prompt sentence builder handles indefinite articles and traits correctly`() {
        val subject1 = com.example.generator.PromptSentenceBuilder.buildSubjectPhrase(
            trait = "awestruck, inquisitive",
            subject = "an underwater explorer"
        )
        assertEquals("An awestruck, inquisitive underwater explorer", subject1)

        val subject2 = com.example.generator.PromptSentenceBuilder.buildSubjectPhrase(
            trait = "curious",
            subject = "fox"
        )
        assertEquals("A curious fox", subject2)

        val subject3 = com.example.generator.PromptSentenceBuilder.buildSubjectPhrase(
            trait = "",
            subject = "underwater explorer"
        )
        assertEquals("An underwater explorer", subject3)

        val subject4 = com.example.generator.PromptSentenceBuilder.buildSubjectPhrase(
            trait = "ancient",
            subject = "a dragon"
        )
        assertEquals("An ancient dragon", subject4)
    }

    @Test
    fun `test structured sections in ArtPrompt`() {
        val prompt = com.example.model.ArtPrompt(
            id = 42L,
            trait = "Awestruck, Inquisitive",
            subject = "Underwater Explorer",
            action = "discovering ancient scrolls inside",
            environment = "a sunken library",
            atmosphere = "golden sunlight filtering through deep blue water",
            style = "Chiaroscuro Digital Painting",
            challenge = "Focus on extreme depth of field"
        )

        assertEquals("An awestruck, inquisitive underwater explorer", prompt.subjectPhrase)
        assertEquals("Discovering ancient scrolls inside a sunken library", prompt.scenePhrase)
        assertEquals("Golden sunlight filtering through deep blue water", prompt.atmospherePhrase)
        assertEquals("Chiaroscuro Digital Painting", prompt.stylePhrase)
        assertEquals("Focus on extreme depth of field", prompt.challengePhrase)
        assertTrue(prompt.displayStoryHook.isNotBlank())
        assertTrue(prompt.toShareText().contains("SUBJECT:"))
        assertTrue(prompt.toShareText().contains("SCENE:"))
    }

    @Test
    fun `test share card bitmap renderer`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prompt = com.example.model.ArtPrompt(
            id = 999L,
            trait = "Luminous",
            subject = "Sea Creature",
            action = "navigating through",
            environment = "a coral reef palace",
            atmosphere = "bioluminescent moonless night",
            style = "Watercolor on textured cold-press paper",
            challenge = "Use only three colors"
        )
        val bitmap = android.graphics.Bitmap.createBitmap(1080, 1350, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        com.example.ui.util.ShareCardRenderer.renderCardOnCanvas(canvas, prompt)
        assertNotNull(bitmap)
        assertEquals(1080, bitmap.width)
        assertEquals(1350, bitmap.height)
    }

    @Test
    fun `test mode toggle retains previous board when cycling through spark or gap`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = ArtSparkViewModel(app)

        val initialClassicPrompt = viewModel.currentPrompt.value
        assertTrue("Initial prompt should be ClassicSpark", initialClassicPrompt is com.example.model.ClassicSpark)

        // Switching mode to creative gap for the first time generates and displays a CreativeGap
        viewModel.setCreativeGapMode(true)
        assertTrue(viewModel.isCreativeGapMode.value)
        val firstGapPrompt = viewModel.currentPrompt.value
        assertTrue("Prompt should now be CreativeGap", firstGapPrompt is com.example.model.CreativeGap)

        // Switching back to classic spark retains the exact previous classic spark board
        viewModel.setCreativeGapMode(false)
        org.junit.Assert.assertFalse(viewModel.isCreativeGapMode.value)
        assertEquals("Should retain original classic spark board", initialClassicPrompt.displayPromptText, viewModel.currentPrompt.value.displayPromptText)

        // Switching back to creative gap retains the exact first creative gap board
        viewModel.setCreativeGapMode(true)
        assertTrue(viewModel.isCreativeGapMode.value)
        assertEquals("Should retain original creative gap board", firstGapPrompt.displayPromptText, viewModel.currentPrompt.value.displayPromptText)

        // Only when reroll() is called does that mode get a new board
        viewModel.reroll()
        val rerolledGapPrompt = viewModel.currentPrompt.value
        assertTrue("Rerolled prompt should be CreativeGap", rerolledGapPrompt is com.example.model.CreativeGap)

        // Switching back to classic spark still retains the classic spark board
        viewModel.setCreativeGapMode(false)
        assertEquals("Should still retain original classic spark board", initialClassicPrompt.displayPromptText, viewModel.currentPrompt.value.displayPromptText)
    }
}

