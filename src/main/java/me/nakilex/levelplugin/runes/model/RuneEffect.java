package me.nakilex.levelplugin.runes.model;

import java.util.Map;

/**
 * Describes a single effect provided by a rune.
 */
public class RuneEffect {
    /**
     * Type of effect this rune grants.
     */
    public enum Type {
        MODIFIER,    // e.g. +damage %, -cooldown %
        TRANSFORM    // e.g. swap effectKey, add extra projectiles, AoE behavior
    }

    private final Type type;
    private final double bonusDamagePercent;         // e.g. 6.0 for +6%
    private final double cooldownReductionPercent;   // e.g. 10.0 for -10%
    private final String newEffectKey;               // e.g. "METEOR_SHOWER_EFFECT" when transforming
    private final Map<String, Object> extraParams;   // any additional parameters (e.g. "extraProjectiles":3)

    public RuneEffect(
        Type type,
        double bonusDamagePercent,
        double cooldownReductionPercent,
        String newEffectKey,
        Map<String, Object> extraParams
    ) {
        this.type = type;
        this.bonusDamagePercent = bonusDamagePercent;
        this.cooldownReductionPercent = cooldownReductionPercent;
        this.newEffectKey = newEffectKey;
        this.extraParams = extraParams;
    }

    public Type getType() {
        return type;
    }

    public double getBonusDamagePercent() {
        return bonusDamagePercent;
    }

    public double getCooldownReductionPercent() {
        return cooldownReductionPercent;
    }

    /**
     * @return the effectKey to swap to when applying a transform effect, or null for modifiers
     */
    public String getNewEffectKey() {
        return newEffectKey;
    }

    /**
     * Additional effect parameters, if any (e.g. map entries like "extraProjectiles" -> Integer)
     */
    public Map<String, Object> getExtraParams() {
        return extraParams;
    }

    @Override
    public String toString() {
        return String.format("RuneEffect[type=%s, bonusDamage=%.2f%%, cooldownRed=%.2f%%, newEffectKey=%s, params=%s]",
            type, bonusDamagePercent, cooldownReductionPercent, newEffectKey, extraParams);
    }
}
