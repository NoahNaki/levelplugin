package me.nakilex.levelplugin.spells.deck;

import me.nakilex.levelplugin.spells.input.SpellInputType;
import org.bukkit.Material;

import java.util.List;

public record SpellCardDefinition(String cardId,
                                  String familyId,
                                  String spellId,
                                  String displayName,
                                  SpellDeckRarity rarity,
                                  SpellInputType defaultInputType,
                                  Material displayMaterial,
                                  List<String> effectLines,
                                  List<String> tradeoffLines) {
    public SpellCardDefinition {
        if (cardId == null || cardId.isBlank()) {
            throw new IllegalArgumentException("cardId cannot be blank");
        }
        if (familyId == null || familyId.isBlank()) {
            throw new IllegalArgumentException("familyId cannot be blank");
        }
        if (spellId == null || spellId.isBlank()) {
            throw new IllegalArgumentException("spellId cannot be blank");
        }
        if (rarity == null) {
            throw new IllegalArgumentException("rarity cannot be null");
        }
        if (defaultInputType == null) {
            throw new IllegalArgumentException("defaultInputType cannot be null");
        }
        displayName = (displayName == null || displayName.isBlank()) ? cardId : displayName;
        displayMaterial = displayMaterial == null ? rarity.displayMaterial() : displayMaterial;
        effectLines = effectLines == null ? List.of() : List.copyOf(effectLines);
        tradeoffLines = tradeoffLines == null ? List.of() : List.copyOf(tradeoffLines);
    }
}
