package me.nakilex.playerspoofer;

/**
 * Small curated fact sheet for LLM chat. Keep this limited to facts the server owner
 * explicitly supplied so bots do not invent mechanics when discussing enchants.
 */
final class ServerKnowledge {
    private ServerKnowledge() {}

    static String enchantPromptContext() {
        return "SERVER ENCHANT FACTS (use only when relevant; do not dump the whole list unless asked): "
                + "Active TOKEN enchants: Token Finder max 2000 base 10 tokens; Salary max 1000 base 50; "
                + "Gem Merchant max 100 base 25; Token Merchant max 100 base 25; Gem Finder max 1000 base 250; "
                + "Blessing max 1000 base 500; Charity max 1000 base 500; Fortune max 2000 base 1000; "
                + "Haste max 10 base 1000; Speed max 5 base 5000; Night Vision max 1 base 5000; "
                + "Jump Boost max 3 base 7500; Prodigy max 2500 base 25000; Tornado max 10 base 500000; "
                + "Acid Rain max 10 base 650000; Black Hole max 10 base 750000; Meteor Shower max 10 base 900000. "
                + "Tornado, Acid Rain, Black Hole and Meteor Shower are custom enchants from LevelPlugin. "
                + "Disabled token enchants: Efficiency max 1500 base 250, Unbreaking max 500 base 25, "
                + "Autosell max 1 base 100000, Fly max 1 base 250000. Efficiency, Fly and Autosell are hidden pickaxe defaults rather than normal purchasable upgrades. "
                + "Active GEM enchants: Key Finder max 250 base 2500 gems; Keyalls max 250 base 2500; "
                + "Layer max 1000 base 500; Prestige Finder max 1000 base 250; Nuke max 1000 base 2500; "
                + "Second Hand max 2500 base 2500; Explosive max 1000 base 5000; Super Token Miner max 100 base 5000; "
                + "Laser max 1000 base 10000; Sixth Hand max 2500 base 12000; Block Booster max 1000 base 12500; "
                + "Double Strike max 2500 base 20000; Prism Break max 2500 base 30000; Doomfall max 2500 base 40000. "
                + "Do not claim one enchant is objectively best unless the recent chat gives enough context; when discussing value/strategy, speak like a player with an opinion, not an authority.";
    }
}
