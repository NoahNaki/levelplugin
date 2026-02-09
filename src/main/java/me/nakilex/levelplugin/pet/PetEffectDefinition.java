package me.nakilex.levelplugin.pet;

public record PetEffectDefinition(PetEffectType type,
                                  double baseValue,
                                  double perLevelValue) {
    public double valueForLevel(int level) {
        int safeLevel = Math.max(1, level);
        return baseValue + (safeLevel - 1) * perLevelValue;
    }
}
