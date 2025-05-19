// File: src/main/java/me/nakilex/levelplugin/runes/model/Rune.java
package me.nakilex.levelplugin.runes.model;

import org.bukkit.ChatColor;

import java.util.Collections;
import java.util.List;

/**
 * Represents a Rune template that can be equipped to modify spells.
 */
public class Rune {
    private final String id;
    private final String displayName;
    private final List<String> description;
    private final Rarity rarity;
    private final String targetClass;
    private final String targetSpell;
    private final boolean unique;
    private final List<RuneEffect> effects;

    public Rune(
        String id,
        String displayName,
        List<String> description,
        Rarity rarity,
        String targetClass,
        String targetSpell,
        boolean unique,
        List<RuneEffect> effects
    ) {
        this.id = id;
        this.displayName = displayName;
        this.description = Collections.unmodifiableList(description);
        this.rarity = rarity;
        this.targetClass = targetClass;
        this.targetSpell = targetSpell;
        this.unique = unique;
        this.effects = Collections.unmodifiableList(effects);
    }

    /** Unique identifier for this rune template */
    public String getId() {
        return id;
    }

    /** Human-readable name shown in GUIs */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Descriptive lore lines for this rune.
     */
    public List<String> getDescription() {
        return description;
    }

    /** Rarity tier of the rune. */
    public Rarity getRarity() {
        return rarity;
    }

    /**
     * The character class this rune applies to, e.g. "MAGE".
     */
    public String getTargetClass() {
        return targetClass;
    }

    /**
     * The spell ID this rune modifies, e.g. "meteor".
     */
    public String getTargetSpell() {
        return targetSpell;
    }

    /**
     * True if only one of this rune type may be equipped per spell.
     */
    public boolean isUnique() {
        return unique;
    }

    /**
     * The list of effects this rune grants when applied.
     */
    public List<RuneEffect> getEffects() {
        return effects;
    }

    @Override
    public String toString() {
        return String.format(
            "Rune[id=%s,name=%s,rarity=%s,class=%s,spell=%s,unique=%b,effects=%s]",
            id, displayName, rarity, targetClass, targetSpell, unique, effects
        );
    }

    /**
     * Rarity tiers for runes, each with a display color.
     */
    public enum Rarity {
        COMMON(ChatColor.WHITE),
        UNCOMMON(ChatColor.GREEN),
        RARE(ChatColor.BLUE),
        EPIC(ChatColor.DARK_PURPLE),
        LEGENDARY(ChatColor.GOLD);

        private final ChatColor color;

        Rarity(ChatColor color) {
            this.color = color;
        }

        /** Returns the GUI color for this rarity. */
        public ChatColor getColor() {
            return color;
        }
    }
}
