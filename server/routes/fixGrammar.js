const express = require("express");
const router = express.Router();
const { generateContentWithGemini, safeParseJson, getApiKey } = require("../services/geminiService");

/**
 * POST /api/fixGrammar
 * Polishes custom entered user prompt text with proper phrasing and punctuation.
 */
router.post("/", async (req, res) => {
  try {
    const { text = "" } = req.body || {};

    if (!getApiKey()) {
      return res.status(503).json({
        success: false,
        error: "Brainstorm AI is temporarily unavailable."
      });
    }

    const systemInstruction = `You are an editor for art prompt descriptions. Correct grammar, naturalize phrasing, and preserve the creative spirit. Return valid JSON: { "correctedText": "..." }`;

    const raw = await generateContentWithGemini({
      systemInstruction,
      contents: [{ role: "user", text: `Text: "${text}"` }]
    });

    const parsed = safeParseJson(raw);
    res.status(200).json({ success: true, ...parsed });
  } catch (error) {
    console.error("Error in /fixGrammar:", error);
    res.status(500).json({ success: false, error: "Unable to fix grammar." });
  }
});

module.exports = router;
