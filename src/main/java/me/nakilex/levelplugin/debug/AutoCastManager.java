package me.nakilex.levelplugin.debug;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.spells.Spell;
import me.nakilex.levelplugin.spells.managers.CooldownManager;
import me.nakilex.levelplugin.spells.managers.SpellManager;
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
    private final Map<UUID, Long> lastCast = new HashMap<>();

    public record ToggleOutcome(boolean success, boolean enabled, String errorMessage) {
        public static ToggleOutcome failure(String errorMessage) {
            return new ToggleOutcome(false, false, errorMessage);
        }

        public static ToggleOutcome success(boolean enabled) {
            return new ToggleOutcome(true, enabled, null);
        }
    }

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
            lastCast.remove(id);
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
                long now = System.nanoTime();
                Long prev = lastCast.put(id, now);
                if (prev != null) {
                    double interval = (now - prev) / 1_000_000_000.0;
                    player.sendMessage(String.format("Cast interval: %.3fs", interval));
                }

                Spell spell = SpellManager.getInstance()
                        .getSpellById(ps.playerClass.name().toLowerCase(), skillId);
                if (spell != null) {
                    spell.castEffect(player);
                } else if (Bukkit.getPluginManager().isPluginEnabled("MythicMobs")) {
                    MythicBukkit.inst().getAPIHelper().castSkill(player, skillId);
                } else {
                    player.sendMessage("MythicMobs is not enabled; cannot autocast skill " + skillId + ".");
                    cancel(player);
                    return;
                }
                cd.setCooldown(id, COOLDOWN_KEY, cooldown);
            }
        }, 0L, 1L);
        tasks.put(id, task);
        lastCast.remove(id);
        return true;
    }

    public ToggleOutcome toggleMageFireball(Player player) {
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        if (ps == null || ps.playerClass != PlayerClass.MAGE) {
            return ToggleOutcome.failure("Mage class required for autocast debug.");
        }
        boolean enabled = toggle(player, "fireball");
        return ToggleOutcome.success(enabled);
    }

    public boolean isAutoCasting(Player player) {
        return tasks.containsKey(player.getUniqueId());
    }

    /**
     * Cancel any running autocast task for the player.
     */
    public void cancel(Player player) {
        UUID id = player.getUniqueId();
        BukkitTask task = tasks.remove(id);
        if (task != null) {
            task.cancel();
        }
        lastCast.remove(id);
    }
}
