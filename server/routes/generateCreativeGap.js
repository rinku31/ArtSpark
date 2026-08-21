const express = require("express");
const router = express.Router();
const { handleCreativeGapBrainstorm } = require("../strategies/creativeGapStrategy");

/**
 * POST /api/generateCreativeGap
 * Dedicated endpoint for generating brand-new Creative Gap prompts.
 */
router.post("/", async (req, res) => {
  try {
    const { difficulty = "MEDIUM", theme = "", userMessage = "" } = req.body || {};

    const result = await handleCreativeGapBrainstorm({
      difficulty,
      promptData: {
        sentence: theme ? `An artwork featuring ${theme} and a mysterious ______` : ""
      },
      conversationHistory: [],
      userMessage: userMessage || `Generate a new inventive Creative Gap prompt with theme: ${theme || "fantasy/sci-fi"}`
    });

    res.status(200).json({ success: true, ...result });
  } catch (error) {
    console.error("Error in /generateCreativeGap:", error);
    res.status(500).json({ success: false, error: "Unable to generate Creative Gap prompt." });
  }
});

module.exports = router;
