package me.nakilex.levelplugin.spells.context;

import me.nakilex.levelplugin.spells.Spell;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Holds dynamic spell modifications applied at cast time, including stacked rune-driven effects.
 */
public class SpellCastContext {
    private final Spell baseSpell;
    private final Player player;

    // Modifier percents
    private double damagePercent = 0.0;
    private double cooldownReductionPercent = 0.0;

    // New fields from extraParams
    private double aoeRadius = 0.0;
    private double stunDuration = 0.0;
    private boolean applyCooldown = true;
    private double manaCostModifier = 0.0;

    // Support multiple TRANSFORM effects
    private final List<String> effectKeys = new ArrayList<>();

    // Store any other params generically
    private final Map<String, Object> extraParams = new HashMap<>();

    public SpellCastContext(Spell baseSpell, Player player) {
        this.baseSpell = baseSpell;
        this.player = player;
        // Initialize with the default effect key
        this.effectKeys.add(baseSpell.getEffectKey());
    }

    /** Accumulate flat damage % bonuses. */
    public void addDamagePercent(double percent) {
        this.damagePercent += percent;
    }

    /** Accumulate cooldown reduction % bonuses. */
    public void reduceCooldownPercent(double percent) {
        this.cooldownReductionPercent += percent;
    }

    /** Add an additional effect key (for TRANSFORM runes). */
    public void addEffectKey(String key) {
        if (key != null && !key.isEmpty()) {
            effectKeys.add(key);
        }
    }

    /** Get the ordered list of effect keys to execute. */
    public List<String> getEffectKeys() {
        return Collections.unmodifiableList(effectKeys);
    }

    /**
     * Injects generic parameters by name, mapping recognized keys to fields.
     * Unknown keys are stored in extraParams.
     */
    public void putExtraParam(String key, Object value) {
        switch (key) {
            case "aoeRadius":
                this.aoeRadius = ((Number) value).doubleValue();
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
            // Add more recognized keys here as needed
            default:
                extraParams.put(key, value);
        }
    }

    /** Retrieves a generic extra param by key. */
    public Object getExtraParam(String key) {
        return extraParams.get(key);
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
        if (!applyCooldown) {
            return 0L;
        }
        double reduced = baseSpell.getCooldown() * (1 - cooldownReductionPercent / 100.0);
        return (long) (reduced * 1000);
    }

    /**
     * Calculate final mana cost, adding any flat increases.
     */
    public double getFinalManaCost() {
        double cost = baseSpell.getCurrentManaCost(player);
        return cost + manaCostModifier;
    }

    public Spell getBaseSpell() { return baseSpell; }
    public Player getPlayer() { return player; }
}
