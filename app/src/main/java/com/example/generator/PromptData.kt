package com.example.generator

object PromptData {
    val traits = listOf(
        "Sleepy", "Curious", "Lonely", "Mischievous", "Elegant", "Angry", "Joyful",
        "Nervous", "Mysterious", "Brave", "Ancient", "Forgetful", "Grumpy", "Dreamy",
        "Melancholy", "Inventive", "Gentle", "Rebellious", "Stoic", "Whimsical",
        "Clumsy", "Timid", "Fierce", "Noble", "Eccentric", "Wise", "Cunning",
        "Jovial", "Pensive", "Rowdy", "Enchanted", "Wandering", "Scholarly", "Feisty",
        "Awestruck", "Inquisitive", "Luminous", "Serene", "Tenacious"
    )

    val subjects = listOf(
        "Underwater Explorer", "Fox", "Cat", "Dragon", "Robot", "Knight", "Witch", "Wizard", "Mermaid",
        "Dinosaur", "Alien", "Ghost", "Samurai", "Pirate", "Mushroom Person", "Crow",
        "Rabbit", "Forest Giant", "Tiny Creature", "Mechanical Whale", "Owl Scholar",
        "Deep-sea Diver", "Celestial Spirit", "Golem Sculptor", "Clockwork Bird",
        "Steampunk Alchemist", "Raccoon Bandit", "Shadow Beast", "Forest Dryad",
        "Badger Blacksmith", "Cyberpunk Courier", "Astronaut Gardener", "Moss Beast",
        "Stag Monarch", "Frog Herbalist", "Lantern Beetle", "Chameleon Painter",
        "Glowing Sea Creature", "Star Navigator", "Ancient Archivist"
    )

    val actions = listOf(
        "guarding ancient ruins in", "discovering ancient scrolls inside", "brewing glowing herbal tea inside",
        "repairing a broken celestial relic in", "sketching constellations from", "resting atop mossy ruins in",
        "fishing for falling stars from", "reading a forgotten scroll in", "navigating uncharted waters through",
        "stargazing from", "hiding within", "dancing across the rooftops of",
        "cultivating glowing botanicals in", "singing softly to slumbering spirits in", "wandering lost through",
        "building a secret shelter inside", "sheltering from the elements under", "meditating inside",
        "delivering a forgotten letter to", "harvesting luminous crystals in", "baking magical pastries inside",
        "playing an enchanted lute atop", "mapping out constellations from", "guarding the forgotten gates of"
    )

    val environments = listOf(
        "an abandoned lighthouse", "an enchanted mossy forest", "a floating island sanctuary",
        "a ruined medieval castle", "a sunlit rooftop greenhouse", "a sunken underwater library",
        "a sun-bleached desert temple", "a lunar observatory", "a cozy cluttered attic studio",
        "a misty mountain village", "a retro space station diner", "a volcanic forge cavern",
        "a frozen crystal canyon", "an ancient clocktower interior", "a hidden waterfall oasis",
        "a neon rain-slicked alleyway", "a giant mushroom glade", "a subterranean grand library",
        "a misty mountain shrine", "a hollowed giant ancient oak", "a forgotten greenhouse conservatory",
        "an overgrown train station", "a cloud-top windmill village", "a vibrant coral reef palace"
    )

    val atmospheres = listOf(
        "golden sunlight filtering through deep blue water", "a violent electric thunderstorm",
        "dense magical fog", "a golden hour sunset", "a crisp pastel sunrise",
        "a shimmering aurora borealis", "a gentle snowstorm", "a warm summer drizzle",
        "a total solar eclipse", "a clear starry midnight", "a howling autumn gale",
        "a hazy indigo twilight", "a bioluminescent moonless night", "a glowing firefly dusk",
        "a copper dust storm", "a blood-red harvest moon", "a calm misty morning",
        "an eerie emerald tempest", "a sun-dappled quiet afternoon"
    )

    val styles = listOf(
        "Chiaroscuro Digital Painting",
        "Watercolor on textured cold-press paper",
        "Expressive fine-liner ink sketch",
        "Soft colored pencil drawing",
        "Whimsical children's book illustration",
        "Dynamic anime-inspired style",
        "Vintage comic-book ink & halftone",
        "Dark atmospheric fantasy painting",
        "Playful retro 90s cartoon style",
        "Dramatic impressionist oil study",
        "Detailed 16-bit pixel art aesthetic",
        "Cinematic visual development concept art",
        "Vibrant matte gouache illustration",
        "Bold linocut block print style",
        "Risograph print with grain & offset",
        "Loose charcoal tonal study"
    )

    val challenges = listOf(
        "Focus on extreme depth of field",
        "Use only three colors",
        "No erasing allowed",
        "Use only one textured brush",
        "Draw it in 10 minutes",
        "Use your non-dominant hand for the sketch",
        "Dramatic silhouette and negative space only",
        "Construct strictly with geometric shapes",
        "Use only blue ink / monochrome",
        "Continuous line drawing (do not lift the pencil)",
        "No straight lines anywhere in the piece",
        "High-contrast crosshatching for all shadows",
        "Draw from an extreme low-angle perspective",
        "Invert values: white lines on dark background",
        "Only warm colors (red, orange, yellow, pink)",
        "Only cool colors (blues, teals, purples, mint)",
        "Focus on exaggerated textures and patterns"
    )

    data class CreativeGapTemplateDef(
        val template: String,
        val blankPosition: String,
        val starters: List<String>
    )

    // Structured Creative Gap Definitions with intentional blank and position role
    val creativeGapDefinitions = listOf(
        CreativeGapTemplateDef(
            template = "A curious %s discovers a mysterious ______ hidden beneath an ancient %s.",
            blankPosition = "mysterious discovery",
            starters = listOf(
                "a miniature pocket universe in a bottle",
                "a crystal that captures forgotten memories",
                "a glowing key that opens whatever door you draw"
            )
        ),
        CreativeGapTemplateDef(
            template = "A lonely %s finds something extraordinary inside an abandoned %s.",
            blankPosition = "extraordinary discovery",
            starters = listOf(
                "a mechanical moth with stained-glass wings",
                "a seedling that grows into clockwork gears",
                "a lantern fueled by glowing starlight"
            )
        ),
        CreativeGapTemplateDef(
            template = "The potion master's %s contains a secret doorway leading directly to ______.",
            blankPosition = "secret realm / destination",
            starters = listOf(
                "a realm of floating liquid islands",
                "a garden where memories grow as flowers",
                "a library of unwritten futures"
            )
        ),
        CreativeGapTemplateDef(
            template = "A sleepy %s carefully protects ______ during %s.",
            blankPosition = "treasured object / creature",
            starters = listOf(
                "a delicate egg made of blown glass",
                "a slumbering constellation in a jar",
                "a tiny dragon no bigger than a teacup"
            )
        ),
        CreativeGapTemplateDef(
            template = "An ancient %s awakens after centuries to find ______ resting in its hands.",
            blankPosition = "mysterious relic / organism",
            starters = listOf(
                "a blooming mechanical lotus",
                "a compass pointing toward whatever you miss most",
                "a forgotten crown made of autumn leaves"
            )
        ),
        CreativeGapTemplateDef(
            template = "A traveling %s plays a strange melody that causes ______ to sprout across %s.",
            blankPosition = "magical flora / phenomenon",
            starters = listOf(
                "luminescent crystal mushrooms",
                "whispering golden vines",
                "floating origami butterflies"
            )
        ),
        CreativeGapTemplateDef(
            template = "Inside the forgotten %s, a glowing container reveals ______.",
            blankPosition = "glowing contents / surprise",
            starters = listOf(
                "a miniature galaxy spinning silently",
                "a potion that changes ink into living birds",
                "a parchment map that redraws itself"
            )
        ),
        CreativeGapTemplateDef(
            template = "A mischievous %s attempts to trade a shiny coin for ______.",
            blankPosition = "desired trade item / oddity",
            starters = listOf(
                "a bottle of captured lightning",
                "a key made of frozen moonlight",
                "a pair of glasses that see through time"
            )
        ),
        CreativeGapTemplateDef(
            template = "Beneath %s, two unexpected friends share ______ during %s.",
            blankPosition = "shared treasure / moment",
            starters = listOf(
                "a cup of hot star-dew tea",
                "a secret book of celestial constellations",
                "a pastry baked from forgotten memories"
            )
        ),
        CreativeGapTemplateDef(
            template = "A weary %s looks up at the sky and notices ______ descending from the clouds.",
            blankPosition = "aerial arrival / phenomenon",
            starters = listOf(
                "a celestial airship with silk sails",
                "a falling star with feathered wings",
                "a giant clockwork constellation"
            )
        ),
        CreativeGapTemplateDef(
            template = "The wizard accidentally cast a spell that transformed their hat into ______.",
            blankPosition = "magical transformation",
            starters = listOf(
                "a tiny terrarium of miniature fireflies",
                "a living nest of origami songbirds",
                "a portal spilling starry mist"
            )
        ),
        CreativeGapTemplateDef(
            template = "In the heart of %s, the royal %s is crowned with ______ instead of gold.",
            blankPosition = "symbolic crown / relic",
            starters = listOf(
                "a wreath of glowing bioluminescent moss",
                "a circlet of frozen northern lights",
                "a halo of floating ancient runes"
            )
        ),
        CreativeGapTemplateDef(
            template = "A tiny %s builds a cozy home out of ______ inside %s.",
            blankPosition = "crafting material / structure",
            starters = listOf(
                "clockwork pocket watch gears",
                "hollowed-out quartz crystals",
                "stacked tea cups and vintage postage stamps"
            )
        )
    )

    // Creative Gap Templates with intentional blank for the artist's imagination
    val creativeGapTemplates = creativeGapDefinitions.map { it.template }

    // Thought-starter inspiration chips when user explores Creative Gap
    val gapInspirationIdeas = listOf(
        "a miniature pocket universe in a bottle",
        "a crystal that captures forgotten memories",
        "a seedling that grows into clockwork gears",
        "a map showing places that only exist at night",
        "a tea set carved from meteorite stone",
        "a mechanical moth with stained-glass wings",
        "a key that opens whatever door you draw",
        "a tiny dragon no bigger than a teacup",
        "a lantern fueled by glowing starlight",
        "a compass pointing toward whatever you miss most"
    )

    fun getOptionsForCategory(category: com.example.model.PromptCategory): List<String> = when (category) {
        com.example.model.PromptCategory.TRAIT -> traits
        com.example.model.PromptCategory.SUBJECT -> subjects
        com.example.model.PromptCategory.ACTION -> actions
        com.example.model.PromptCategory.ENVIRONMENT -> environments
        com.example.model.PromptCategory.ATMOSPHERE -> atmospheres
        com.example.model.PromptCategory.STYLE -> styles
        com.example.model.PromptCategory.CHALLENGE -> challenges
    }
}
