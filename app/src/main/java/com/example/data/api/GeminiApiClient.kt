package com.example.data.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.BuildConfig
import com.example.model.ArtPrompt
import com.example.model.ArtSparkIdea
import com.example.model.BrainstormMessage
import com.example.model.MessageSender
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
        val idea: ArtSparkIdea?,
        val actionType: String
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

        private const val SYSTEM_INSTRUCTION = """
You are the AI creative brainstorming partner in ArtSpark — an Android application that inspires artists with playful, imaginative, and actionable art prompts.

Your goal is to have an encouraging, inspiring conversation with an artist to discover, develop, and refine an art idea they want to create.

CORE BEHAVIOR RULES:
1. Tone: Enthusiastic, supportive, playful, visual, and artist-friendly. Never patronizing or judgmental.
2. Brevity: Keep conversational text concise (2-4 sentences max). Never write huge walls of text.
3. Visual Focus: Suggest evocative visual details, lighting, poses, compositions, colors, and textures.
4. ArtSpark Categories: You are deeply integrated with the ArtSpark category system:
   - Subject: Central creature, person, object, or entity (e.g. "Frog", "Cyberpunk Samurai", "Ghost Florist")
   - Personality / Trait: Character mood, posture, or quirk (e.g. "Curious", "Sleepy", "Grumpy", "Heroic")
   - Action / Situation: What they are doing (e.g. "baking croissants", "reading ancient scrolls", "stargazing")
   - Environment: Setting, architecture, landscape (e.g. "Magical Bakery", "Overgrown Greenhouse", "Neon Rooftop")
   - Atmosphere / Weather: Lighting, mood, weather (e.g. "Moody candlelight", "Golden sunset", "Thunderstorm")
   - Art Style: Illustration medium/technique (e.g. "Storybook Watercolor", "Gouache & Ink", "90s Anime", "Chiaroscuro Oil")
   - Creative Challenge: Fun constraint (e.g. "3 colors only", "15 minute sketch", "Dramatic low angle", "Inverted values")
5. Continuous Context & Partial Modifications:
   - If the user modifies an aspect (e.g. "Make the character a dragon", "Make it darker", "Change the style to watercolor"), PRESERVE all other previous idea fields and ONLY update the requested category.
   - If the user references pronouns like "him", "her", or "it", refer to the current character/subject in conversation.
6. Quick Choice Pills:
   - ALWAYS provide 3-5 concise, clickable choice options in `quickPills` (e.g., ["Cute", "Weird", "Dark", "Epic"] or ["Watercolor", "Pixel Art", "Retro Comic"] or ["Add a companion", "Make it floating in space"]).
7. Output Format:
   - ALWAYS return valid JSON strictly conforming to this schema:
   {
     "reply": "Your concise, friendly, conversational reply (markdown allowed).",
     "quickPills": ["Choice 1", "Choice 2", "Choice 3", "Choice 4"],
     "hasIdea": true/false,
     "idea": {
       "subject": "...",
       "trait": "...",
       "action": "...",
       "environment": "...",
       "atmosphere": "...",
       "style": "...",
       "challenge": "..."
     },
     "actionType": "GENERATE_IDEA" | "UPDATE_CATEGORY" | "CREATE_VARIATIONS" | "SUGGEST_CHANGES" | "CONVERSE"
   }
   - When a viable, fun idea is present or updated, set `hasIdea: true` and populate `idea`.
   - If the idea is still very early and the user hasn't chosen a core direction yet, `hasIdea` can be false and `idea` can be null or empty.
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
            true // fallback
        }
    }

    suspend fun brainstorm(
        messages: List<BrainstormMessage>,
        currentIdea: ArtSparkIdea?,
        seedPrompt: ArtPrompt?
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
            val requestJson = buildRequestBody(messages, currentIdea, seedPrompt)

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

            parseGeminiResponse(responseBody, currentIdea)
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
        currentIdea: ArtSparkIdea?,
        seedPrompt: ArtPrompt?
    ): JSONObject {
        val root = JSONObject()

        // System Instruction
        val systemInstructionObj = JSONObject()
        val sysParts = JSONArray()
        var fullSysPrompt = SYSTEM_INSTRUCTION

        if (currentIdea != null && currentIdea.isComplete) {
            fullSysPrompt += "\n\nCURRENT WORKING IDEA IN MEMORY:\n" +
                    "- Subject: ${currentIdea.subject}\n" +
                    "- Trait: ${currentIdea.trait}\n" +
                    "- Action: ${currentIdea.action}\n" +
                    "- Environment: ${currentIdea.environment}\n" +
                    "- Atmosphere: ${currentIdea.atmosphere}\n" +
                    "- Style: ${currentIdea.style}\n" +
                    "- Challenge: ${currentIdea.challenge}\n"
        } else if (seedPrompt != null) {
            fullSysPrompt += "\n\nCURRENT SEED PROMPT FROM DISCOVER:\n" +
                    "- Subject: ${seedPrompt.subject}\n" +
                    "- Trait: ${seedPrompt.trait}\n" +
                    "- Action: ${seedPrompt.action}\n" +
                    "- Environment: ${seedPrompt.environment}\n" +
                    "- Atmosphere: ${seedPrompt.atmosphere}\n" +
                    "- Style: ${seedPrompt.style}\n" +
                    "- Challenge: ${seedPrompt.challenge}\n" +
                    "- Full Prompt: ${seedPrompt.narrativeText}\n"
        }

        sysParts.put(JSONObject().put("text", fullSysPrompt))
        systemInstructionObj.put("parts", sysParts)
        root.put("systemInstruction", systemInstructionObj)

        // Contents (conversation history)
        val contentsArray = JSONArray()

        // Filter valid user / AI messages (excluding errors)
        val validHistory = messages.filter { !it.isError }

        for (msg in validHistory) {
            val contentObj = JSONObject()
            val role = if (msg.sender == MessageSender.USER) "user" else "model"
            contentObj.put("role", role)

            val partsArray = JSONArray()
            val textContent = if (msg.sender == MessageSender.AI && msg.idea != null) {
                // If model message had structured idea, provide summary for LLM context
                "${msg.text}\n[Idea: Subject=${msg.idea.subject}, Trait=${msg.idea.trait}, Env=${msg.idea.environment}, Style=${msg.idea.style}]"
            } else {
                msg.text
            }
            partsArray.put(JSONObject().put("text", textContent))
            contentObj.put("parts", partsArray)

            contentsArray.put(contentObj)
        }

        // If history is empty, add a greeting prompt
        if (contentsArray.length() == 0) {
            val initialPrompt = if (seedPrompt != null) {
                "I want to brainstorm around my current prompt: \"${seedPrompt.narrativeText}\". How can we expand or twist this idea?"
            } else {
                "Hello! Let's brainstorm a new creative art idea."
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

    private fun parseGeminiResponse(rawJson: String, previousIdea: ArtSparkIdea?): GeminiResult {
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

            // Parse inner JSON returned by Gemini
            val cleanedJsonText = cleanJsonFence(text)
            val parsedObj = JSONObject(cleanedJsonText)

            val reply = parsedObj.optString("reply", "Here's an idea for your artwork!").trim()
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

            val hasIdea = parsedObj.optBoolean("hasIdea", false)
            val ideaObj = parsedObj.optJSONObject("idea")

            val structuredIdea = if (hasIdea && ideaObj != null) {
                val baseSubj = ideaObj.optString("subject", previousIdea?.subject.orEmpty()).trim()
                val baseTrait = ideaObj.optString("trait", previousIdea?.trait.orEmpty()).trim()
                val baseAction = ideaObj.optString("action", previousIdea?.action.orEmpty()).trim()
                val baseEnv = ideaObj.optString("environment", previousIdea?.environment.orEmpty()).trim()
                val baseAtm = ideaObj.optString("atmosphere", previousIdea?.atmosphere.orEmpty()).trim()
                val baseStyle = ideaObj.optString("style", previousIdea?.style.orEmpty()).trim()
                val baseChallenge = ideaObj.optString("challenge", previousIdea?.challenge.orEmpty()).trim()

                ArtSparkIdea(
                    subject = baseSubj,
                    trait = baseTrait,
                    action = baseAction,
                    environment = baseEnv,
                    atmosphere = baseAtm,
                    style = baseStyle,
                    challenge = baseChallenge
                )
            } else if (ideaObj != null && (ideaObj.has("subject") || ideaObj.has("environment"))) {
                // If idea fields were returned even if hasIdea wasn't explicitly true
                ArtSparkIdea(
                    subject = ideaObj.optString("subject", previousIdea?.subject.orEmpty()).trim(),
                    trait = ideaObj.optString("trait", previousIdea?.trait.orEmpty()).trim(),
                    action = ideaObj.optString("action", previousIdea?.action.orEmpty()).trim(),
                    environment = ideaObj.optString("environment", previousIdea?.environment.orEmpty()).trim(),
                    atmosphere = ideaObj.optString("atmosphere", previousIdea?.atmosphere.orEmpty()).trim(),
                    style = ideaObj.optString("style", previousIdea?.style.orEmpty()).trim(),
                    challenge = ideaObj.optString("challenge", previousIdea?.challenge.orEmpty()).trim()
                )
            } else {
                previousIdea
            }

            GeminiResult.Success(
                replyText = reply,
                quickPills = quickPillsList,
                idea = structuredIdea,
                actionType = actionType
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Gemini output: $rawJson", e)
            // Fallback: Return raw text as reply with default pills
            val fallbackText = cleanJsonFence(rawJson).take(300)
            GeminiResult.Success(
                replyText = fallbackText,
                quickPills = listOf("Make it cute", "Add mystery", "Change setting", "Surprise me"),
                idea = previousIdea,
                actionType = "CONVERSE"
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
