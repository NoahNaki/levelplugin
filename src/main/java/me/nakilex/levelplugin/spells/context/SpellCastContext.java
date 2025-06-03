package me.nakilex.levelplugin.spells.context;

import me.nakilex.levelplugin.spells.Spell;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Holds dynamic spell modifications applied at cast time, including stacked rune-driven effects
 * and priority-based extra parameter resolution.
 */
public class SpellCastContext {
    private final Spell baseSpell;
    private final Player player;

    // Modifier percents
    private double damagePercent = 0.0;
    private double cooldownReductionPercent = 0.0;

    // Mapped extra params
    private double aoeRadius = 0.0;
    private double stunDuration = 0.0;
    private boolean applyCooldown = true;
    private double manaCostModifier = 0.0;

    // Support multiple TRANSFORM effects
    private final List<String> effectKeys = new ArrayList<>();

    // Generic extra params with priority
    private static class Param {
        Object value;
        int priority;
    }
    private final Map<String, Param> extraParams = new HashMap<>();

    public SpellCastContext(Spell baseSpell, Player player) {
        this.baseSpell = baseSpell;
        this.player = player;
        // start with the default effect key
        this.effectKeys.add(baseSpell.getEffectKey());
    }

    /** Add flat damage% bonuses. */
    public void addDamagePercent(double percent) {
        this.damagePercent += percent;
    }

    public double getDamagePercent() {
        return damagePercent;
    }

    /** Add cooldown reduction% bonuses. */
    public void reduceCooldownPercent(double percent) {
        this.cooldownReductionPercent += percent;
    }

    /** Add an additional effect key (for TRANSFORM runes). */
    public void addEffectKey(String key) {
        if (key != null && !key.isEmpty()) {
            effectKeys.add(key);
        }
    }

    /** Returns the ordered list of effect keys to execute. */
    public List<String> getEffectKeys() {
        return Collections.unmodifiableList(effectKeys);
    }

    /**
     * Convenience overload for effects that don’t specify a priority.
     * Uses a default priority of 0 (lowest).
     */
    public void putExtraParam(String key, Object value) {
        putExtraParam(key, value, 0);
    }

    /**
     * Injects generic parameters by name, only if this effect's priority >= existing priority.
     */
    /**
     * Injects generic parameters by name, accumulating duplicate keys into a list,
     * and only applies priority rules for transforming vs. modifiers.
     */
    public void putExtraParam(String key, Object value, int priority) {
        switch (key) {
            case "aoeRadius":
            case "aoeRange":
            case "extraProjectiles":
            case "pierceLevel":
                // always accumulate these params into a list
                Param existingParam = extraParams.get(key);
                List<Object> list;
                if (existingParam != null && existingParam.value instanceof List<?>) {
                    @SuppressWarnings("unchecked")
                    List<Object> existingList = (List<Object>) existingParam.value;
                    list = existingList;
                } else {
                    list = new ArrayList<>();
                    if (existingParam != null) {
                        list.add(existingParam.value);
                    }
                }
                list.add(value);
                Param newParam = new Param();
                newParam.value = list;
                newParam.priority = priority; // priority less relevant for stacking
                extraParams.put(key, newParam);
                break;

            case "stunDuration":
                this.stunDuration = ((Number) value).doubleValue();
                break;
            case "applyCooldown":
                this.applyCooldown = (Boolean) value;
                break;
            case "manaCostIncrease":
                this.manaCostModifier += ((Number) value).doubleValue();
                break;

            default:
                // for other keys, highest-priority wins
                Param prev = extraParams.get(key);
                if (prev == null || priority >= prev.priority) {
                    Param p = new Param();
                    p.value = value;
                    p.priority = priority;
                    extraParams.put(key, p);
                }
        }
    }


    /** Retrieves the final value (highest priority) for this key, or null. */
    public Object getExtraParam(String key) {
        Param p = extraParams.get(key);
        return p == null ? null : p.value;
    }

    // --- Getters for mapped params ---
    public double getAoeRadius() { return aoeRadius; }
    public double getStunDuration() { return stunDuration; }
    public boolean isApplyCooldown() { return applyCooldown; }
    public double getManaCostModifier() { return manaCostModifier; }

    /** Calculate final damage based on base and percent modifiers. */
    public double getFinalDamage() {
        return baseSpell.getBaseDamage() * (1 + damagePercent / 100.0);
    }

    /**
     * Calculate final cooldown in milliseconds, or 0 if cooldown is disabled.
     */
    public long getFinalCooldown() {
        if (!applyCooldown) return 0L;
        double reduced = baseSpell.getCooldown() * (1 - cooldownReductionPercent / 100.0);
        return (long) (reduced * 1000);
    }

    /** Count final mana cost, adding any flat increases. */
    public double getFinalManaCost() {
        double cost = baseSpell.getCurrentManaCost(player);
        return cost + manaCostModifier;
    }

    public Spell getBaseSpell() { return baseSpell; }
    public Player getPlayer()     { return player;     }
}
