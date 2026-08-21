const { generateContentWithGemini, safeParseJson } = require("../services/geminiService");

const CREATIVE_GAP_SYSTEM_PROMPT = `
You are the AI creative brainstorming partner in ArtSpark — an Android application that inspires artists with playful, imaginative, and actionable art prompts.

You are currently handling CREATIVE GAP prompts (Fill-In-The-Blank):
- gapSentence: An evocative sentence containing \`______\` as the blank to be filled in (e.g. "A lonely automaton searches for a missing ______ in the neon fog.").
- gapSuggestions: At least 3 creative, distinct idea starters for what could go in the blank.
- style: Suggested art medium / rendering style (e.g. "Gouache on textured paper", "Risograph Print").
- challenge: Creative drawing constraint (e.g. "Use only warm light sources", "Focus on silhouette").
- difficulty: "EASY", "MEDIUM", or "HARD".

RULES:
1. ALWAYS KEEP THE BLANK (\`______\`) in \`gapSentence\`. Do NOT remove or fill in the blank in \`gapSentence\`.
2. Provide 3-5 distinct, imaginative \`gapSuggestions\` for what artists might choose to draw in the blank.
3. When asked to make it harder / increase difficulty, set difficulty to "HARD" and introduce an intriguing constraint. When asked to simplify / make easier, set difficulty to "EASY".
4. Always include 3-5 concise, clickable options in \`quickPills\` that offer relevant next steps or twists.
5. Tone: Enthusiastic, playful, artist-friendly, and concise (2-3 sentences max).
6. Output format must strictly match this JSON schema:
{
  "reply": "Concise conversational reply (markdown allowed).",
  "quickPills": ["Choice 1", "Choice 2", "Choice 3", "Choice 4"],
  "hasIdea": true,
  "ideaType": "CREATIVE_GAP",
  "idea": {
    "difficulty": "EASY" | "MEDIUM" | "HARD",
    "gapSentence": "Sentence containing ______",
    "gapSuggestions": ["Starter 1", "Starter 2", "Starter 3", "Starter 4"],
    "style": "...",
    "challenge": "..."
  },
  "actionType": "GENERATE_IDEA" | "UPDATE_CATEGORY" | "CREATE_VARIATIONS" | "SUGGEST_CHANGES" | "CONVERSE"
}
`;

async function handleCreativeGapBrainstorm({
  difficulty = "MEDIUM",
  promptData = {},
  conversationHistory = [],
  userMessage = ""
}) {
  const currentSentence = promptData.gapSentence || promptData.narrativeText || promptData.sentence || "";
  const currentSuggestions = Array.isArray(promptData.gapSuggestions) ? promptData.gapSuggestions : [];
  const currentStyle = promptData.style || promptData.artStyle || "";
  const currentChallenge = promptData.challenge || promptData.creativeChallenge || "";
  const normalizedDiff = (difficulty || promptData.difficulty || "MEDIUM").toUpperCase();

  let contextSnippet = `\n\nACTIVE CONTEXT (Difficulty: ${normalizedDiff}):\n` +
    `• Gap Sentence: "${currentSentence}"\n` +
    `• Suggestions: ${currentSuggestions.join(", ")}\n` +
    `• Style: ${currentStyle}\n` +
    `• Challenge: ${currentChallenge}\n`;

  const systemInstruction = CREATIVE_GAP_SYSTEM_PROMPT + contextSnippet;

  // Build contents
  const contents = [];
  for (const item of conversationHistory) {
    const text = item.text || item.message || "";
    if (text) {
      contents.push({
        sender: item.sender || (item.role === "model" ? "AI" : "USER"),
        text
      });
    }
  }

  // Add latest user message if present
  if (userMessage) {
    contents.push({
      sender: "USER",
      text: userMessage
    });
  }

  if (contents.length === 0) {
    const starterPrompt = currentSentence
      ? `Let's explore this Creative Gap prompt:\n"${currentSentence}"\nWhat are some imaginative ideas for the blank, art style, and constraints?`
      : "Hello! Let's brainstorm a fresh Creative Gap fill-in-the-blank prompt.";
    contents.push({
      sender: "USER",
      text: starterPrompt
    });
  }

  const rawJson = await generateContentWithGemini({
    systemInstruction,
    contents
  });

  const parsed = safeParseJson(rawJson);
  const ideaObj = parsed.idea || {};

  const diffStr = (ideaObj.difficulty || normalizedDiff || "MEDIUM").toUpperCase();
  const validDiff = ["EASY", "MEDIUM", "HARD"].includes(diffStr) ? diffStr : "MEDIUM";

  let gapSentence = (ideaObj.gapSentence || currentSentence || "").trim();
  if (!gapSentence) {
    gapSentence = "An intrepid explorer uncovers a glowing ______ in an uncharted realm.";
  } else if (!gapSentence.includes("______") && !gapSentence.includes("___")) {
    gapSentence = `${gapSentence} ______`;
  }

  let suggestions = Array.isArray(ideaObj.gapSuggestions) ? ideaObj.gapSuggestions.filter(Boolean) : [];
  if (suggestions.length === 0 && currentSuggestions.length > 0) {
    suggestions = currentSuggestions;
  }
  if (suggestions.length === 0) {
    suggestions = ["luminescent crystal", "mechanical heart", "living constellation", "forgotten key"];
  }

  const style = (ideaObj.style || currentStyle || "Storybook Gouache").trim();
  const challenge = (ideaObj.challenge || currentChallenge || "Harmonious warm lighting").trim();

  return {
    reply: parsed.reply || "Here is a fresh Creative Gap concept!",
    quickPills: Array.isArray(parsed.quickPills) && parsed.quickPills.length > 0
      ? parsed.quickPills
      : ["Suggest twists for the blank", "Make it harder", "Suggest art style", "Give variations"],
    actionType: parsed.actionType || "GENERATE_IDEA",
    promptType: "CreativeGap",
    idea: {
      promptType: "CreativeGap",
      difficulty: validDiff,
      gapSentence,
      gapSuggestions: suggestions,
      style,
      challenge
    }
  };
}

module.exports = {
  handleCreativeGapBrainstorm
};
