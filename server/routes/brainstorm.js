const express = require("express");
const router = express.Router();
const { handleClassicSparkBrainstorm } = require("../strategies/classicSparkStrategy");
const { handleCreativeGapBrainstorm } = require("../strategies/creativeGapStrategy");
const { getApiKey } = require("../services/geminiService");

/**
 * POST /api/brainstorm & /brainstorm
 * Handles unified Brainstorm requests for ClassicSpark and CreativeGap.
 */
router.post("/", async (req, res) => {
  try {
    const {
      promptType = "ClassicSpark",
      difficulty = "Medium",
      promptData = {},
      conversationHistory = [],
      userMessage = ""
    } = req.body || {};

    if (!getApiKey()) {
      return res.status(503).json({
        success: false,
        error: "Brainstorm AI is temporarily unavailable.",
        message: "Server-side GEMINI_API_KEY is not configured on the backend."
      });
    }

    // Determine prompt type
    const normalizedType = String(promptType).toLowerCase().replace(/[\s_-]/g, "");
    const isCreativeGap = normalizedType.includes("gap") || normalizedType.includes("creativegap") ||
      (promptData && promptData.gapSentence && !promptData.subjectCharacter);

    let result;
    if (isCreativeGap) {
      result = await handleCreativeGapBrainstorm({
        difficulty,
        promptData,
        conversationHistory,
        userMessage
      });
    } else {
      result = await handleClassicSparkBrainstorm({
        difficulty,
        promptData,
        conversationHistory,
        userMessage
      });
    }

    return res.status(200).json({
      success: true,
      ...result
    });
  } catch (error) {
    console.error("Error in /brainstorm endpoint:", error);
    const isNetworkOrQuota = error.message && (error.message.includes("quota") || error.message.includes("RESOURCE_EXHAUSTED") || error.message.includes("ENOTFOUND"));
    return res.status(500).json({
      success: false,
      error: "Brainstorm AI is temporarily unavailable.",
      message: isNetworkOrQuota
        ? "Brainstorm AI is temporarily busy. Please try again in a moment."
        : "Something went wrong while processing your request. Please try again."
    });
  }
});

module.exports = router;
