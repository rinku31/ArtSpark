const express = require("express");
const cors = require("cors");
require("dotenv").config();

const brainstormRouter = require("./server/routes/brainstorm");
const improvePromptRouter = require("./server/routes/improvePrompt");
const generateStoryHooksRouter = require("./server/routes/generateStoryHooks");
const generateCreativeGapRouter = require("./server/routes/generateCreativeGap");
const fixGrammarRouter = require("./server/routes/fixGrammar");

const app = express();
const PORT = process.env.PORT || 8080;

app.use(cors());
app.use(express.json({ limit: "5mb" }));

// Health Check
app.get(["/health", "/api/health"], (req, res) => {
  res.status(200).json({
    status: "ok",
    service: "artspark-backend",
    hasApiKey: Boolean(process.env.GEMINI_API_KEY || process.env.API_KEY),
    timestamp: new Date().toISOString()
  });
});

// Mount Routes (supporting both /api/* and direct /* prefixes)
app.use("/api/brainstorm", brainstormRouter);
app.use("/brainstorm", brainstormRouter);

app.use("/api/improvePrompt", improvePromptRouter);
app.use("/improvePrompt", improvePromptRouter);

app.use("/api/generateStoryHooks", generateStoryHooksRouter);
app.use("/generateStoryHooks", generateStoryHooksRouter);

app.use("/api/generateCreativeGap", generateCreativeGapRouter);
app.use("/generateCreativeGap", generateCreativeGapRouter);

app.use("/api/fixGrammar", fixGrammarRouter);
app.use("/fixGrammar", fixGrammarRouter);

// 404 Handler
app.use((req, res) => {
  res.status(404).json({
    success: false,
    error: "Not Found",
    path: req.originalUrl
  });
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error("Unhandled server error:", err);
  res.status(500).json({
    success: false,
    error: "Brainstorm AI is temporarily unavailable.",
    message: err.message || "Internal server error"
  });
});

// Start listening when executed directly
if (require.main === module) {
  app.listen(PORT, "0.0.0.0", () => {
    console.log(`ArtSpark Brainstorm backend running on port ${PORT}`);
  });
}

module.exports = app;
