const express = require("express");
const router = express.Router();
const { generateContentWithGemini, safeParseJson, getApiKey } = require("../services/geminiService");

/**
 * POST /api/generateStoryHooks
 * Generates engaging 1-sentence narrative sparks for an art piece.
 */
router.post("/", async (req, res) => {
  try {
    const { subject = "", scene = "", environment = "" } = req.body || {};

    if (!getApiKey()) {
      return res.status(503).json({
        success: false,
        error: "Brainstorm AI is temporarily unavailable."
      });
    }

    const systemInstruction = `You are a creative storyteller. Generate 3 short, intriguing 1-sentence story hooks for this scene. Return valid JSON: { "storyHooks": ["Hook 1", "Hook 2", "Hook 3"] }`;

    const raw = await generateContentWithGemini({
      systemInstruction,
      contents: [{ role: "user", text: `Subject: ${subject}, Scene: ${scene}, Environment: ${environment}` }]
    });

    const parsed = safeParseJson(raw);
    res.status(200).json({ success: true, ...parsed });
  } catch (error) {
    console.error("Error in /generateStoryHooks:", error);
    res.status(500).json({ success: false, error: "Unable to generate story hooks." });
  }
});

module.exports = router;
