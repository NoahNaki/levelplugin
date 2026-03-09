package me.nakilex.levelplugin.dungeon.modifiers;

import me.nakilex.levelplugin.mob.custom.CustomMobDefinition;

public interface RunModifier {
    String id();

    default int modifyRewardCoins(int baseCoins) {
        return baseCoins;
    }

    default double modifyDropChance(double baseChance) {
        return baseChance;
    }

    default CustomMobDefinition modifyMob(CustomMobDefinition base) {
        return base;
    }
}
