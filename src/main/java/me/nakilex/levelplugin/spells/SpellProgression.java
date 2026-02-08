package me.nakilex.levelplugin.spells;

import java.util.List;

public record SpellProgression(String baseSpellId, List<String> upgradeSpellIds) {
    public SpellProgression {
        if (baseSpellId == null || baseSpellId.isBlank()) {
            throw new IllegalArgumentException("Base spell id cannot be blank");
        }
        upgradeSpellIds = upgradeSpellIds == null ? List.of() : List.copyOf(upgradeSpellIds);
    }
}
