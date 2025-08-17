package me.nakilex.levelplugin.debug;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.spells.managers.CooldownManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Simple utility that repeatedly casts a given MythicMobs skill for a player
 * using their Technique-based attack speed as the cooldown. Primarily used for
 * debugging attack cadence.
 */
public class AutoCastManager {
    private static final String COOLDOWN_KEY = "debug_autocast";
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();

    /**
     * Toggle autocasting of the provided skill for the player.
     * @param player target player
     * @param skillId MythicMobs skill identifier to cast
     * @return true if autocast is now enabled, false if disabled
     */
    public boolean toggle(Player player, String skillId) {
        UUID id = player.getUniqueId();
        BukkitTask existing = tasks.remove(id);
        if (existing != null) {
            existing.cancel();
            return false;
        }

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(Main.getPlugin(), () -> {
            if (!player.isOnline()) {
                cancel(player);
                return;
            }
            StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(id);
            double cooldown = 1.0 / ps.attackSpeed;
            CooldownManager cd = CooldownManager.getInstance();
            if (!cd.isOnCooldown(id, COOLDOWN_KEY)) {
                MythicBukkit.inst().getAPIHelper().castSkill(player, skillId);
                cd.setCooldown(id, COOLDOWN_KEY, cooldown);
            }
        }, 0L, 1L);
        tasks.put(id, task);
        return true;
    }

    /**
     * Cancel any running autocast task for the player.
     */
    public void cancel(Player player) {
        BukkitTask task = tasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }
}
