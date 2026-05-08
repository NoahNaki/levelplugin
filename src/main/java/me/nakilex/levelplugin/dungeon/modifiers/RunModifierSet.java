package me.nakilex.levelplugin.dungeon.modifiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RunModifierSet {
    private final List<RunModifier> modifiers;

    public RunModifierSet(List<RunModifier> modifiers) {
        this.modifiers = modifiers == null ? new ArrayList<>() : new ArrayList<>(modifiers);
    }

    public int modifyRewardCoins(int baseCoins) {
        int value = baseCoins;
        for (RunModifier modifier : modifiers) {
            value = modifier.modifyRewardCoins(value);
        }
        return value;
    }

    public double modifyDropChance(double baseChance) {
        double value = baseChance;
        for (RunModifier modifier : modifiers) {
            value = modifier.modifyDropChance(value);
        }
        return value;
    }

    public int modifyWaveMobCount(int baseCount) {
        int value = baseCount;
        for (RunModifier modifier : modifiers) {
            value = modifier.modifyWaveMobCount(value);
        }
        return value;
    }

    public double modifyDamageTaken(double baseDamage) {
        double value = baseDamage;
        for (RunModifier modifier : modifiers) {
            value = modifier.modifyDamageTaken(value);
        }
        return value;
    }

    public double modifyScoreMultiplier(double baseMultiplier) {
        double value = baseMultiplier;
        for (RunModifier modifier : modifiers) {
            value = modifier.modifyScoreMultiplier(value);
        }
        return value;
    }

    public double modifyEliteObjectiveChance(double baseChance) {
        double value = baseChance;
        for (RunModifier modifier : modifiers) {
            value = modifier.modifyEliteObjectiveChance(value);
        }
        return value;
    }

    public boolean isEmpty() {
        return modifiers.isEmpty();
    }

    public List<RunModifier> modifiers() {
        return Collections.unmodifiableList(modifiers);
    }
}
