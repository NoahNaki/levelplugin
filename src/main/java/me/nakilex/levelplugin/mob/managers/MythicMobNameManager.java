package me.nakilex.levelplugin.mob.managers;

import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MythicMobNameManager implements Listener {

    private final Main plugin;

    public MythicMobNameManager(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMythicMobSpawn(MythicMobSpawnEvent event) {
        // Capture the ActiveMob
        ActiveMob activeMob = event.getMob();

        // Schedule a 1-tick delay so MythicMobs doesn't override the name afterward
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Double-check the mob is still valid
            if (activeMob == null || activeMob.getEntity() == null) return;

            // Example level or logic to get the actual level
            int level = 10;
            double currentHP = activeMob.getEntity().getHealth();
            double maxHP = activeMob.getEntity().getMaxHealth();

            // Convert something like "RANCID_PIG_ZOMBIE" → "Rancid Pig Zombie"
            String rawName = activeMob.getMobType();
            String prettyType = formatMobName(rawName);

            // Build the display name
            String displayName = ChatColor.GRAY + "[Lv " + level + "] "
                + ChatColor.WHITE + prettyType + " "
                + ChatColor.RED + (int) currentHP + "/" + (int) maxHP + " \u2764";

            // Apply custom name
            activeMob.getEntity().getBukkitEntity().setCustomName(displayName);
            activeMob.getEntity().getBukkitEntity().setCustomNameVisible(true);

            plugin.getLogger().info("[DEBUG] Delayed set display name to: " + displayName);

        }, 1L);
    }

    /**
     * Converts "RANCID_PIG_ZOMBIE" to "Rancid Pig Zombie".
     * Splits on underscores, lowercases, capitalizes first letter,
     * and joins with spaces.
     */
    private String formatMobName(String rawName) {
        String[] parts = rawName.split("_");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].toLowerCase();
            if (!part.isEmpty()) {
                part = part.substring(0, 1).toUpperCase() + part.substring(1);
            }
            parts[i] = part;
        }
        return String.join(" ", parts);
    }
}
