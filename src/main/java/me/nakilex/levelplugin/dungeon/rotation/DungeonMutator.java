package me.nakilex.levelplugin.dungeon.rotation;

import org.bukkit.ChatColor;

/**
 * Weekly dungeon mutators used by both live dungeon runs and expedition simulations.
 */
public enum DungeonMutator {
    ELITE_SWARM("Elite Swarm", ChatColor.RED, 1.15, 1.12),
    GLASS_CANNON("Glass Cannon", ChatColor.GOLD, 1.20, 1.08),
    ARCANE_DROUGHT("Arcane Drought", ChatColor.DARK_PURPLE, 1.10, 1.15),
    FRACTURED_ARMOR("Fractured Armor", ChatColor.YELLOW, 1.12, 1.10),
    BOUNTIFUL_TROVES("Bountiful Troves", ChatColor.GREEN, 1.30, 1.00);

    private final String displayName;
    private final ChatColor color;
    private final double rewardMultiplier;
    private final double riskMultiplier;

    DungeonMutator(String displayName, ChatColor color, double rewardMultiplier, double riskMultiplier) {
        this.displayName = displayName;
        this.color = color;
        this.rewardMultiplier = rewardMultiplier;
        this.riskMultiplier = riskMultiplier;
    }

    public String displayName() {
        return displayName;
    }

    public ChatColor color() {
        return color;
    }

    public double rewardMultiplier() {
        return rewardMultiplier;
    }

    public double riskMultiplier() {
        return riskMultiplier;
    }

    public String displayLabel() {
        return color + displayName;
    }
}
