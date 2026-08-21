package com.example.data.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.BuildConfig
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

sealed class GeminiResult {
    data class Success(
        val replyText: String,
        val quickPills: List<String>,
        val idea: BrainstormIdea?,
        val actionType: String,
        val detectedPromptType: PromptType = idea?.promptType ?: PromptType.CLASSIC_SPARK
    ) : GeminiResult()

    data class Error(
        val message: String,
        val isOffline: Boolean = false,
        val isApiKeyMissing: Boolean = false
    ) : GeminiResult()
}

class GeminiApiClient(private val context: Context) {

    companion object {
        private const val TAG = "GeminiApiClient"
        private const val MODEL_NAME = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

        private const val SYSTEM_INSTRUCTION_UNIFIED = """
You are the AI creative brainstorming partner in ArtSpark — an Android application that inspires artists with playful, imaginative, and actionable art prompts.

ArtSpark features TWO distinct prompt types:

1. CLASSIC SPARK (7 Structured Categories):
   - `personality`: Character mood, posture, personality, or quirk (e.g. "Curious", "Sleepy", "Heroic").
   - `subject`: Central creature, character, figure, object, or entity (e.g. "Cyberpunk Samurai", "Overgrown Golem", "Little Fox Explorer").
   - `scene`: What they are doing, the situation, or dynamic scene (e.g. "baking glowing pastries", "repairing a clockwork dragon").
   - `environment`: Setting, terrain, or architecture (e.g. "Sunken Crystal Library", "Neon Rooftop").
   - `atmosphere`: Lighting, weather, time of day, mood (e.g. "Moody candlelight and rain", "Golden sunset glow").
   - `style`: Artistic medium, rendering technique (e.g. "Storybook Watercolor", "Gouache & Ink", "90s Anime").
   - `challenge`: Creative drawing constraint (e.g. "Use 3 colors only", "15 minute sketch").
   - `storyHook`: (optional) 1-sentence narrative spark.
   - `difficulty`: "EASY", "MEDIUM", or "HARD".

2. CREATIVE GAP (Fill-In-The-Blank):
   - `gapSentence`: An evocative sentence containing `______` as the blank to be filled in (e.g. "A lonely automaton searches for a missing ______ in the neon fog.").
   - `gapSuggestions`: At least 3 creative, distinct idea starters for what could go in the blank.
   - `style`: Suggested art medium / rendering style (e.g. "Gouache on textured paper", "Risograph Print").
   - `challenge`: Creative drawing constraint.
   - `difficulty`: "EASY", "MEDIUM", or "HARD".

PROMPT TYPE DETERMINATION & USER INTENT:
- When starting a brainstorm session or receiving a message such as "Let's make a creative gap", "Create a creative gap", "Make a fill in the blank", "gap prompt", "gap sentence", or whenever the user asks for a Creative Gap:
  You MUST set `ideaType` to "CREATIVE_GAP" and provide the Creative Gap structure (`gapSentence` containing `______`, `gapSuggestions` with 3-5 idea starters, `style`, `challenge`, and `difficulty`). Do NOT output Classic Spark subject/scene fields.
- When starting a brainstorm session or receiving a message such as "Let's make a classic spark", "Create a classic spark", "7 categories", or when asking for a structured classic prompt:
  You MUST set `ideaType` to "CLASSIC_SPARK" and provide all 7 Classic Spark categories (`personality`, `subject`, `scene`, `environment`, `atmosphere`, `style`, `challenge`, `storyHook`, `difficulty`). Do NOT output gap fields.
- If the user does not specify a prompt type, follow the active prompt mode provided in the active context below.

RULES:
1. When generating a Creative Gap, ALWAYS KEEP THE BLANK (`______`) in `gapSentence`. Do NOT remove the blank. Provide 3-5 distinct `gapSuggestions`.
2. When generating a Classic Spark, provide all 7 categories (`personality`, `subject`, `scene`, `environment`, `atmosphere`, `style`, `challenge`).
3. When asked to make it harder / increase difficulty, set difficulty to "HARD" and introduce an intriguing constraint. When asked to simplify / make easier, set difficulty to "EASY".
4. Always include 3-5 concise, clickable options in `quickPills` that offer relevant next steps or twists.
5. Tone: Enthusiastic, playful, artist-friendly, and concise (2-3 sentences max).
6. Output format must strictly match this JSON schema:
{
  "reply": "Concise conversational reply (markdown allowed).",
  "quickPills": ["Choice 1", "Choice 2", "Choice 3", "Choice 4"],
  "hasIdea": true,
  "ideaType": "CLASSIC_SPARK" | "CREATIVE_GAP",
  "idea": {
    "difficulty": "EASY" | "MEDIUM" | "HARD",
    "personality": "...",
    "subject": "...",
    "scene": "...",
    "environment": "...",
    "atmosphere": "...",
    "style": "...",
    "challenge": "...",
    "storyHook": "...",
    "gapSentence": "Sentence containing ______",
    "gapSuggestions": ["Starter 1", "Starter 2", "Starter 3"]
  },
  "actionType": "GENERATE_IDEA" | "UPDATE_CATEGORY" | "CREATE_VARIATIONS" | "SUGGEST_CHANGES" | "CONVERSE"
}
"""
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun isOnline(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = connectivityManager?.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            true
        }
    }

    suspend fun brainstorm(
        messages: List<BrainstormMessage>,
        currentIdea: BrainstormIdea?,
        seedPrompt: DiscoverPrompt?,
        promptType: PromptType
    ): GeminiResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeminiResult.Error(
                message = "Gemini API key is not configured. Please set GEMINI_API_KEY in the Secrets panel in AI Studio.",
                isApiKeyMissing = true
            )
        }

        if (!isOnline()) {
            return@withContext GeminiResult.Error(
                message = "Brainstorm AI needs an internet connection, but your ArtSpark randomizer still works.",
                isOffline = true
            )
        }

        try {
            val endpoint = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"
            val requestJson = buildRequestBody(messages, currentIdea, seedPrompt, promptType)

            val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(endpoint)
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                val errorMsg = if (response.code == 400 || response.code == 403) {
                    "Invalid API configuration or quota reached (${response.code})."
                } else {
                    "Server returned code ${response.code}: ${response.message}"
                }
                return@withContext GeminiResult.Error(errorMsg)
            }

            parseGeminiResponse(responseBody, messages, currentIdea, seedPrompt, promptType)
        } catch (e: IOException) {
            Log.e(TAG, "Network error during Gemini request", e)
            GeminiResult.Error(
                message = "Connection issue: ${e.localizedMessage ?: "Network failed"}. Your randomizer remains available.",
                isOffline = !isOnline()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in Gemini brainstorming", e)
            GeminiResult.Error("Something went wrong while brainstorming: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun buildRequestBody(
        messages: List<BrainstormMessage>,
        currentIdea: BrainstormIdea?,
        seedPrompt: DiscoverPrompt?,
        promptType: PromptType
    ): JSONObject {
        val root = JSONObject()

        // System Instruction
        val systemInstructionObj = JSONObject()
        val sysParts = JSONArray()
        var fullSysPrompt = SYSTEM_INSTRUCTION_UNIFIED

        // Append active context
        when (promptType) {
            PromptType.CLASSIC_SPARK -> {
                val classicIdea = currentIdea as? ClassicSparkIdea
                val classicSeed = seedPrompt as? ClassicSpark
                val p = classicIdea?.personalityTrait ?: classicSeed?.personalityTrait.orEmpty()
                val s = classicIdea?.subjectCharacter ?: classicSeed?.subjectCharacter.orEmpty()
                val sc = classicIdea?.actionSituationScene ?: classicSeed?.actionSituationScene.orEmpty()
                val env = classicIdea?.environment ?: classicSeed?.environment.orEmpty()
                val atm = classicIdea?.atmosphereWeather ?: classicSeed?.atmosphereWeather.orEmpty()
                val sty = classicIdea?.artStyle ?: classicSeed?.artStyle.orEmpty()
                val ch = classicIdea?.creativeChallenge ?: classicSeed?.creativeChallenge.orEmpty()
                val diff = classicIdea?.difficulty ?: classicSeed?.difficulty ?: Difficulty.MEDIUM

                fullSysPrompt += "\n\nACTIVE CONTEXT MODE: CLASSIC SPARK (Difficulty: ${diff.name}):\n" +
                        "• Personality/Trait: $p\n" +
                        "• Subject/Character: $s\n" +
                        "• Scene/Action: $sc\n" +
                        "• Environment: $env\n" +
                        "• Atmosphere/Weather: $atm\n" +
                        "• Art Style: $sty\n" +
                        "• Creative Challenge: $ch\n"
            }
            PromptType.CREATIVE_GAP -> {
                val gapIdea = currentIdea as? CreativeGapIdea
                val gapSeed = seedPrompt as? CreativeGap
                val sentence = gapIdea?.gapSentence ?: gapSeed?.gapSentence.orEmpty()
                val suggestions = gapIdea?.gapSuggestions ?: gapSeed?.displayGapSuggestions ?: emptyList()
                val sty = gapIdea?.style ?: gapSeed?.style.orEmpty()
                val ch = gapIdea?.challenge ?: gapSeed?.challenge.orEmpty()
                val diff = gapIdea?.difficulty ?: gapSeed?.difficulty ?: Difficulty.MEDIUM

                fullSysPrompt += "\n\nACTIVE CONTEXT MODE: CREATIVE GAP (Difficulty: ${diff.name}):\n" +
                        "• Gap Sentence: \"$sentence\"\n" +
                        "• Fill-in Suggestions: ${suggestions.joinToString(", ")}\n" +
                        "• Style: $sty\n" +
                        "• Challenge: $ch\n"
            }
        }

        val lastUserMessage = messages.lastOrNull { it.sender == MessageSender.USER }?.text?.lowercase().orEmpty()
        val userWantsCreativeGap = lastUserMessage.contains("creative gap") ||
                lastUserMessage.contains("fill in the blank") ||
                lastUserMessage.contains("fill-in-the-blank") ||
                lastUserMessage.contains("gap prompt") ||
                lastUserMessage.contains("gap sentence") ||
                lastUserMessage.contains("make a gap")

        val userWantsClassicSpark = lastUserMessage.contains("classic spark") ||
                lastUserMessage.contains("classic prompt") ||
                lastUserMessage.contains("7 categories") ||
                lastUserMessage.contains("seven categories")

        if (userWantsCreativeGap) {
            fullSysPrompt += "\n\nEXPLICIT USER DIRECTIVE: The user is explicitly asking to create a CREATIVE GAP prompt. You MUST set `ideaType` to \"CREATIVE_GAP\" and generate an evocative `gapSentence` containing `______` and 3-5 `gapSuggestions`."
        } else if (userWantsClassicSpark) {
            fullSysPrompt += "\n\nEXPLICIT USER DIRECTIVE: The user is explicitly asking to create a CLASSIC SPARK prompt. You MUST set `ideaType` to \"CLASSIC_SPARK\" and generate all 7 structured categories."
        }

        sysParts.put(JSONObject().put("text", fullSysPrompt))
        systemInstructionObj.put("parts", sysParts)
        root.put("systemInstruction", systemInstructionObj)

        // Contents (conversation history)
        val contentsArray = JSONArray()
        val validHistory = messages.filter { !it.isError }

        for (msg in validHistory) {
            val contentObj = JSONObject()
            val role = if (msg.sender == MessageSender.USER) "user" else "model"
            contentObj.put("role", role)

            val partsArray = JSONArray()
            val textContent = if (msg.sender == MessageSender.AI && msg.idea != null) {
                when (val idea = msg.idea) {
                    is ClassicSparkIdea -> {
                        "${msg.text}\n[Classic Spark State (Difficulty: ${idea.difficulty.name}): Personality: ${idea.personalityTrait}, Subject: ${idea.subjectCharacter}, Scene: ${idea.actionSituationScene}, Environment: ${idea.environment}, Atmosphere: ${idea.atmosphereWeather}, Style: ${idea.artStyle}, Challenge: ${idea.creativeChallenge}]"
                    }
                    is CreativeGapIdea -> {
                        "${msg.text}\n[Creative Gap State (Difficulty: ${idea.difficulty.name}): Gap Sentence: \"${idea.gapSentence}\", Suggestions: ${idea.gapSuggestions.joinToString(", ")}, Style: ${idea.style}, Challenge: ${idea.challenge}]"
                    }
                    else -> msg.text
                }
            } else {
                msg.text
            }
            partsArray.put(JSONObject().put("text", textContent))
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
        }

        // If history is empty, add initial prompt
        if (contentsArray.length() == 0) {
            val initialPrompt = when (promptType) {
                PromptType.CLASSIC_SPARK -> {
                    if (seedPrompt is ClassicSpark) {
                        "Let's brainstorm this Classic Spark:\n\"${seedPrompt.displayPromptText}\"\nHow can we develop, refine, or twist this concept?"
                    } else {
                        "Hello! Let's brainstorm a fresh Classic Spark artwork prompt."
                    }
                }
                PromptType.CREATIVE_GAP -> {
                    if (seedPrompt is CreativeGap) {
                        "Let's brainstorm this Creative Gap prompt:\n\"${seedPrompt.gapSentence}\"\nWhat are some imaginative ideas for the blank?"
                    } else {
                        "Hello! Let's brainstorm a fresh Creative Gap artwork prompt."
                    }
                }
            }
            val contentObj = JSONObject()
            contentObj.put("role", "user")
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", initialPrompt))
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
        }

        root.put("contents", contentsArray)

        // Generation Config
        val genConfig = JSONObject()
        genConfig.put("temperature", 0.7)
        genConfig.put("responseMimeType", "application/json")
        root.put("generationConfig", genConfig)

        return root
    }

    private fun parseGeminiResponse(
        rawJson: String,
        messages: List<BrainstormMessage>,
        previousIdea: BrainstormIdea?,
        seedPrompt: DiscoverPrompt?,
        promptType: PromptType
    ): GeminiResult {
        return try {
            val root = JSONObject(rawJson)
            val candidates = root.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val firstPart = parts?.optJSONObject(0)
            val text = firstPart?.optString("text").orEmpty()

            if (text.isBlank()) {
                return GeminiResult.Error("Received empty response from Gemini.")
            }

            val cleanedJsonText = cleanJsonFence(text)
            val parsedObj = JSONObject(cleanedJsonText)

            val reply = parsedObj.optString("reply", "Here is a fresh creative idea!").trim()
            val actionType = parsedObj.optString("actionType", "GENERATE_IDEA")

            val quickPillsList = mutableListOf<String>()
            val pillsArray = parsedObj.optJSONArray("quickPills")
            if (pillsArray != null) {
                for (i in 0 until pillsArray.length()) {
                    val pill = pillsArray.optString(i).trim()
                    if (pill.isNotBlank()) {
                        quickPillsList.add(pill)
                    }
                }
            }

            val ideaObj = parsedObj.optJSONObject("idea")

            // Determine if the returned idea is Creative Gap or Classic Spark based on user intent and JSON output
            val latestUserMsg = messages.lastOrNull { it.sender == MessageSender.USER }?.text?.lowercase().orEmpty()
            val userExplicitlyWantsGap = latestUserMsg.contains("creative gap") ||
                    latestUserMsg.contains("fill in the blank") ||
                    latestUserMsg.contains("fill-in-the-blank") ||
                    latestUserMsg.contains("gap prompt") ||
                    latestUserMsg.contains("gap sentence") ||
                    latestUserMsg.contains("make a gap")

            val userExplicitlyWantsClassic = latestUserMsg.contains("classic spark") ||
                    latestUserMsg.contains("classic prompt") ||
                    latestUserMsg.contains("7 categories") ||
                    latestUserMsg.contains("seven categories")

            val rawIdeaType = parsedObj.optString("ideaType", ideaObj?.optString("type", "") ?: "").uppercase()
            val isGap = when {
                userExplicitlyWantsGap -> true
                userExplicitlyWantsClassic -> false
                rawIdeaType == "CREATIVE_GAP" -> true
                rawIdeaType == "CLASSIC_SPARK" -> false
                ideaObj != null && (ideaObj.has("gapSentence") || ideaObj.has("gapSuggestions")) && !ideaObj.has("subject") && !ideaObj.has("scene") -> true
                ideaObj != null && (ideaObj.has("subject") || ideaObj.has("scene") || ideaObj.has("environment") || ideaObj.has("personality")) -> false
                else -> promptType == PromptType.CREATIVE_GAP
            }

            val structuredIdea: BrainstormIdea? = if (isGap) {
                val prevGap = previousIdea as? CreativeGapIdea
                val seedGap = seedPrompt as? CreativeGap

                val rawDiff = ideaObj?.optString("difficulty", prevGap?.difficulty?.name ?: seedGap?.difficulty?.name ?: "MEDIUM").orEmpty()
                val difficulty = try {
                    Difficulty.valueOf(rawDiff.uppercase())
                } catch (e: Exception) {
                    prevGap?.difficulty ?: seedGap?.difficulty ?: Difficulty.MEDIUM
                }

                var gapSentence = ideaObj?.optString("gapSentence", prevGap?.gapSentence ?: seedGap?.gapSentence.orEmpty())?.trim().orEmpty()
                if (gapSentence.isBlank()) {
                    val s = ideaObj?.optString("subject", "").orEmpty()
                    val env = ideaObj?.optString("environment", "").orEmpty()
                    gapSentence = if (s.isNotBlank() && env.isNotBlank()) {
                        "A $s discovers a mysterious ______ in $env."
                    } else if (s.isNotBlank()) {
                        "A $s exploring a strange realm with a missing ______."
                    } else {
                        "An intrepid traveler uncovers a glowing ______ in an uncharted realm."
                    }
                } else if (!gapSentence.contains("______") && !gapSentence.contains("___")) {
                    gapSentence = "$gapSentence ______"
                }

                val suggestions = mutableListOf<String>()
                val suggestionsArray = ideaObj?.optJSONArray("gapSuggestions") ?: ideaObj?.optJSONArray("suggestedFillIns")
                if (suggestionsArray != null) {
                    for (i in 0 until suggestionsArray.length()) {
                        val s = suggestionsArray.optString(i).trim()
                        if (s.isNotBlank()) suggestions.add(s)
                    }
                }
                val finalSuggestions = if (suggestions.isNotEmpty()) {
                    suggestions
                } else if (prevGap?.gapSuggestions?.isNotEmpty() == true) {
                    prevGap.gapSuggestions
                } else if (seedGap?.displayGapSuggestions?.isNotEmpty() == true) {
                    seedGap.displayGapSuggestions
                } else {
                    listOf("luminescent crystal", "mechanical heart", "living constellation", "forgotten key")
                }

                val style = ideaObj?.optString("style", prevGap?.style ?: seedGap?.style.orEmpty())?.trim().orEmpty()
                val challenge = ideaObj?.optString("challenge", prevGap?.challenge ?: seedGap?.challenge.orEmpty())?.trim().orEmpty()

                CreativeGapIdea(
                    difficulty = difficulty,
                    gapSentence = gapSentence,
                    gapSuggestions = finalSuggestions,
                    style = if (style.isNotBlank()) style else "Storybook Gouache",
                    challenge = if (challenge.isNotBlank()) challenge else "Harmonious warm lighting"
                )
            } else {
                val prevClassic = previousIdea as? ClassicSparkIdea
                val seedClassic = seedPrompt as? ClassicSpark

                val rawDiff = ideaObj?.optString("difficulty", prevClassic?.difficulty?.name ?: seedClassic?.difficulty?.name ?: "MEDIUM").orEmpty()
                val difficulty = try {
                    Difficulty.valueOf(rawDiff.uppercase())
                } catch (e: Exception) {
                    prevClassic?.difficulty ?: seedClassic?.difficulty ?: Difficulty.MEDIUM
                }

                val personality = ideaObj?.optString("personality", ideaObj.optString("trait", prevClassic?.personalityTrait ?: seedClassic?.personalityTrait.orEmpty()))?.trim().orEmpty()
                val subject = ideaObj?.optString("subject", prevClassic?.subjectCharacter ?: seedClassic?.subjectCharacter.orEmpty())?.trim().orEmpty()
                val scene = ideaObj?.optString("scene", ideaObj.optString("action", prevClassic?.actionSituationScene ?: seedClassic?.actionSituationScene.orEmpty()))?.trim().orEmpty()
                val environment = ideaObj?.optString("environment", prevClassic?.environment ?: seedClassic?.environment.orEmpty())?.trim().orEmpty()
                val atmosphere = ideaObj?.optString("atmosphere", prevClassic?.atmosphereWeather ?: seedClassic?.atmosphereWeather.orEmpty())?.trim().orEmpty()
                val style = ideaObj?.optString("style", prevClassic?.artStyle ?: seedClassic?.artStyle.orEmpty())?.trim().orEmpty()
                val challenge = ideaObj?.optString("challenge", prevClassic?.creativeChallenge ?: seedClassic?.creativeChallenge.orEmpty())?.trim().orEmpty()
                val storyHook = ideaObj?.optString("storyHook", prevClassic?.storyHook ?: seedClassic?.storyHook.orEmpty())?.trim().orEmpty()

                if (personality.isNotBlank() || subject.isNotBlank() || scene.isNotBlank() || environment.isNotBlank() || style.isNotBlank()) {
                    ClassicSparkIdea(
                        difficulty = difficulty,
                        personalityTrait = personality,
                        subjectCharacter = subject,
                        actionSituationScene = scene,
                        environment = environment,
                        atmosphereWeather = atmosphere,
                        artStyle = style,
                        creativeChallenge = challenge,
                        storyHook = storyHook
                    )
                } else {
                    prevClassic
                }
            }

            val detectedMode = structuredIdea?.promptType ?: if (isGap) PromptType.CREATIVE_GAP else PromptType.CLASSIC_SPARK

            GeminiResult.Success(
                replyText = reply,
                quickPills = quickPillsList,
                idea = structuredIdea,
                actionType = actionType,
                detectedPromptType = detectedMode
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Gemini output: $rawJson", e)
            val fallbackText = cleanJsonFence(rawJson).take(300)
            val defaultPills = when (promptType) {
                PromptType.CLASSIC_SPARK -> listOf("Make atmosphere darker", "Change action / scene", "Try another art style", "Increase difficulty")
                PromptType.CREATIVE_GAP -> listOf("Suggest twists for the blank", "Make it harder", "Suggest art style", "Give variations")
            }
            GeminiResult.Success(
                replyText = fallbackText,
                quickPills = defaultPills,
                idea = previousIdea,
                actionType = "CONVERSE",
                detectedPromptType = promptType
            )
        }
    }

    private fun cleanJsonFence(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json").trim()
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```").trim()
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```").trim()
        }
        return clean.trim()
    }
}
