package me.nakilex.levelplugin.pet;

import org.bukkit.potion.PotionEffectType;

public record PetEffectDefinition(PotionEffectType type,
                                  int baseAmplifier,
                                  int perLevelAmplifier) {
    public int amplifierForLevel(int level) {
        int safeLevel = Math.max(1, level);
        return Math.max(0, baseAmplifier + (safeLevel - 1) * perLevelAmplifier);
    }
}
