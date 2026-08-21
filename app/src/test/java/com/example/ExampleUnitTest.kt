package com.example

import com.example.generator.PromptSentenceBuilder
import com.example.ui.util.ShareTheme
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testPolishGrammar_fixesArticlesAndSpacing() {
        val raw = "a  apple in in an basket , rendered rendered as watercolor ."
        val polished = PromptSentenceBuilder.polishGrammar(raw)
        assertEquals("An apple in a basket, rendered as watercolor.", polished)
    }

    @Test
    fun testPolishGrammar_handlesConsonantsAndVowels() {
        val raw = "a ancient temple inside a abandoned grove"
        val polished = PromptSentenceBuilder.polishGrammar(raw)
        assertEquals("An ancient temple inside an abandoned grove.", polished)
    }

    @Test
    fun testBuildFullNarrative_continuousSentenceFlow() {
        val sentence = PromptSentenceBuilder.buildFullNarrative(
            trait = "Reckless, adrenaline-fueled",
            subject = "elven rogue and a mechanical clockwork falcon",
            action = "zip-lining down a burning sail to snatch a glowing power-core",
            environment = "A chaotic steampunk airship dock amidst the clouds",
            atmosphere = "Blazing orange firelight, dark storm clouds, and flashing lightning",
            style = "Dynamic anime cel-shading with dramatic motion lines"
        )
        assertFalse("Should not contain 'inside A '", sentence.contains("inside A "))
        assertFalse("Should not contain 'during B'", sentence.contains("during B"))
        assertTrue("Should contain 'inside a chaotic'", sentence.contains("inside a chaotic"))
        assertTrue("Should contain 'during blazing orange'", sentence.contains("during blazing orange"))
        assertTrue("Should contain 'rendered as a dynamic'", sentence.contains("rendered as a dynamic"))
    }

    @Test
    fun testShareThemes_exist() {
        assertEquals(2, ShareTheme.values().size)
        assertTrue(ShareTheme.values().contains(ShareTheme.COZY_NIGHT))
        assertTrue(ShareTheme.values().contains(ShareTheme.SKETCHBOOK))
    }
}
