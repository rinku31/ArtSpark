const { generateContentWithGemini, safeParseJson } = require("../services/geminiService");

const CLASSIC_SPARK_SYSTEM_PROMPT = `
You are the AI creative brainstorming partner in ArtSpark — an Android application that inspires artists with playful, imaginative, and actionable art prompts.

You are currently handling CLASSIC SPARK prompts (7 Structured Categories):
1. personality: Character mood, posture, personality, or quirk (e.g. "Curious", "Sleepy", "Heroic").
2. subject: Central creature, character, figure, object, or entity (e.g. "Cyberpunk Samurai", "Overgrown Golem", "Little Fox Explorer").
3. scene: What they are doing, the situation, or dynamic scene (e.g. "baking glowing pastries", "repairing a clockwork dragon").
4. environment: Setting, terrain, or architecture (e.g. "Sunken Crystal Library", "Neon Rooftop").
5. atmosphere: Lighting, weather, time of day, mood (e.g. "Moody candlelight and rain", "Golden sunset glow").
6. style: Artistic medium, rendering technique (e.g. "Storybook Watercolor", "Gouache & Ink", "90s Anime").
7. challenge: Creative drawing constraint (e.g. "Use 3 colors only", "15 minute sketch").
- storyHook: (optional) 1-sentence narrative spark.
- difficulty: "EASY", "MEDIUM", or "HARD".

RULES:
1. Provide all 7 categories (personality, subject, scene, environment, atmosphere, style, challenge).
2. When asked to make it harder / increase difficulty, set difficulty to "HARD" and introduce an intriguing constraint. When asked to simplify / make easier, set difficulty to "EASY".
3. Always include 3-5 concise, clickable options in quickPills that offer relevant next steps or twists.
4. Tone: Enthusiastic, playful, artist-friendly, and concise (2-3 sentences max).
5. Output format must strictly match this JSON schema:
{
  "reply": "Concise conversational reply (markdown allowed).",
  "quickPills": ["Choice 1", "Choice 2", "Choice 3", "Choice 4"],
  "hasIdea": true,
  "ideaType": "CLASSIC_SPARK",
  "idea": {
    "difficulty": "EASY" | "MEDIUM" | "HARD",
    "personality": "...",
    "subject": "...",
    "scene": "...",
    "environment": "...",
    "atmosphere": "...",
    "style": "...",
    "challenge": "...",
    "storyHook": "..."
  },
  "actionType": "GENERATE_IDEA" | "UPDATE_CATEGORY" | "CREATE_VARIATIONS" | "SUGGEST_CHANGES" | "CONVERSE"
}
`;

function buildFullSentence(personality, subject, scene, environment, atmosphere, style, challenge) {
  const parts = [];
  const subjectPart = personality ? `${personality} ${subject}` : subject;
  if (subjectPart) parts.push(subjectPart);
  if (scene) parts.push(scene);
  if (environment) parts.push(`in ${environment}`);
  if (atmosphere) parts.push(`with ${atmosphere}`);
  if (style) parts.push(`in ${style} style`);
  if (challenge) parts.push(`(${challenge})`);
  return parts.join(" ");
}

async function handleClassicSparkBrainstorm({
  difficulty = "MEDIUM",
  promptData = {},
  conversationHistory = [],
  userMessage = ""
}) {
  const currentPersonality = promptData.personalityTrait || promptData.personality || promptData.trait || "";
  const currentSubject = promptData.subjectCharacter || promptData.subject || "";
  const currentScene = promptData.actionSituationScene || promptData.scene || promptData.action || "";
  const currentEnv = promptData.environment || "";
  const currentAtm = promptData.atmosphereWeather || promptData.atmosphere || "";
  const currentStyle = promptData.artStyle || promptData.style || "";
  const currentChallenge = promptData.creativeChallenge || promptData.challenge || "";
  const currentStory = promptData.storyHook || "";
  const normalizedDiff = (difficulty || promptData.difficulty || "MEDIUM").toUpperCase();

  let contextSnippet = `\n\nACTIVE CONTEXT (Difficulty: ${normalizedDiff}):\n` +
    `• Personality/Trait: ${currentPersonality}\n` +
    `• Subject/Character: ${currentSubject}\n` +
    `• Scene/Action: ${currentScene}\n` +
    `• Environment: ${currentEnv}\n` +
    `• Atmosphere/Weather: ${currentAtm}\n` +
    `• Art Style: ${currentStyle}\n` +
    `• Creative Challenge: ${currentChallenge}\n`;

  const systemInstruction = CLASSIC_SPARK_SYSTEM_PROMPT + contextSnippet;

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

  // Add the latest user message if provided
  if (userMessage) {
    contents.push({
      sender: "USER",
      text: userMessage
    });
  }

  if (contents.length === 0) {
    const starterPrompt = currentSubject
      ? `Let's explore this Classic Spark:\nPersonality: ${currentPersonality}\nSubject: ${currentSubject}\nScene: ${currentScene}\nEnvironment: ${currentEnv}\nAtmosphere: ${currentAtm}\nStyle: ${currentStyle}\nChallenge: ${currentChallenge}\nWhat can we refine or develop?`
      : "Hello! Let's brainstorm a fresh Classic Spark art prompt with 7 structured categories.";
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

  const personalityTrait = (ideaObj.personality || ideaObj.trait || currentPersonality || "").trim();
  const subjectCharacter = (ideaObj.subject || currentSubject || "").trim();
  const actionSituationScene = (ideaObj.scene || ideaObj.action || currentScene || "").trim();
  const environment = (ideaObj.environment || currentEnv || "").trim();
  const atmosphereWeather = (ideaObj.atmosphere || currentAtm || "").trim();
  const artStyle = (ideaObj.style || currentStyle || "").trim();
  const creativeChallenge = (ideaObj.challenge || currentChallenge || "").trim();
  const storyHook = (ideaObj.storyHook || currentStory || "").trim();

  const generatedSentence = buildFullSentence(
    personalityTrait,
    subjectCharacter,
    actionSituationScene,
    environment,
    atmosphereWeather,
    artStyle,
    creativeChallenge
  );

  return {
    reply: parsed.reply || "Here is a refined Classic Spark concept!",
    quickPills: Array.isArray(parsed.quickPills) && parsed.quickPills.length > 0
      ? parsed.quickPills
      : ["Make atmosphere darker", "Change action / scene", "Try another art style", "Increase difficulty"],
    actionType: parsed.actionType || "GENERATE_IDEA",
    promptType: "ClassicSpark",
    idea: {
      promptType: "ClassicSpark",
      difficulty: validDiff,
      personalityTrait,
      subjectCharacter,
      actionSituationScene,
      environment,
      atmosphereWeather,
      artStyle,
      creativeChallenge,
      storyHook,
      generatedSentence
    }
  };
}

module.exports = {
  handleClassicSparkBrainstorm
};
