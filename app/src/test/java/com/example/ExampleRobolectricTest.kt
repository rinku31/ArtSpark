package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.generator.PromptGenerator
import com.example.model.Difficulty
import com.example.model.PromptCategory
import com.example.model.PromptLockState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
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
}
