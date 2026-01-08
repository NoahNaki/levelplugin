package me.nakilex.levelplugin.mob.custom;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;

public record CustomMobStats(int vitality,
                             int strength,
                             int agility,
                             int intelligence,
                             int dexterity,
                             int will,
                             int technique) {

    public static CustomMobStats empty() {
        return new CustomMobStats(0, 0, 0, 0, 0, 0, 0);
    }

    public double computeMaxHealth(double baseHealth) {
        double health = baseHealth;
        health += vitality * StatsManager.HEALTH_PER_VITALITY;
        health += strength * StatsManager.HEALTH_PER_STRENGTH;
        return Math.max(1.0, health);
    }
}
