package me.nakilex.levelplugin.spells.registry;

import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.effect.MythicSkillEffect;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EffectRegistry {
    private static final Map<String, SpellEffect> EFFECTS = new ConcurrentHashMap<>();
    private static final EffectRegistry INSTANCE = new EffectRegistry();
    private static boolean initialized = false;

    static {
        registerAll();
    }

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
    public static synchronized void registerAll() {
        if (initialized) return;
        initialized = true;

        // --- CoolArcher Mythic skills ---
        register("MYTHIC_QUICK_SHOT", new MythicSkillEffect("Quick_Shot"));
        register("MYTHIC_BACKSTEP", new MythicSkillEffect("Backstep"));
        register("MYTHIC_WINDRAZOR", new MythicSkillEffect("Windrazor"));
        register("MYTHIC_ARROW_BARRAGE", new MythicSkillEffect("Arrow_Barrage"));
        register("MYTHIC_DRAGON_PIERCER", new MythicSkillEffect("Dragon_Piercer"));
        register("BOW_DRONE", new me.nakilex.levelplugin.spells.effect.archer.BowDroneEffect());

        // --- PhoenixHunter Mythic skills ---
        register("MYTHIC_BLAZING_FEATHERS", new MythicSkillEffect("Blazing_Feathers"));
        register("MYTHIC_ASHDANCE", new MythicSkillEffect("Ashdance"));
        register("MYTHIC_FLAMEBURST_CONVERGENCE", new MythicSkillEffect("Flameburst_Convergence"));
        register("MYTHIC_PHOENIX_TOTEM", new MythicSkillEffect("Phoenix_Totem"));
        register("MYTHIC_PYROCLASMIC_BARRAGE", new MythicSkillEffect("Pyroclasmic_Barrage"));
        register("MYTHIC_PHOENIX_REBIRTH", new MythicSkillEffect("Phoenix_Rebirth"));

        // --- Warrior Mythic skills ---
        register("MYTHIC_BRUTAL_STRIKE", new MythicSkillEffect("Brutal_Strike"));
        register("MYTHIC_CHARGE", new MythicSkillEffect("Charge"));
        register("MYTHIC_CHAIN_HOOK", new MythicSkillEffect("Chain_Hook"));
        register("MYTHIC_SHIELD_BARRIER", new MythicSkillEffect("Shield_Barrier"));
        register("MYTHIC_WHIRLWIND", new MythicSkillEffect("Whirlwind"));
        register("MYTHIC_JUDGEMENT", new MythicSkillEffect("Judgement"));
        register("MYTHIC_RAMPAGE", new MythicSkillEffect("Rampage"));
        register("SHOCKWAVE", new me.nakilex.levelplugin.spells.effect.warrior.ShockwaveEffect());

        // No legacy warrior or rogue spell effects retained

        // --- Barbarian Mythic skills ---
        register("MYTHIC_RAGEBLADE", new MythicSkillEffect("Rageblade"));
        register("MYTHIC_PRIMAL_AXE", new MythicSkillEffect("Primal_Axe"));
        register("MYTHIC_WAR_CRY", new MythicSkillEffect("War_Cry"));
        register("MYTHIC_DOUBLE_EDGE", new MythicSkillEffect("Double_Edge"));
        register("MYTHIC_RELENTLESS_LEAP", new MythicSkillEffect("Relentless_Leap"));
        register("MYTHIC_ETERNAL_FURY", new MythicSkillEffect("Eternal_Fury"));

        // --- Paladin Mythic skills ---
        register("MYTHIC_HOLY_STRIKE", new MythicSkillEffect("Holy_Strike"));
        register("MYTHIC_BOUND_SEAL", new MythicSkillEffect("Bound_Seal"));
        register("MYTHIC_HAMMER_OF_JUSTICE", new MythicSkillEffect("Hammer_Of_Justice"));
        register("MYTHIC_HEAVENLY_SHIELD", new MythicSkillEffect("Heavenly_Shield"));
        register("MYTHIC_UNBREAKABLE_WILL", new MythicSkillEffect("Unbreakable_Will"));
        register("MYTHIC_LAST_STAND", new MythicSkillEffect("Last_Stand"));
        // --- Death Knight Mythic skills ---
        register("MYTHIC_DEATH_STRIKE", new MythicSkillEffect("Death_Strike_ST"));
        register("MYTHIC_PHANTOM_CHARGE", new MythicSkillEffect("Phantom_Charge"));
        register("MYTHIC_WRAITHBOUND_CHAINS", new MythicSkillEffect("Wraithbound_Chains"));
        register("MYTHIC_SOUL_BARRIER", new MythicSkillEffect("Soul_Barrier"));
        register("MYTHIC_NECROTIC_WHIRLWIND", new MythicSkillEffect("Necrotic_Whirlwind"));
        register("MYTHIC_DEATH_SENTENCE", new MythicSkillEffect("Death_Sentence"));

        // --- Abyssion Mythic skills ---
        register("MYTHIC_AQUA_SLASH", new MythicSkillEffect("Aqua_Slash"));
        register("MYTHIC_ABYSSAL_DASH", new MythicSkillEffect("Abyssal_Dash"));
        register("MYTHIC_TIDAL_WAVE", new MythicSkillEffect("Tidal_Wave"));
        register("MYTHIC_AQUA_AURA", new MythicSkillEffect("Aqua_Aura"));
        register("MYTHIC_ABYSSAL_SMASH", new MythicSkillEffect("Abyssal_Smash"));

        // --- Mage Mythic skills ---
        register("MYTHIC_FIREBALL", new MythicSkillEffect("Fireball"));
        register("MYTHIC_BLINK", new me.nakilex.levelplugin.spells.effect.mage.TeleportEffect());
        register("MYTHIC_METEOR", new MythicSkillEffect("Meteor"));
        register("MYTHIC_FROST_NOVA", new MythicSkillEffect("Frost_Nova"));
        register("MYTHIC_INFERNO_CHAINS", new MythicSkillEffect("Inferno_Chains"));

        // --- Dragonian Mythic skills ---
        register("MYTHIC_DRAGONIAN_L_T", new MythicSkillEffect("dragonian_l_t"));
        register("MYTHIC_DRAGONIAN_R_T", new MythicSkillEffect("dragonian_r_t"));
        register("MYTHIC_DRAGONIAN_RS_T", new MythicSkillEffect("dragonian_rs_t"));
        register("MYTHIC_DRAGONIAN_SS_T", new MythicSkillEffect("dragonian_ss_t"));
        register("MYTHIC_DRAGONIAN_LS_T", new MythicSkillEffect("dragonian_ls_t"));

        // --- Windrune Mythic skills ---
        register("MYTHIC_GALE_SLASH", new MythicSkillEffect("Gale_Slash"));
        register("MYTHIC_VAULT", new MythicSkillEffect("Vault"));
        register("MYTHIC_DANCING_BLADE", new MythicSkillEffect("Dancing_Blade"));
        register("MYTHIC_TORRENT", new MythicSkillEffect("Torrent"));
        register("MYTHIC_CLOUDPIERCER", new MythicSkillEffect("Cloudpiercer"));
        register("MYTHIC_WINDBOUND_FURY", new MythicSkillEffect("Windbound_Fury"));

        // --- Arctic Knight Mythic skills ---
        register("MYTHIC_FROST_STRIKE", new MythicSkillEffect("Frost_Strike"));
        register("MYTHIC_GLACIAL_IMPALEMENT", new MythicSkillEffect("Glacial_Impalement"));
        register("MYTHIC_GLACIER_SMASH", new MythicSkillEffect("Glacier_Smash"));
        register("MYTHIC_ARCTIC_CHARGE", new MythicSkillEffect("Arctic_Charge"));
        register("MYTHIC_FROZEN_SHIELD", new MythicSkillEffect("Frozen_Shield"));
        register("MYTHIC_PERMAFROST_LANCE", new MythicSkillEffect("Permafrost_Lance"));




    }
}
