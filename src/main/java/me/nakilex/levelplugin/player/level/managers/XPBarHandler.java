package me.nakilex.levelplugin.player.level.managers;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class XPBarHandler {

    public static void updateXPBar(Player player, LevelManager levelManager) {
        int currentLevel = levelManager.getLevel(player);
        int currentXP = levelManager.getXP(player);
        int xpNeeded = levelManager.getXpNeededForNextLevel(player);

        if (currentLevel >= levelManager.getMaxLevel()) {
            player.setLevel(currentLevel);
            player.setExp(1.0f);
            return;
        }

        player.setLevel(currentLevel);

        if (xpNeeded > 0) {
            float progress = (float) currentXP / (float) xpNeeded;
            player.setExp(Math.min(progress, 0.999f));
        } else {
            player.setExp(0.0f);
        }
    }

    public static void handleLevelUpEvent(Player player, int newLevel) {
        player.sendMessage("§aYou’ve reached Level " + newLevel + "!");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 30);
    }
}
