package me.nakilex.levelplugin.spells;

public record SpellDefinition(String id, String displayName, int baseManaCost, boolean movementSpell) {
    public SpellDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Spell id cannot be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Spell display name cannot be blank");
        }
        if (baseManaCost < 0) {
            throw new IllegalArgumentException("Mana cost cannot be negative");
        }
    }
}
