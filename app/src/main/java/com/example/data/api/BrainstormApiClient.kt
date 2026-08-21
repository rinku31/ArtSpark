package com.example.data.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
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

// Backward compatibility alias for any existing references
typealias GeminiResult = BrainstormResult
typealias GeminiApiClient = BrainstormApiClient

/**
 * Client for ArtSpark's Brainstorm AI backend service.
 * Communicates strictly with the backend endpoint — no Gemini API keys are embedded or accessed on the client.
 */
class BrainstormApiClient(private val context: Context) {

    companion object {
        private const val TAG = "BrainstormApiClient"

        // Backend endpoints: primary cloud endpoint with local emulator fallback
        private const val PRIMARY_BACKEND_URL = "https://ais-dev-xpmdcceuhrklfrr35nys5b-873705439101.asia-east1.run.app/api/brainstorm"
        private const val LOCAL_EMULATOR_URL = "http://10.0.2.2:8080/api/brainstorm"
        private const val LOCALHOST_URL = "http://localhost:8080/api/brainstorm"

        private const val ERROR_OFFLINE = "Unable to contact Brainstorm AI. Please check your internet connection."
        private const val ERROR_UNAVAILABLE = "Brainstorm AI is temporarily unavailable."
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
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

    /**
     * Sends a brainstorm request to the server runtime.
     */
    suspend fun brainstorm(
        messages: List<BrainstormMessage>,
        currentIdea: BrainstormIdea?,
        seedPrompt: DiscoverPrompt?,
        promptType: PromptType
    ): BrainstormResult = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            return@withContext BrainstormResult.Error(
                message = ERROR_OFFLINE,
                isOffline = true
            )
        }

        val requestPayload = buildRequestJson(messages, currentIdea, seedPrompt, promptType)
        val body = requestPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        // Try primary endpoint first, then local fallback if primary fails with network error
        val endpointsToTry = listOf(PRIMARY_BACKEND_URL, LOCAL_EMULATOR_URL, LOCALHOST_URL)

        var lastErrorMessage = ERROR_UNAVAILABLE
        var isNetworkError = false

        for (endpoint in endpointsToTry) {
            try {
                val request = Request.Builder()
                    .url(endpoint)
                    .post(body)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    return@withContext parseBackendResponse(responseBody, promptType, currentIdea, seedPrompt)
                }

                if (!responseBody.isNullOrBlank()) {
                    try {
                        val errorJson = JSONObject(responseBody)
                        val msg = errorJson.optString("message", errorJson.optString("error", ERROR_UNAVAILABLE))
                        if (msg.isNotBlank()) {
                            lastErrorMessage = msg
                        }
                    } catch (e: Exception) {
                        lastErrorMessage = "Brainstorm AI responded with status ${response.code}."
                    }
                }
            } catch (e: IOException) {
                isNetworkError = true
                Log.w(TAG, "Failed connecting to endpoint $endpoint: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error executing request on $endpoint", e)
                lastErrorMessage = ERROR_UNAVAILABLE
            }
        }

        if (!isOnline() || isNetworkError) {
            BrainstormResult.Error(
                message = ERROR_OFFLINE,
                isOffline = !isOnline()
            )
        } else {
            BrainstormResult.Error(message = lastErrorMessage)
        }
    }

    private fun buildRequestJson(
        messages: List<BrainstormMessage>,
        currentIdea: BrainstormIdea?,
        seedPrompt: DiscoverPrompt?,
        promptType: PromptType
    ): JSONObject {
        val root = JSONObject()

        val pTypeStr = when (promptType) {
            PromptType.CREATIVE_GAP -> "CreativeGap"
            PromptType.CLASSIC_SPARK -> "ClassicSpark"
        }
        root.put("promptType", pTypeStr)

        val activeDiff = currentIdea?.difficulty ?: seedPrompt?.difficulty ?: Difficulty.MEDIUM
        root.put("difficulty", activeDiff.name)

        // Structured promptData
        val promptData = JSONObject()
        when (promptType) {
            PromptType.CLASSIC_SPARK -> {
                val classicIdea = currentIdea as? ClassicSparkIdea
                val classicSeed = seedPrompt as? ClassicSpark
                promptData.put("personalityTrait", classicIdea?.personalityTrait ?: classicSeed?.personalityTrait.orEmpty())
                promptData.put("subjectCharacter", classicIdea?.subjectCharacter ?: classicSeed?.subjectCharacter.orEmpty())
                promptData.put("actionSituationScene", classicIdea?.actionSituationScene ?: classicSeed?.actionSituationScene.orEmpty())
                promptData.put("environment", classicIdea?.environment ?: classicSeed?.environment.orEmpty())
                promptData.put("atmosphereWeather", classicIdea?.atmosphereWeather ?: classicSeed?.atmosphereWeather.orEmpty())
                promptData.put("artStyle", classicIdea?.artStyle ?: classicSeed?.artStyle.orEmpty())
                promptData.put("creativeChallenge", classicIdea?.creativeChallenge ?: classicSeed?.creativeChallenge.orEmpty())
                promptData.put("storyHook", classicIdea?.storyHook ?: classicSeed?.storyHook.orEmpty())
            }
            PromptType.CREATIVE_GAP -> {
                val gapIdea = currentIdea as? CreativeGapIdea
                val gapSeed = seedPrompt as? CreativeGap
                promptData.put("gapSentence", gapIdea?.gapSentence ?: gapSeed?.gapSentence.orEmpty())
                val suggestions = gapIdea?.gapSuggestions ?: gapSeed?.displayGapSuggestions ?: emptyList()
                val suggestionsArray = JSONArray()
                for (s in suggestions) suggestionsArray.put(s)
                promptData.put("gapSuggestions", suggestionsArray)
                promptData.put("style", gapIdea?.style ?: gapSeed?.style.orEmpty())
                promptData.put("challenge", gapIdea?.challenge ?: gapSeed?.challenge.orEmpty())
            }
        }
        root.put("promptData", promptData)

        // Conversation history
        val historyArray = JSONArray()
        val validHistory = messages.filter { !it.isError }
        for (msg in validHistory) {
            val msgObj = JSONObject()
            msgObj.put("sender", if (msg.sender == MessageSender.USER) "USER" else "AI")
            msgObj.put("text", msg.text)
            historyArray.put(msgObj)
        }
        root.put("conversationHistory", historyArray)

        val latestUserMsg = messages.lastOrNull { it.sender == MessageSender.USER }?.text.orEmpty()
        root.put("userMessage", latestUserMsg)

        return root
    }

    private fun parseBackendResponse(
        responseBody: String,
        fallbackType: PromptType,
        previousIdea: BrainstormIdea?,
        seedPrompt: DiscoverPrompt?
    ): BrainstormResult {
        return try {
            val root = JSONObject(responseBody)
            val isSuccess = root.optBoolean("success", true)

            if (!isSuccess) {
                val errMsg = root.optString("message", root.optString("error", ERROR_UNAVAILABLE))
                return BrainstormResult.Error(message = errMsg)
            }

            val reply = root.optString("reply", "Here is a fresh creative idea!").trim()
            val actionType = root.optString("actionType", "GENERATE_IDEA")

            val quickPillsList = mutableListOf<String>()
            val pillsArray = root.optJSONArray("quickPills")
            if (pillsArray != null) {
                for (i in 0 until pillsArray.length()) {
                    val pill = pillsArray.optString(i).trim()
                    if (pill.isNotBlank()) {
                        quickPillsList.add(pill)
                    }
                }
            }

            val rawPromptType = root.optString("promptType", "").uppercase()
            val ideaObj = root.optJSONObject("idea")

            val isGap = when {
                rawPromptType.contains("GAP") -> true
                rawPromptType.contains("CLASSIC") -> false
                ideaObj != null && ideaObj.has("gapSentence") && !ideaObj.has("subjectCharacter") && !ideaObj.has("subject") -> true
                ideaObj != null && (ideaObj.has("subjectCharacter") || ideaObj.has("personalityTrait") || ideaObj.has("subject")) -> false
                else -> fallbackType == PromptType.CREATIVE_GAP
            }

            val structuredIdea: BrainstormIdea? = if (isGap) {
                val prevGap = previousIdea as? CreativeGapIdea
                val seedGap = seedPrompt as? CreativeGap

                val diffStr = ideaObj?.optString("difficulty", prevGap?.difficulty?.name ?: seedGap?.difficulty?.name ?: "MEDIUM").orEmpty()
                val difficulty = try {
                    Difficulty.valueOf(diffStr.uppercase())
                } catch (e: Exception) {
                    prevGap?.difficulty ?: seedGap?.difficulty ?: Difficulty.MEDIUM
                }

                var gapSentence = ideaObj?.optString("gapSentence", prevGap?.gapSentence ?: seedGap?.gapSentence.orEmpty())?.trim().orEmpty()
                if (gapSentence.isBlank()) {
                    gapSentence = "An intrepid explorer uncovers a glowing ______ in an uncharted realm."
                } else if (!gapSentence.contains("______") && !gapSentence.contains("___")) {
                    gapSentence = "$gapSentence ______"
                }

                val suggestions = mutableListOf<String>()
                val suggestionsArray = ideaObj?.optJSONArray("gapSuggestions")
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

                val diffStr = ideaObj?.optString("difficulty", prevClassic?.difficulty?.name ?: seedClassic?.difficulty?.name ?: "MEDIUM").orEmpty()
                val difficulty = try {
                    Difficulty.valueOf(diffStr.uppercase())
                } catch (e: Exception) {
                    prevClassic?.difficulty ?: seedClassic?.difficulty ?: Difficulty.MEDIUM
                }

                val personality = ideaObj?.optString("personalityTrait", ideaObj.optString("personality", prevClassic?.personalityTrait ?: seedClassic?.personalityTrait.orEmpty()))?.trim().orEmpty()
                val subject = ideaObj?.optString("subjectCharacter", ideaObj.optString("subject", prevClassic?.subjectCharacter ?: seedClassic?.subjectCharacter.orEmpty()))?.trim().orEmpty()
                val scene = ideaObj?.optString("actionSituationScene", ideaObj.optString("scene", prevClassic?.actionSituationScene ?: seedClassic?.actionSituationScene.orEmpty()))?.trim().orEmpty()
                val environment = ideaObj?.optString("environment", prevClassic?.environment ?: seedClassic?.environment.orEmpty())?.trim().orEmpty()
                val atmosphere = ideaObj?.optString("atmosphereWeather", ideaObj.optString("atmosphere", prevClassic?.atmosphereWeather ?: seedClassic?.atmosphereWeather.orEmpty()))?.trim().orEmpty()
                val style = ideaObj?.optString("artStyle", ideaObj.optString("style", prevClassic?.artStyle ?: seedClassic?.artStyle.orEmpty()))?.trim().orEmpty()
                val challenge = ideaObj?.optString("creativeChallenge", ideaObj.optString("challenge", prevClassic?.creativeChallenge ?: seedClassic?.creativeChallenge.orEmpty()))?.trim().orEmpty()
                val storyHook = ideaObj?.optString("storyHook", prevClassic?.storyHook ?: seedClassic?.storyHook.orEmpty())?.trim().orEmpty()
                val generatedSentence = ideaObj?.optString("generatedSentence", "").orEmpty().trim()

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
                        storyHook = storyHook,
                        generatedSentence = generatedSentence
                    )
                } else {
                    prevClassic
                }
            }

            val detectedMode = structuredIdea?.promptType ?: if (isGap) PromptType.CREATIVE_GAP else PromptType.CLASSIC_SPARK

            BrainstormResult.Success(
                replyText = reply,
                quickPills = if (quickPillsList.isNotEmpty()) quickPillsList else listOf("Make atmosphere darker", "Change action / scene", "Try another art style", "Increase difficulty"),
                idea = structuredIdea,
                actionType = actionType,
                detectedPromptType = detectedMode
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing server response: $responseBody", e)
            BrainstormResult.Error(message = ERROR_UNAVAILABLE)
        }
    }
}
