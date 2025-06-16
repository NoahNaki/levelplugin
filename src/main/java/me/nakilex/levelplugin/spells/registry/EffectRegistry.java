package me.nakilex.levelplugin.spells.registry;

import me.nakilex.levelplugin.spells.ArcherSpell;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.effect.archer.*;
import me.nakilex.levelplugin.spells.effect.mage.*;
import me.nakilex.levelplugin.spells.effect.warrior.*;
import me.nakilex.levelplugin.spells.effect.rogue.*;


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
        // --- Mage spells ---

        register("METEOR", new MeteorEffect());
        register("BLACKHOLE", new BlackholeEffect());
        register("HEAL", new HealEffect());
        register("TELEPORT", new TeleportEffect());
        register("BASIC_RAY", new BasicRayEffect());

        // --- Meteor Runes ---
        register("METEOR_SHOWER_EFFECT", new MeteorShowerEffect());
        register("FROST_COMET_EFFECT", new FrostCometEffect());
        register("OBSIDIAN_METEOR_EFFECT", new ObsidianMeteorEffect());

        // --- Blackhole Runes
        register("SINGULARITY_BLACKHOLE_EFFECT", new SingularityBlackholeEffect());
        register("TEMPORAL_BLACKHOLE_EFFECT", new TemporalStasisBlackholeEffect());
        register("CHAOS_BLACKHOLE_EFFECT", new ChaosBlackholeEffect());
        register("HEAL", new HealEffect());

        // --- Archer Spells

        register("BASIC_ATTACK", new BasicArrowShotEffect());
        register("POWER_SHOT", new PowerShotEffect());
        register("BOW_DRONE", new BowDroneEffect());
        register("GRAPPLE_HOOK", new GrappleHookEffect());
        register("ARROW_STORM", new ArrowStormEffect());
        register("EPIC_ARROW_STORM_EFFECT", new EpicArrowStormEffect());
        register("EXPLOSIVE_ARROW", new ExplosiveArrowShotEffect());

        // --- Warrior Spells
        register("IRON_FORTRESS", new IronFortressEffect());
        register("HEROIC_LEAP", new HeroicLeapEffect());
        register("SHOCKWAVE", new ShockwaveEffect());
        register("VORTEX_SHOCKWAVE_EFFECT", new VortexShockwaveEffect());
        register("VOLCANIC_BLAST_EFFECT", new VolcanicBlastEffect());
        register("VORTEX_LEAP_EFFECT", new VortexLeapEffect());
        register("STUNNING_LEAP_EFFECT", new StunningLeapEffect());
        register("WAR_CRY", new WarCryEffect());
        register("POWER_STRIKE", new PowerStrikeEffect());
        register("JUDGEMENT", new JudgementEffect());
        register("GATE_OF_RUIN_EFFECT", new GateOfRuinEffect());

        // --- Rogue Spells ---
        register("CRESCENT_SLASH", new CrescentSlashEffect());
        register("MULTIHIT", new MultihitEffect());
        register("SMOKE_BOMB", new SmokeBombEffect());
        register("VANISH", new VanishEffect());

    }
}
