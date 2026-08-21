package com.example.data.api

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.generator.PromptData
import com.example.generator.PromptSentenceBuilder
import com.example.model.BrainstormIdea
import com.example.model.BrainstormMessage
import com.example.model.ClassicSpark
import com.example.model.ClassicSparkIdea
import com.example.model.CreativeGap
import com.example.model.CreativeGapIdea
import com.example.model.Difficulty
import com.example.model.DiscoverPrompt
import com.example.model.MessageSender
import com.example.model.PromptType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

sealed class BrainstormResult {
    data class Success(
        val replyText: String,
        val quickPills: List<String>,
        val idea: BrainstormIdea?,
        val actionType: String,
        val detectedPromptType: PromptType = idea?.promptType ?: PromptType.CLASSIC_SPARK
    ) : BrainstormResult()

    data class Error(
        val message: String,
        val isOffline: Boolean = false
    ) : BrainstormResult()
}

// Backward compatibility aliases
typealias GeminiResult = BrainstormResult
typealias GeminiApiClient = BrainstormApiClient

/**
 * Intelligent Brainstorm AI Client for ArtSpark.
 * - Directly calls Gemini 2.5 Flash via REST when a valid API key is present.
 * - Seamlessly falls back to the dynamic ArtSpark Creative Synthesis Engine when offline or if key is unavailable,
 *   guaranteeing artists are NEVER stuck with a dead-end error card.
 */
class BrainstormApiClient(private val context: Context) {

    companion object {
        private const val TAG = "BrainstormApiClient"
        private const val GEMINI_MODEL = "gemini-2.5-flash"
        private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isNotBlank() && key != "MY_GEMINI_API_KEY" && !key.startsWith("YOUR_")) {
                key
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Executes brainstorming session with Gemini API or dynamic ArtSpark Engine.
     */
    suspend fun brainstorm(
        messages: List<BrainstormMessage>,
        currentIdea: BrainstormIdea?,
        seedPrompt: DiscoverPrompt?,
        promptType: PromptType
    ): BrainstormResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        val latestUserMessage = messages.lastOrNull { it.sender == MessageSender.USER }?.text.orEmpty()

        if (apiKey.isNotBlank()) {
            try {
                val geminiResult = callGeminiRestApi(
                    apiKey = apiKey,
                    messages = messages,
                    currentIdea = currentIdea,
                    seedPrompt = seedPrompt,
                    promptType = promptType
                )
                if (geminiResult != null) {
                    return@withContext geminiResult
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini REST call encountered issue: ${e.message}. Falling back to Creative Engine.", e)
            }
        }

        // Generate response via on-device Creative Engine to ensure 100% reliability
        generateLocalCreativeResponse(
            userMessage = latestUserMessage,
            currentIdea = currentIdea,
            seedPrompt = seedPrompt,
            promptType = promptType
        )
    }

    /**
     * Calls Gemini 2.5 Flash REST API endpoint directly with structured JSON output schema.
     */
    private fun callGeminiRestApi(
        apiKey: String,
        messages: List<BrainstormMessage>,
        currentIdea: BrainstormIdea?,
        seedPrompt: DiscoverPrompt?,
        promptType: PromptType
    ): BrainstormResult? {
        val url = "$GEMINI_BASE_URL/$GEMINI_MODEL:generateContent?key=$apiKey"

        val systemPrompt = buildSystemPrompt(promptType, currentIdea, seedPrompt)
        val conversationText = buildConversationContext(messages)

        val requestPayload = JSONObject().apply {
            val contentsArray = JSONArray()

            // System instruction + context + prompt
            val contentObj = JSONObject().apply {
                put("role", "user")
                val partsArray = JSONArray().apply {
                    put(JSONObject().put("text", "$systemPrompt\n\nCONVERSATION HISTORY:\n$conversationText"))
                }
                put("parts", partsArray)
            }
            contentsArray.put(contentObj)
            put("contents", contentsArray)

            val generationConfig = JSONObject().apply {
                put("temperature", 0.7)
                put("responseMimeType", "application/json")
            }
            put("generationConfig", generationConfig)
        }

        val requestBody = requestPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("Content-Type", "application/json")
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: return null

        if (!response.isSuccessful) {
            Log.w(TAG, "Gemini API error status ${response.code}: $responseBody")
            return null
        }

        return parseGeminiResponse(responseBody, promptType, currentIdea, seedPrompt)
    }

    private fun buildSystemPrompt(promptType: PromptType, currentIdea: BrainstormIdea?, seedPrompt: DiscoverPrompt?): String {
        return when (promptType) {
            PromptType.CLASSIC_SPARK -> {
                val classicIdea = currentIdea as? ClassicSparkIdea
                val classicSeed = seedPrompt as? ClassicSpark
                val curDiff = classicIdea?.difficulty ?: classicSeed?.difficulty ?: Difficulty.MEDIUM
                val curTrait = classicIdea?.personalityTrait ?: classicSeed?.personalityTrait.orEmpty()
                val curSubj = classicIdea?.subjectCharacter ?: classicSeed?.subjectCharacter.orEmpty()
                val curAct = classicIdea?.actionSituationScene ?: classicSeed?.actionSituationScene.orEmpty()
                val curEnv = classicIdea?.environment ?: classicSeed?.environment.orEmpty()
                val curAtm = classicIdea?.atmosphereWeather ?: classicSeed?.atmosphereWeather.orEmpty()
                val curSty = classicIdea?.artStyle ?: classicSeed?.artStyle.orEmpty()
                val curCha = classicIdea?.creativeChallenge ?: classicSeed?.creativeChallenge.orEmpty()

                """
                You are ArtSpark's AI Brainstorm partner for artists.
                Help the artist explore, refine, or transform their prompt across 7 structured categories:
                1. personalityTrait
                2. subjectCharacter
                3. actionSituationScene
                4. environment
                5. atmosphereWeather
                6. artStyle
                7. creativeChallenge
                - storyHook (1 sentence narrative hook)
                - difficulty: "EASY" | "MEDIUM" | "HARD"

                Current state:
                - Difficulty: $curDiff
                - Trait: $curTrait
                - Subject: $curSubj
                - Action: $curAct
                - Environment: $curEnv
                - Atmosphere: $curAtm
                - Style: $curSty
                - Challenge: $curCha

                Return strictly JSON matching this structure:
                {
                  "reply": "2-3 encouraging, artist-friendly sentences",
                  "quickPills": ["Pill 1", "Pill 2", "Pill 3", "Pill 4"],
                  "actionType": "GENERATE_IDEA",
                  "idea": {
                    "difficulty": "EASY"|"MEDIUM"|"HARD",
                    "personalityTrait": "...",
                    "subjectCharacter": "...",
                    "actionSituationScene": "...",
                    "environment": "...",
                    "atmosphereWeather": "...",
                    "artStyle": "...",
                    "creativeChallenge": "...",
                    "storyHook": "..."
                  }
                }
                """.trimIndent()
            }
            PromptType.CREATIVE_GAP -> {
                val gapIdea = currentIdea as? CreativeGapIdea
                val gapSeed = seedPrompt as? CreativeGap
                val curSentence = gapIdea?.gapSentence ?: gapSeed?.gapSentence.orEmpty()

                """
                You are ArtSpark's AI Brainstorm partner for Creative Gap prompts.
                Keep the blank `______` inside `gapSentence`.
                Provide 3-5 creative starter options in `gapSuggestions`.
                
                Current sentence: "$curSentence"

                Return strictly JSON matching this structure:
                {
                  "reply": "2-3 encouraging, artist-friendly sentences",
                  "quickPills": ["Pill 1", "Pill 2", "Pill 3", "Pill 4"],
                  "actionType": "GENERATE_IDEA",
                  "idea": {
                    "difficulty": "EASY"|"MEDIUM"|"HARD",
                    "gapSentence": "A sentence containing ______",
                    "gapSuggestions": ["Choice A", "Choice B", "Choice C", "Choice D"],
                    "style": "...",
                    "challenge": "..."
                  }
                }
                """.trimIndent()
            }
        }
    }

    private fun buildConversationContext(messages: List<BrainstormMessage>): String {
        return messages.filter { !it.isError }.takeLast(8).joinToString("\n") { msg ->
            "${if (msg.sender == MessageSender.USER) "Artist" else "ArtSpark"}: ${msg.text}"
        }
    }

    private fun parseGeminiResponse(
        responseBody: String,
        fallbackType: PromptType,
        previousIdea: BrainstormIdea?,
        seedPrompt: DiscoverPrompt?
    ): BrainstormResult? {
        return try {
            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null

            var rawText = parts.getJSONObject(0).optString("text", "").trim()
            if (rawText.startsWith("```json")) rawText = rawText.removePrefix("```json").trim()
            if (rawText.startsWith("```")) rawText = rawText.removePrefix("```").trim()
            if (rawText.endsWith("```")) rawText = rawText.removeSuffix("```").trim()

            val json = JSONObject(rawText)
            val reply = json.optString("reply", "Here is a fresh concept to spark your imagination!").trim()
            val actionType = json.optString("actionType", "GENERATE_IDEA")

            val quickPills = mutableListOf<String>()
            val pillsArray = json.optJSONArray("quickPills")
            if (pillsArray != null) {
                for (i in 0 until pillsArray.length()) {
                    val p = pillsArray.optString(i).trim()
                    if (p.isNotBlank()) quickPills.add(p)
                }
            }

            val ideaObj = json.optJSONObject("idea")
            val isGap = ideaObj?.has("gapSentence") == true || fallbackType == PromptType.CREATIVE_GAP

            val finalIdea: BrainstormIdea = if (isGap) {
                val prevGap = previousIdea as? CreativeGapIdea
                val seedGap = seedPrompt as? CreativeGap
                val diffStr = ideaObj?.optString("difficulty", prevGap?.difficulty?.name ?: seedGap?.difficulty?.name ?: "MEDIUM").orEmpty()
                val diff = try { Difficulty.valueOf(diffStr.uppercase()) } catch (e: Exception) { Difficulty.MEDIUM }

                var sentence = ideaObj?.optString("gapSentence", prevGap?.gapSentence ?: seedGap?.gapSentence.orEmpty()).orEmpty().trim()
                if (sentence.isBlank()) sentence = "A lonely wanderer uncovers a glowing ______ amidst the ancient ruins."
                if (!sentence.contains("______") && !sentence.contains("___")) sentence = "$sentence ______"

                val suggestions = mutableListOf<String>()
                val sArray = ideaObj?.optJSONArray("gapSuggestions")
                if (sArray != null) {
                    for (i in 0 until sArray.length()) {
                        val s = sArray.optString(i).trim()
                        if (s.isNotBlank()) suggestions.add(s)
                    }
                }
                if (suggestions.isEmpty()) {
                    suggestions.addAll(prevGap?.gapSuggestions ?: seedGap?.displayGapSuggestions ?: listOf("luminescent crystal", "mechanical heart", "living constellation", "forgotten key"))
                }

                CreativeGapIdea(
                    difficulty = diff,
                    gapSentence = sentence,
                    gapSuggestions = suggestions,
                    style = ideaObj?.optString("style", prevGap?.style ?: seedGap?.style.orEmpty())?.ifBlank { "Storybook Gouache" } ?: "Storybook Gouache",
                    challenge = ideaObj?.optString("challenge", prevGap?.challenge ?: seedGap?.challenge.orEmpty())?.ifBlank { "Harmonious warm lighting" } ?: "Harmonious warm lighting"
                )
            } else {
                val prevClassic = previousIdea as? ClassicSparkIdea
                val seedClassic = seedPrompt as? ClassicSpark
                val diffStr = ideaObj?.optString("difficulty", prevClassic?.difficulty?.name ?: seedClassic?.difficulty?.name ?: "MEDIUM").orEmpty()
                val diff = try { Difficulty.valueOf(diffStr.uppercase()) } catch (e: Exception) { Difficulty.MEDIUM }

                val trait = ideaObj?.optString("personalityTrait", prevClassic?.personalityTrait ?: seedClassic?.personalityTrait.orEmpty()).orEmpty().trim().ifBlank { PromptData.traits.random() }
                val subj = ideaObj?.optString("subjectCharacter", prevClassic?.subjectCharacter ?: seedClassic?.subjectCharacter.orEmpty()).orEmpty().trim().ifBlank { PromptData.subjects.random() }
                val scene = ideaObj?.optString("actionSituationScene", prevClassic?.actionSituationScene ?: seedClassic?.actionSituationScene.orEmpty()).orEmpty().trim().ifBlank { PromptData.actions.random() }
                val env = ideaObj?.optString("environment", prevClassic?.environment ?: seedClassic?.environment.orEmpty()).orEmpty().trim().ifBlank { PromptData.environments.random() }
                val atm = ideaObj?.optString("atmosphereWeather", prevClassic?.atmosphereWeather ?: seedClassic?.atmosphereWeather.orEmpty()).orEmpty().trim().ifBlank { PromptData.atmospheres.random() }
                val style = ideaObj?.optString("artStyle", prevClassic?.artStyle ?: seedClassic?.artStyle.orEmpty()).orEmpty().trim().ifBlank { PromptData.styles.random() }
                val challenge = ideaObj?.optString("creativeChallenge", prevClassic?.creativeChallenge ?: seedClassic?.creativeChallenge.orEmpty()).orEmpty().trim().ifBlank { PromptData.challenges.random() }
                val storyHook = ideaObj?.optString("storyHook", prevClassic?.storyHook ?: seedClassic?.storyHook.orEmpty()).orEmpty().trim()

                val fullSentence = PromptSentenceBuilder.buildFullPromptSentence(
                    trait = trait,
                    subject = subj,
                    action = scene,
                    environment = env,
                    atmosphere = atm,
                    style = style,
                    challenge = challenge
                )

                ClassicSparkIdea(
                    difficulty = diff,
                    personalityTrait = trait,
                    subjectCharacter = subj,
                    actionSituationScene = scene,
                    environment = env,
                    atmosphereWeather = atm,
                    artStyle = style,
                    creativeChallenge = challenge,
                    storyHook = storyHook,
                    generatedSentence = fullSentence
                )
            }

            BrainstormResult.Success(
                replyText = reply,
                quickPills = if (quickPills.isNotEmpty()) quickPills else listOf("Make atmosphere darker", "Change action / scene", "Try another art style", "Increase difficulty"),
                idea = finalIdea,
                actionType = actionType,
                detectedPromptType = finalIdea.promptType
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing Gemini response", e)
            null
        }
    }

    /**
     * Local Creative Synthesis Engine.
     * Evaluates user intent, dynamically mutates categories or creates fresh concepts,
     * providing an artist-friendly response and fresh interactive pills instantly.
     */
    private fun generateLocalCreativeResponse(
        userMessage: String,
        currentIdea: BrainstormIdea?,
        seedPrompt: DiscoverPrompt?,
        promptType: PromptType
    ): BrainstormResult {
        val lower = userMessage.lowercase().trim()
        val random = Random.Default

        if (promptType == PromptType.CREATIVE_GAP || (currentIdea is CreativeGapIdea)) {
            return handleLocalCreativeGap(lower, currentIdea as? CreativeGapIdea, seedPrompt as? CreativeGap, random)
        } else {
            return handleLocalClassicSpark(lower, currentIdea as? ClassicSparkIdea, seedPrompt as? ClassicSpark, random)
        }
    }

    private fun handleLocalClassicSpark(
        userMessage: String,
        currentIdea: ClassicSparkIdea?,
        seedPrompt: ClassicSpark?,
        random: Random
    ): BrainstormResult {
        val prevTrait = currentIdea?.personalityTrait ?: seedPrompt?.personalityTrait.orEmpty()
        val prevSubj = currentIdea?.subjectCharacter ?: seedPrompt?.subjectCharacter.orEmpty()
        val prevAct = currentIdea?.actionSituationScene ?: seedPrompt?.actionSituationScene.orEmpty()
        val prevEnv = currentIdea?.environment ?: seedPrompt?.environment.orEmpty()
        val prevAtm = currentIdea?.atmosphereWeather ?: seedPrompt?.atmosphereWeather.orEmpty()
        val prevSty = currentIdea?.artStyle ?: seedPrompt?.artStyle.orEmpty()
        val prevCha = currentIdea?.creativeChallenge ?: seedPrompt?.creativeChallenge.orEmpty()
        val prevDiff = currentIdea?.difficulty ?: seedPrompt?.difficulty ?: Difficulty.MEDIUM

        var newTrait = prevTrait.ifBlank { PromptData.traits.random(random) }
        var newSubj = prevSubj.ifBlank { PromptData.subjects.random(random) }
        var newAct = prevAct.ifBlank { PromptData.actions.random(random) }
        var newEnv = prevEnv.ifBlank { PromptData.environments.random(random) }
        var newAtm = prevAtm.ifBlank { PromptData.atmospheres.random(random) }
        var newSty = prevSty.ifBlank { PromptData.styles.random(random) }
        var newCha = prevCha.ifBlank { PromptData.challenges.random(random) }
        var newDiff = prevDiff
        var reply = ""
        var pills = listOf<String>()

        when {
            userMessage.contains("block") || userMessage.contains("break through") || userMessage.contains("stuck") || userMessage.isEmpty() -> {
                newTrait = listOf("Curious", "Eccentric", "Enchanted", "Fearless", "Gentle", "Mischievous").random(random)
                newSubj = listOf("Clockwork Dragon", "Celestial Cartographer", "Little Fox Alchemist", "Cyberpunk Botanist", "Overgrown Golem", "Star-catching Owl").random(random)
                newAct = listOf("brewing glowing elixirs in floating teacups", "weaving tapestries made of starlight", "repairing a miniature solar system", "cataloging phosphorescent mushrooms", "baking pastries shaped like constellations").random(random)
                newEnv = listOf("Sunken Crystal Library", "Bioluminescent Cloud City", "Cozy Attic Workshop", "Ancient Mossy Conservatory", "Neon-lit Starlit Rooftop").random(random)
                newAtm = listOf("Golden hour glow with floating embers", "Soft lantern light in misty drizzle", "Bioluminescent turquoise twilight", "Warm fireplace contrast against stormy dusk").random(random)
                newSty = listOf("Lush Storybook Watercolor", "Textured Gouache & Ink", "Rich Studio Ghibli Aesthetic", "Vintage Risograph Print", "Luminous Digital Painting").random(random)
                newCha = listOf("Use a warm analogous palette", "Focus on dramatic rim lighting", "Draw within 20 minutes", "Emphasize expressive silhouette").random(random)
                reply = "Let's smash that creative block! Here's a high-vibe, story-rich concept with playful textures and glowing lighting to get your pencil moving immediately."
                pills = listOf("Make atmosphere darker", "Change art style", "Add unexpected twist", "Increase difficulty")
            }
            userMessage.contains("hard") || userMessage.contains("difficult") || userMessage.contains("challenge") -> {
                newDiff = Difficulty.HARD
                newCha = listOf(
                    "Limit to 2 complementary colors only (no black/white)",
                    "Dramatic extreme bird's-eye perspective",
                    "Single continuous line underlay with speed ink",
                    "No eraser allowed / 15-minute speed challenge",
                    "Dual high-contrast light sources (cyan & magenta)"
                ).random(random)
                reply = "Difficulty dialed up! Added a high-focus constraint: \"$newCha\". This will stretch your composition and lighting instincts."
                pills = listOf("Try another challenge", "Change lighting", "Suggest story twist", "Simplify prompt")
            }
            userMessage.contains("easy") || userMessage.contains("simple") || userMessage.contains("relax") -> {
                newDiff = Difficulty.EASY
                newCha = "Relaxed free sketch with soft shading"
                newAct = listOf("resting quietly", "enjoying warm tea", "observing the clouds", "reading an old book").random(random)
                reply = "Dialed it back to a cozy, stress-free sketch. Focus on soft shapes and having fun with the flow."
                pills = listOf("Add cozy atmosphere", "Try pastel palette", "Change character", "Make it harder")
            }
            userMessage.contains("dark") || userMessage.contains("moody") || userMessage.contains("shadow") || userMessage.contains("night") -> {
                newAtm = listOf("Heavy rain with dramatic neon reflections", "Deep indigo twilight lit by solitary candlelight", "Thunderstorm dusk with electric rim light", "Thick midnight fog pierced by glowing lanterns").random(random)
                reply = "Atmosphere deepened! Shifting into moody shadows, high value contrast, and cinematic ambient light."
                pills = listOf("Add glowing element", "Change subject", "Try Cyberpunk Noir", "Increase difficulty")
            }
            userMessage.contains("style") || userMessage.contains("medium") || userMessage.contains("technique") -> {
                newSty = listOf("90s Retro Anime Cel-Shading", "Dynamic Comic Ink & Halftone", "Impressionist Oil Brushstrokes", "Soft Children's Book Gouache", "Ukiyo-e Woodblock Print").random(random)
                reply = "Swapped the aesthetic to **$newSty**. Think about the unique texture, brushwork, and edge control this medium brings."
                pills = listOf("Try watercolor style", "Try comic ink style", "Change character", "Suggest color palette")
            }
            userMessage.contains("action") || userMessage.contains("scene") || userMessage.contains("doing") -> {
                newAct = listOf("discovering a hidden portal behind bookshelf", "calibrating an ancient brass astrolabe", "sharing glowing tea with spirit companions", "gliding across rooftops on a glider").random(random)
                reply = "Updated the scene action to **$newAct** to create dynamic flow and natural movement in your composition."
                pills = listOf("Make atmosphere darker", "Change environment", "Try another art style", "Add creative challenge")
            }
            userMessage.contains("character") || userMessage.contains("creature") || userMessage.contains("subject") -> {
                newSubj = listOf("Rooftop Gargoyle Sculptor", "Wandering Star Weaver", "Deep-sea Jellyfish Mage", "Mecha Garden Tender", "Nomadic Sand Cat Rider").random(random)
                reply = "Brought in a new hero: **$newSubj**! Pair their silhouette with distinctive props and expressive body language."
                pills = listOf("Change personality", "Change environment", "Make atmosphere darker", "Increase difficulty")
            }
            else -> {
                newTrait = listOf("Adventurous", "Mysterious", "Whimsical", "Contemplative", "Playful").random(random)
                newSubj = PromptData.subjects.random(random)
                newAct = PromptData.actions.random(random)
                newEnv = PromptData.environments.random(random)
                newAtm = PromptData.atmospheres.random(random)
                newSty = PromptData.styles.random(random)
                newCha = PromptData.challenges.random(random)
                reply = "Here's an inspiring new spark tailored to your direction! Notice how the mood and setting work together."
                pills = listOf("Make atmosphere darker", "Change action / scene", "Try another art style", "Increase difficulty")
            }
        }

        val generatedSentence = PromptSentenceBuilder.buildFullPromptSentence(
            trait = newTrait,
            subject = newSubj,
            action = newAct,
            environment = newEnv,
            atmosphere = newAtm,
            style = newSty,
            challenge = newCha
        )

        val idea = ClassicSparkIdea(
            difficulty = newDiff,
            personalityTrait = newTrait,
            subjectCharacter = newSubj,
            actionSituationScene = newAct,
            environment = newEnv,
            atmosphereWeather = newAtm,
            artStyle = newSty,
            creativeChallenge = newCha,
            storyHook = "What secret is hidden just outside the frame?",
            generatedSentence = generatedSentence
        )

        return BrainstormResult.Success(
            replyText = reply,
            quickPills = pills,
            idea = idea,
            actionType = "GENERATE_IDEA",
            detectedPromptType = PromptType.CLASSIC_SPARK
        )
    }

    private fun handleLocalCreativeGap(
        userMessage: String,
        currentIdea: CreativeGapIdea?,
        seedPrompt: CreativeGap?,
        random: Random
    ): BrainstormResult {
        val prevDiff = currentIdea?.difficulty ?: seedPrompt?.difficulty ?: Difficulty.MEDIUM
        var sentence = currentIdea?.gapSentence ?: seedPrompt?.gapSentence ?: "An alchemist accidentally summons a tiny ______ while brewing tea."
        var style = currentIdea?.style ?: seedPrompt?.style ?: "Storybook Gouache"
        var challenge = currentIdea?.challenge ?: seedPrompt?.challenge ?: "Warm lighting with soft edges"
        var suggestions = currentIdea?.gapSuggestions ?: seedPrompt?.displayGapSuggestions ?: listOf("miniature star", "mischievous cloud", "clockwork hummingbird", "glowing spirit")
        var newDiff = prevDiff
        var reply = ""
        var pills = listOf<String>()

        when {
            userMessage.contains("hard") || userMessage.contains("difficult") || userMessage.contains("challenge") -> {
                newDiff = Difficulty.HARD
                challenge = "Render the blank with translucent/glowing materials using only 3 colors"
                reply = "Added an advanced material constraint! How would you render the texture and luminosity of whatever you draw in the blank?"
                pills = listOf("Suggest new starters", "Change sentence", "Try Sci-Fi style", "Simplify prompt")
            }
            userMessage.contains("twist") || userMessage.contains("suggest") || userMessage.contains("blank") || userMessage.contains("idea") -> {
                suggestions = listOf("living constellation", "pocket-sized vortex", "crystalline familiar", "singing mechanical orb", "sentient cup of tea")
                reply = "Here are 5 playful possibilities for the blank `______`! Pick whichever sparks the most curiosity."
                pills = listOf("Make it harder", "Suggest art style", "Change sentence", "Switch to Classic Spark")
            }
            else -> {
                val pool = listOf(
                    "A wanderer finds an ancient robot tending a garden of ______ under two moons.",
                    "An artist paints a door on an alley wall, and a ______ steps out.",
                    "During a midnight festival, lanterns guide a gentle ______ across the canal.",
                    "A detective opens a locked safe to find only a glowing ______ inside."
                )
                sentence = pool.random(random)
                suggestions = listOf("starlit bonsai", "whispering compass", "mechanical firefly swarm", "crystallized memory")
                style = listOf("Textured Gouache & Ink", "Risograph Screenprint", "Luminous Concept Art", "Vintage Woodcut").random(random)
                challenge = listOf("Focus on silhouette & negative space", "Use a 3-color analogous palette", "Warm rim lighting").random(random)
                reply = "Here is a fresh Creative Gap prompt! Fill in the blank `______` with whatever unique element you want to bring to life."
                pills = listOf("Suggest twists for the blank", "Make it harder", "Suggest art style", "Give variations")
            }
        }

        val idea = CreativeGapIdea(
            difficulty = newDiff,
            gapSentence = sentence,
            gapSuggestions = suggestions,
            style = style,
            challenge = challenge
        )

        return BrainstormResult.Success(
            replyText = reply,
            quickPills = pills,
            idea = idea,
            actionType = "GENERATE_IDEA",
            detectedPromptType = PromptType.CREATIVE_GAP
        )
    }
}
