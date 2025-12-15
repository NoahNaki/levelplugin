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
        // Use Locale.ROOT to avoid Turkish locale issues
        return EFFECTS.get(key.toUpperCase(java.util.Locale.ROOT));
    }

    /**
     * Register a single effect under its key.
     */
    public static void register(String key, SpellEffect effect) {
        EFFECTS.put(key.toUpperCase(java.util.Locale.ROOT), effect);
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

        // --- Archer Mythic skills ---
        register("MYTHIC_QUICK_SHOT", new MythicSkillEffect("Quick_Shot"));
        register("MYTHIC_BACKSTEP", new MythicSkillEffect("Backstep"));
        register("MYTHIC_ARROW_BARRAGE", new MythicSkillEffect("Arrow_Barrage"));
        register("MYTHIC_DRAGON_PIERCER", new MythicSkillEffect("Dragon_Piercer"));
        register("BOW_DRONE", new me.nakilex.levelplugin.spells.effect.archer.BowDroneEffect());
        // --- Deadeye Mythic skills ---
        register("MYTHIC_PISTOL_SHOT", new MythicSkillEffect("Pistol_Shot"));
        register("MYTHIC_SHOTGUN_BLAST", new MythicSkillEffect("Shotgun_Blast"));
        register("MYTHIC_SNIPER_BACKUP", new MythicSkillEffect("Sniper_Backup"));
        register("MYTHIC_DEATHFIRE", new MythicSkillEffect("Deathfire"));
        register("MYTHIC_FOCUS_SHOT", new MythicSkillEffect("Focus_Shot"));
        register("MYTHIC_AIR_STRIKE", new MythicSkillEffect("Air_Strike"));

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
        register("VORTEX_PULL", new me.nakilex.levelplugin.spells.effect.warrior.ShockwaveEffect(
            "Vortex Pull",
            me.nakilex.levelplugin.spells.effect.warrior.ShockwaveEffect.WaveDirection.INWARD,
            5.0,
            0.5,
            0.3,
            0.2,
            0.15
        ));
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

        // --- Rogue Mythic skills ---
        register("MYTHIC_BLADE_SLASH", new MythicSkillEffect("Blade_Slash"));
        register("MYTHIC_ASSASSIN_DASH", new MythicSkillEffect("Assassin_Dash"));
        register("MYTHIC_DAGGER_THROW", new MythicSkillEffect("Dagger_Throw"));
        register("MYTHIC_BLADE_DANCE", new MythicSkillEffect("Blade_Dance"));
        register("MYTHIC_SHADOW_WALK", new MythicSkillEffect("Shadow_Walk_Skill"));

        // --- Awakened Rogue Mythic skills ---
        register("MYTHIC_LETHAL_COMBO", new MythicSkillEffect("Lethal_Combo"));
        register("MYTHIC_RAVAGING_DASH", new MythicSkillEffect("Ravaging_Dash"));
        register("MYTHIC_DEATH_BLOOM", new MythicSkillEffect("Death_Bloom"));
        register("MYTHIC_SHADOWQUAKE", new MythicSkillEffect("Shadowquake"));
        register("MYTHIC_CRIMSON_ARC", new MythicSkillEffect("Crimson_Arc"));
        register("MYTHIC_LAST_DANCE", new MythicSkillEffect("Last_Dance"));
        register("MYTHIC_DEADLY_CALM", new MythicSkillEffect("Deadly_Calm"));

        // --- Awakened Warrior Mythic skills ---
        register("MYTHIC_BRUTAL_COMBO", new MythicSkillEffect("Brutal_Combo"));
        register("MYTHIC_BERSERKERS_LEAP", new MythicSkillEffect("Berserkers_Leap"));
        register("MYTHIC_RELENTLESS_WHIRLWIND", new MythicSkillEffect("Relentless_Whirlwind"));
        register("MYTHIC_BLOODBOUND_BARRIER", new MythicSkillEffect("Bloodbound_Barrier"));
        register("MYTHIC_VICIOUS_STRIKE", new MythicSkillEffect("Vicious_Strike"));
        register("MYTHIC_STRIKE_OF_FURY", new MythicSkillEffect("Strike_Of_Fury"));
        register("MYTHIC_BULWARK_INSTINCT", new MythicSkillEffect("Bulwark_Instinct"));

        // --- Awakened Archer Mythic skills ---
        register("MYTHIC_BLASTING_COMBO", new MythicSkillEffect("Blasting_Combo"));
        register("MYTHIC_EVASIVE_SHOT", new MythicSkillEffect("Evasive_Shot"));
        register("MYTHIC_VOLLEY_OF_ARROWS", new MythicSkillEffect("Volley_Of_Arrows"));
        register("MYTHIC_PIERCING_SKYFALL", new MythicSkillEffect("Piercing_Skyfall"));
        register("MYTHIC_RAPID_ARROWS", new MythicSkillEffect("Rapid_Arrows"));
        register("MYTHIC_SHOT_OF_DESTRUCTION", new MythicSkillEffect("Shot_Of_Destruction"));
        register("MYTHIC_AMBUSH", new MythicSkillEffect("Ambush"));

        // --- Awakened Mage Mythic skills ---
        register("MYTHIC_SORCERY_COMBO", new MythicSkillEffect("Sorcery_Combo"));
        register("MYTHIC_TELEPORT_STRIKE", new MythicSkillEffect("Teleport_Strike"));
        // Use the *_SKILL variants so the full Awakened Mage abilities fire immediately
        // instead of only priming their Mythic stacking auras.
        register("MYTHIC_BLAZING_BARRAGE", new MythicSkillEffect("Blazing_Barrage_SKILL"));
        register("MYTHIC_CRYO_PRISON", new MythicSkillEffect("Cryo_Prison_Skill"));
        register("MYTHIC_HAILPIERCER", new MythicSkillEffect("Hailpiercer"));
        register("MYTHIC_METEOR_OF_DOOM", new MythicSkillEffect("Meteor_Of_Doom"));
        register("MYTHIC_MANA_BARRIER", new MythicSkillEffect("Mana_Barrier"));

        // --- Mage Mythic skills ---
        register("MYTHIC_FIREBALL", new MythicSkillEffect("Fireball"));
        register("MYTHIC_BLINK", new me.nakilex.levelplugin.spells.effect.mage.TeleportEffect());
        register("MYTHIC_METEOR", new me.nakilex.levelplugin.spells.effect.mage.MeteorEffect());
        register("MYTHIC_FROST_NOVA", new MythicSkillEffect("Frost_Nova"));
        register("MYTHIC_INFERNO_CHAINS", new MythicSkillEffect("Inferno_Chains"));

        // --- Archmage Mythic skills ---
        register("MYTHIC_ARCANE_SLASH", new MythicSkillEffect("Arcane_Slash"));
        register("MYTHIC_BLIZZARD", new MythicSkillEffect("Blizzard"));
        register("MYTHIC_CHAINS_OF_VOID", new MythicSkillEffect("Chains_Of_Void"));
        register("MYTHIC_CLOAK_OF_HASTUR", new MythicSkillEffect("Cloak_Of_Hastur"));
        register("MYTHIC_ARCANE_DEVASTATION", new MythicSkillEffect("Arcane_Devastation"));
        register("MYTHIC_METEOR_STORM", new MythicSkillEffect("Meteor_Storm"));
        register("MYTHIC_ARCANE_SHIELD", new MythicSkillEffect("Arcane_Shield"));

        // --- Dragonian Mythic skills ---
        register("MYTHIC_DRAGONIAN_L_T", new MythicSkillEffect("dragonian_l_t"));
        register("MYTHIC_DRAGONIAN_R_T", new MythicSkillEffect("dragonian_r_t"));
        register("MYTHIC_DRAGONIAN_RS_T", new MythicSkillEffect("dragonian_rs_t"));
        register("MYTHIC_DRAGONIAN_SS_T", new MythicSkillEffect("dragonian_ss_t"));
        register("MYTHIC_DRAGONIAN_LS_T", new MythicSkillEffect("dragonian_ls_t"));

        // --- Dragon Warrior Mythic skills ---
        register("MYTHIC_DRAGON_SLASH", new MythicSkillEffect("Dragon_Slash"));
        register("MYTHIC_DRAGON_DASH", new MythicSkillEffect("Dragon_Dash"));
        register("MYTHIC_DRAGON_BREATH", new MythicSkillEffect("Dragon_Breath"));
        register("MYTHIC_DRAGON_ZONE", new MythicSkillEffect("Dragon_Zone"));
        register("MYTHIC_DRAGONBORN", new MythicSkillEffect("Dragonborn"));

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

        // --- Witch Mythic skills ---
        register("MF_CLASS_WITCH_NORMALATTACK", new MythicSkillEffect("mf_class_witch_normalattack"));
        register("MF_CLASS_WITCH_RIGHTCLICK", new MythicSkillEffect("mf_class_witch_rightclick"));
        register("MF_CLASS_WITCH_SNEAK_LEFTCLICK", new MythicSkillEffect("mf_class_witch_sneak_leftclick"));
        register("MF_CLASS_WITCH_SNEAK_RIGHTCLICK", new MythicSkillEffect("mf_class_witch_sneak_rightclick"));
        register("MF_CLASS_WITCH_SHIFTSHIFT", new MythicSkillEffect("mf_class_witch_shiftshift"));
        register("MF_CLASS_WITCH_HOLDSHIFT", new MythicSkillEffect("mf_class_witch_holdshift"));




    }
}
