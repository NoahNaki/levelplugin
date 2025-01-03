package me.nakilex.levelplugin.player.level.managers;

import me.nakilex.levelplugin.utils.ChatFormatter;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

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

    public static void handleLevelUpEvent(Player player, int newLevel, int xpNeeded) {
        ChatFormatter.constructDivider(player, "§a§l-", 45); // Adjust length as needed based on testing
        ChatFormatter.sendCenteredMessage(player, "§6§lLEVEL UP!");
        ChatFormatter.sendCenteredMessage(player, " ");
        ChatFormatter.sendCenteredMessage(player, "§7You are now level §e§l" + newLevel + "§7!");
        ChatFormatter.sendCenteredMessage(player, "§7You need §e" + xpNeeded + " xp §7to reach level §e" + (newLevel + 1) + "§7.");
        ChatFormatter.sendCenteredMessage(player, " ");
        ChatFormatter.sendCenteredMessage(player, "§a+3 §7Stat Points have been added");
        ChatFormatter.constructDivider(player, "§a§l-", 45); // Adjust length as needed based on testing
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 30);
        launchFirework(player.getLocation());
    }

    public static void launchFirework(Location location) {
        // Create the firework entity at the player's location
        Firework firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK_ROCKET);
        FireworkMeta fireworkMeta = firework.getFireworkMeta();

        // Set the firework's properties
        FireworkEffect effect = FireworkEffect.builder()
            .withColor(Color.GREEN) // Primary explosion color
            .withFade(Color.BLUE) // Fade color after the explosion
            .with(FireworkEffect.Type.BALL) // Shape of the firework explosion
            .withFlicker() // Optional flicker effect
            .withTrail() // Optional trail effect
            .build();

        fireworkMeta.addEffect(effect);
        fireworkMeta.setPower(1); // Adjust to control how high the firework goes before exploding
        firework.setFireworkMeta(fireworkMeta);

        // This will make the firework ascend and explode naturally without detonating immediately
        // It also eliminates damage by setting the firework to detonate before it lands
        firework.setSilent(true); // Optionally make the firework silent if you don't want sound from the explosion itself
    }
}
