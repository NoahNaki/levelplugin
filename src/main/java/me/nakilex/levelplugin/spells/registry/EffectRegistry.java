package me.nakilex.levelplugin.spells.registry;

import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.effect.MythicSkillEffect;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EffectRegistry {
    private static final Map<String, SpellEffect> EFFECTS = new ConcurrentHashMap<>();
    private static final EffectRegistry INSTANCE = new EffectRegistry();

    /**
     * Lookup an effect by its key (case‐insensitive).
     */
    public static SpellEffect get(String key) {
        return EFFECTS.get(key.toUpperCase());
    }

    /**
     * Register a single effect under its key.
     */
    public static void register(String key, SpellEffect effect) {
        EFFECTS.put(key.toUpperCase(), effect);
    }

    public static EffectRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Register every built‐in effect. Call this once in onEnable().
     */
    public static void registerAll() {

        // --- CoolArcher Mythic skills ---
        register("MYTHIC_QUICK_SHOT", new MythicSkillEffect("Quick_Shot"));
        register("MYTHIC_BACKSTEP", new MythicSkillEffect("Backstep"));
        register("MYTHIC_WINDRAZOR", new MythicSkillEffect("Windrazor"));
        register("MYTHIC_ARROW_BARRAGE", new MythicSkillEffect("Arrow_Barrage"));
        register("MYTHIC_DRAGON_PIERCER", new MythicSkillEffect("Dragon_Piercer"));
        register("MYTHIC_DEADLY_JAVELIN", new MythicSkillEffect("Deadly_Javelin"));

        // --- PhoenixHunter Mythic skills ---
        register("MYTHIC_BLAZING_FEATHERS", new MythicSkillEffect("Blazing_Feathers"));
        register("MYTHIC_ASHDANCE", new MythicSkillEffect("Ashdance"));
        register("MYTHIC_FLAMEBURST_CONVERGENCE", new MythicSkillEffect("Flameburst_Convergence"));
        register("MYTHIC_PHOENIX_TOTEM", new MythicSkillEffect("Phoenix_Totem"));
        register("MYTHIC_PYROCLASMIC_BARRAGE", new MythicSkillEffect("Pyroclasmic_Barrage"));
        register("MYTHIC_PHOENIX_REBIRTH", new MythicSkillEffect("Phoenix_Rebirth"));

        // No legacy warrior or rogue spell effects retained

    }
}
