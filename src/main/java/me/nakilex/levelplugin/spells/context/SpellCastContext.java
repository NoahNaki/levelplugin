package me.nakilex.levelplugin.spells.context;

import me.nakilex.levelplugin.spells.Spell;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds runtime data for a single spell cast, allowing rune-driven modifications.
 */
public class SpellCastContext {
    private final Spell baseSpell;
    private final Player caster;

    private double finalDamageMultiplier;
    private double finalManaCost;
    private double finalCooldown;
    private String effectKey;
    private final Map<String, Object> extraParams = new HashMap<>();

    public SpellCastContext(Spell baseSpell, Player caster) {
        this.baseSpell = baseSpell;
        this.caster = caster;

        // Initialize with base values
        this.finalDamageMultiplier = baseSpell.getDamageMultiplier();
        this.finalManaCost = baseSpell.getManaCost();
        this.finalCooldown = baseSpell.getCooldown();
        this.effectKey = baseSpell.getEffectKey();
    }

    /**
     * Returns the base damage multiplier defined on the Spell.
     */
    public double getBaseDamage() {
        return baseSpell.getDamageMultiplier();
    }

    /**
     * Exposes the Spell object used for this cast.
     */
    public Spell getBaseSpell() {
        return baseSpell;
    }

    /**
     * Returns the casting player.
     */
    public Player getCaster() {
        return caster;
    }

    /**
     * Returns the final damage multiplier after applying runes.
     */
    public double getFinalDamageMultiplier() {
        return finalDamageMultiplier;
    }

    /**public double getFinalManaCost() {
     return finalManaCost;
     }

     public double getFinalCooldown() {
     return finalCooldown;
     }
     * Increases the damage multiplier by a percentage (e.g. +20% is percent=20).
     */
    public void addDamagePercent(double percent) {
        this.finalDamageMultiplier *= (1 + percent / 100.0);
    }


    public double getFinalManaCost() {
        return finalManaCost;
    }

    public double getFinalCooldown() {
        return finalCooldown;
    }

    /**
     * Reduces the cooldown by a percentage (e.g. 10% reduction is percent=10).
     */
    public void reduceCooldownPercent(double percent) {
        this.finalCooldown *= (1 - percent / 100.0);
    }

    /**
     * Returns the effect key determining which SpellEffect to invoke.
     */
    public String getEffectKey() {
        return effectKey;
    }

    /**
     * Overrides the effect key (used for transform runes).
     */
    public void setEffectKey(String effectKey) {
        this.effectKey = effectKey;
    }

    public void setFinalDamageMultiplier(double finalDamageMultiplier) {
        this.finalDamageMultiplier = finalDamageMultiplier;
    }

    public void setFinalManaCost(double finalManaCost) {
        this.finalManaCost = finalManaCost;
    }

    public void setFinalCooldown(double finalCooldown) {
        this.finalCooldown = finalCooldown;
    }

    /**
     * Returns the map of extra parameters provided by transform runes (e.g. extraProjectiles).
     */
    public Map<String, Object> getExtraParams() {
        return extraParams;
    }

    /**
     * Puts an extra parameter into the context.
     */
    public void putExtraParam(String key, Object value) {
        extraParams.put(key, value);
    }
}
