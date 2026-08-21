const express = require("express");
const router = express.Router();
const { generateContentWithGemini, safeParseJson, getApiKey } = require("../services/geminiService");

/**
 * POST /api/improvePrompt
 * Suggests punchy artistic enhancements and atmospheric additions.
 */
router.post("/", async (req, res) => {
  try {
    const { promptText = "", style = "", difficulty = "MEDIUM" } = req.body || {};

    if (!getApiKey()) {
      return res.status(503).json({
        success: false,
        error: "Brainstorm AI is temporarily unavailable."
      });
    }

    const systemInstruction = `You are an expert art director. Improve the given art prompt by enhancing its visual textures, lighting atmosphere, and composition. Return valid JSON: { "improvedPrompt": "...", "addedElements": ["..."], "suggestedPalette": ["..."] }`;

    const raw = await generateContentWithGemini({
      systemInstruction,
      contents: [{ role: "user", text: `Prompt: "${promptText}" | Style: "${style}" | Difficulty: ${difficulty}` }]
    });

    const parsed = safeParseJson(raw);
    res.status(200).json({ success: true, ...parsed });
  } catch (error) {
    console.error("Error in /improvePrompt:", error);
    res.status(500).json({ success: false, error: "Unable to improve prompt at this time." });
  }
});

module.exports = router;
