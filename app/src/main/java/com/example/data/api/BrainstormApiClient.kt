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
import java.io.IOException
import java.util.concurrent.TimeUnit

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
 * Uses Google Gemini API via BuildConfig.GEMINI_API_KEY.
 * Provides rich, structured brainstorming assistance for Classic Spark and Creative Gap.
 */
class BrainstormApiClient(private val context: Context) {

    companion object {
        private const val TAG = "BrainstormApiClient"
        private const val GEMINI_MODEL = "gemini-3.5-flash"
        private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        return try {
            val key = BuildConfig.GEMINI_API_KEY.trim()
            if (key.isNotBlank() && key != "MY_GEMINI_API_KEY" && !key.startsWith("YOUR_") && !key.startsWith("PASTE_")) {
                key
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Executes brainstorming session with Gemini API.
     */
    suspend fun brainstorm(
        messages: List<BrainstormMessage>,
        currentIdea: BrainstormIdea?,
        seedPrompt: DiscoverPrompt?,
        promptType: PromptType
    ): BrainstormResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext BrainstormResult.Error(
                message = "Gemini API key is not configured. Please set GEMINI_API_KEY in the AI Studio Secrets panel."
            )
        }

        try {
            callGeminiRestApi(
                apiKey = apiKey,
                messages = messages,
                currentIdea = currentIdea,
                seedPrompt = seedPrompt,
                promptType = promptType
            )
        } catch (e: IOException) {
            Log.e(TAG, "Network error during Gemini call", e)
            BrainstormResult.Error(
                message = "Network error: Unable to connect to Gemini API. Please check your internet connection.",
                isOffline = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Gemini call", e)
            BrainstormResult.Error(
                message = "Brainstorm error: ${e.message ?: "An unexpected error occurred."}"
            )
        }
    }

    /**
     * Calls Gemini REST API endpoint directly with structured JSON output schema.
     */
    private fun callGeminiRestApi(
        apiKey: String,
        messages: List<BrainstormMessage>,
        currentIdea: BrainstormIdea?,
        seedPrompt: DiscoverPrompt?,
        promptType: PromptType
    ): BrainstormResult {
        val url = "$GEMINI_BASE_URL/$GEMINI_MODEL:generateContent?key=$apiKey"

        val systemPrompt = buildSystemPrompt(promptType, currentIdea, seedPrompt)
        val conversationText = buildConversationContext(messages)

        val requestPayload = JSONObject().apply {
            val contentsArray = JSONArray()

            val contentObj = JSONObject().apply {
                put("role", "user")
                val partsArray = JSONArray().apply {
                    put(JSONObject().put("text", "$systemPrompt\n\nCONVERSATION HISTORY:\n$conversationText\n\nRespond with strictly valid JSON matching the requested schema."))
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
        val responseBody = response.body?.string()

        if (!response.isSuccessful || responseBody.isNullOrBlank()) {
            val errorMsg = try {
                if (!responseBody.isNullOrBlank()) {
                    val errJson = JSONObject(responseBody)
                    errJson.optJSONObject("error")?.optString("message") ?: "HTTP status ${response.code}"
                } else {
                    "HTTP status ${response.code}"
                }
            } catch (e: Exception) {
                "HTTP status ${response.code}"
            }
            Log.w(TAG, "Gemini API error ${response.code}: $responseBody")
            return BrainstormResult.Error("Gemini API error (${response.code}): $errorMsg")
        }

        return parseGeminiResponse(responseBody, promptType, currentIdea, seedPrompt)
            ?: BrainstormResult.Error("Failed to parse response from Gemini. Please try again.")
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
                You are ArtSpark's AI Brainstorm partner for artists, specializing in Classic Spark prompts.
                Classic Spark uses EXACTLY 7 structured categories:
                1. personalityTrait: A character/subject personality trait (e.g. "Curious", "Eccentric", "Melancholy", "Fearless")
                2. subjectCharacter: The main character, creature, or focal subject (e.g. "Clockwork Owl", "Cyberpunk Botanist", "Overgrown Golem")
                3. actionSituationScene: The specific action, situation, or dynamic scene (e.g. "brewing glowing elixirs in floating teacups", "repairing an ancient astrolabe")
                4. environment: The physical setting or world (e.g. "Sunken Crystal Library", "Overgrown Conservatory", "Bioluminescent Cloud City")
                5. atmosphereWeather: The lighting, weather, and mood (e.g. "Golden hour glow with floating embers", "Thick midnight fog pierced by lantern light")
                6. artStyle: The visual art style or medium (e.g. "Lush Storybook Watercolor", "Risograph Screenprint", "Studio Ghibli Aesthetic", "Dynamic Comic Ink")
                7. creativeChallenge: An artistic constraint or composition challenge (e.g. "Focus on dramatic rim lighting", "Limit palette to 3 warm colors", "Extreme bird's-eye perspective")

                You also maintain:
                - storyHook: A 1-sentence intriguing narrative question or hook.
                - difficulty: "EASY" | "MEDIUM" | "HARD"

                CURRENT PROMPT STATE:
                - Difficulty: $curDiff
                - Personality/Trait: $curTrait
                - Subject/Character: $curSubj
                - Action/Situation/Scene: $curAct
                - Environment: $curEnv
                - Atmosphere & Weather: $curAtm
                - Art Style: $curSty
                - Creative Challenge: $curCha

                RULES:
                - Help the artist explore, refine, or transform their prompt across all 7 categories.
                - When the artist asks to change or twist a category, update it while harmonizing the remaining categories.
                - Always return all 7 categories in the "idea" object.
                - If the user asks to change difficulty (e.g., "make it hard", "make it easy", "increase difficulty"), adjust the "difficulty" field ("EASY", "MEDIUM", "HARD") and update the challenge accordingly.
                - Never treat this prompt as a Creative Gap.
                - In "reply", provide 2-3 enthusiastic, supportive sentences explaining the direction.
                - In "quickPills", provide 4 short, relevant follow-up action suggestions (e.g. ["Make atmosphere darker", "Change art style", "Add unexpected twist", "Increase difficulty"]).

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
                val curDiff = gapIdea?.difficulty ?: gapSeed?.difficulty ?: Difficulty.MEDIUM
                val curSentence = gapIdea?.gapSentence ?: gapSeed?.gapSentence.orEmpty()
                val curSuggestions = (gapIdea?.gapSuggestions ?: gapSeed?.displayGapSuggestions ?: emptyList()).joinToString(", ")
                val curStyle = gapIdea?.style ?: gapSeed?.style.orEmpty()
                val curChallenge = gapIdea?.challenge ?: gapSeed?.challenge.orEmpty()

                """
                You are ArtSpark's AI Brainstorm partner for Creative Gap prompts.
                Creative Gap is a fill-in-the-blank prompt format containing:
                1. gapSentence: A sentence with a blank `______` (e.g., "An alchemist accidentally summons a tiny ______ while brewing tea.")
                2. gapSuggestions: A list of 3-5 creative starter options for the blank (e.g., ["living constellation", "pocket-sized vortex", "crystalline familiar"])
                3. style: Recommended art style
                4. challenge: Creative challenge or constraint
                5. difficulty: "EASY" | "MEDIUM" | "HARD"

                CURRENT PROMPT STATE:
                - Difficulty: $curDiff
                - Gap Sentence: "$curSentence"
                - Idea Starters: $curSuggestions
                - Style: "$curStyle"
                - Challenge: "$curChallenge"

                RULES:
                - Creative Gap is a distinct entity. Do NOT invent the 7 Classic Spark categories for it.
                - Keep the blank `______` inside `gapSentence`.
                - Provide 3-5 fresh, inspiring fill-in starter suggestions in `gapSuggestions`.
                - If the user asks to change difficulty (e.g., "make it hard", "easy"), adjust the "difficulty" field ("EASY", "MEDIUM", "HARD").
                - In "reply", provide 2-3 encouraging, artist-friendly sentences.
                - In "quickPills", provide 4 short follow-up action suggestions (e.g. ["Suggest twists for the blank", "Change art style", "Make it harder", "Surprise variations"]).

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
        return messages.filter { !it.isError }.takeLast(10).joinToString("\n") { msg ->
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
}
