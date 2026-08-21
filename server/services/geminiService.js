const { GoogleGenerativeAI } = require("@google/generative-ai");
require("dotenv").config();

// Allowed models in priority order
const PRIMARY_MODEL = "gemini-2.5-flash";
const FALLBACK_MODEL = "gemini-2.5-flash";

function getApiKey() {
  return process.env.GEMINI_API_KEY || process.env.API_KEY || "";
}

/**
 * Executes a structured content generation call to Gemini with server-side credentials.
 */
async function generateContentWithGemini({
  systemInstruction,
  contents,
  temperature = 0.7,
  responseMimeType = "application/json"
}) {
  const apiKey = getApiKey();
  if (!apiKey) {
    throw new Error("Server-side GEMINI_API_KEY is not configured.");
  }

  const genAI = new GoogleGenerativeAI(apiKey);

  const model = genAI.getGenerativeModel({
    model: PRIMARY_MODEL,
    systemInstruction: systemInstruction ? { parts: [{ text: systemInstruction }] } : undefined,
    generationConfig: {
      temperature,
      responseMimeType
    }
  });

  const formattedContents = contents.map((c) => {
    const role = c.role === "user" || c.sender === "USER" ? "user" : "model";
    const text = typeof c.text === "string" ? c.text : JSON.stringify(c);
    return {
      role,
      parts: [{ text }]
    };
  });

  const result = await model.generateContent({
    contents: formattedContents
  });

  const response = await result.response;
  const rawText = response.text();

  return rawText;
}

/**
 * Utility to clean markdown fences if present
 */
function cleanJsonFence(raw) {
  if (!raw) return "";
  let clean = raw.trim();
  if (clean.startsWith("```json")) {
    clean = clean.slice(7).trim();
  } else if (clean.startsWith("```")) {
    clean = clean.slice(3).trim();
  }
  if (clean.endsWith("```")) {
    clean = clean.slice(0, -3).trim();
  }
  return clean.trim();
}

/**
 * Safely parse JSON from raw text
 */
function safeParseJson(raw) {
  const cleaned = cleanJsonFence(raw);
  return JSON.parse(cleaned);
}

module.exports = {
  getApiKey,
  generateContentWithGemini,
  cleanJsonFence,
  safeParseJson
};
