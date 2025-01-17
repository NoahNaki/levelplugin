package me.nakilex.levelplugin.mob.managers;

import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class MythicMobNameManager implements Listener {

    private final Main plugin;

    /**
     * Keep track of all MythicMobs that have spawned
     * so we can continuously update their HP display.
     */
    private final Set<ActiveMob> trackedMobs = new HashSet<>();

    public MythicMobNameManager(Main plugin) {
        this.plugin = plugin;

        // Schedule a repeating task every 20 ticks (1 second)
        // to update the name of tracked mobs.
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            updateMobNames();
        }, 5L, 5L);
    }

    /**
     * When a MythicMob spawns, add it to our tracking set.
     */
    @EventHandler
    public void onMythicMobSpawn(MythicMobSpawnEvent event) {
        ActiveMob activeMob = event.getMob();
        trackedMobs.add(activeMob);

        // Optionally, set an initial display name on spawn
        // (not strictly required if the repeating task updates it quickly).
        setDisplayName(activeMob);
    }

    /**
     * If a mob dies, you can remove it from tracking here
     * to keep the set clean. (If you don't, our code below
     * will auto-remove them anyway when it sees they're dead.)
     */
    @EventHandler
    public void onMythicMobDeath(EntityDeathEvent event) {
        // If you only want to remove MythicMobs from tracking:
        ActiveMob mob = plugin.getMythicHelper().getMythicMobInstance(event.getEntity());
        if (mob != null) {
            trackedMobs.remove(mob);
        }
    }

    /**
     * Called every second by our scheduled task.
     * Updates the display name of each tracked MythicMob.
     */
    private void updateMobNames() {
        // Use an iterator so we can remove dead mobs mid-loop
        for (Iterator<ActiveMob> it = trackedMobs.iterator(); it.hasNext();) {
            ActiveMob mob = it.next();

            // If it's null or dead, remove it from tracking
            if (mob == null || mob.getEntity() == null || mob.getEntity().isDead()) {
                it.remove();
                continue;
            }

            // Update the name with current HP / max HP
            setDisplayName(mob);
        }
    }

    /**
     * Applies the custom name format:
     *   [Lv 10] (FormattedName) currentHP/maxHP ♥
     */
    private void setDisplayName(ActiveMob mob) {
        // Example: set a static level or fetch from your logic
        int level = 10;

        double currentHP = mob.getEntity().getHealth();
        double maxHP     = mob.getEntity().getMaxHealth();

        // e.g. "RANCID_PIG_ZOMBIE" → "Rancid Pig Zombie"
        String rawType   = mob.getMobType();
        String prettyType = formatMobName(rawType);

        String displayName = ChatColor.GRAY + "[Lv " + level + "] "
            + ChatColor.WHITE + prettyType + " "
            + ChatColor.RED + (int) currentHP + "/" + (int) maxHP + " \u2764";

        mob.getEntity().getBukkitEntity().setCustomName(displayName);
        mob.getEntity().getBukkitEntity().setCustomNameVisible(true);
    }

    /**
     * Converts "RANCID_PIG_ZOMBIE" to "Rancid Pig Zombie".
     *  1) Split by underscore
     *  2) Lowercase each piece
     *  3) Capitalize the first letter
     *  4) Join with spaces
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
