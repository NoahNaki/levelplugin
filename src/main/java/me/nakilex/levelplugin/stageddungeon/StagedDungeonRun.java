package me.nakilex.levelplugin.stageddungeon;

import me.nakilex.levelplugin.arena.instance.ArenaInstance;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.boss.BossBar;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

final class StagedDungeonRun {
    final UUID playerId;
    final StagedDungeonDefinition definition;
    final int stage;
    final double mobHealth;
    final Location returnLocation;
    final ArenaInstance instance;
    UUID mobId;
    BossBar healthBar;
    BukkitTask timerTask;
    long deadlineMs;
    boolean finishing;

    StagedDungeonRun(UUID playerId, StagedDungeonDefinition definition, int stage,
                     double mobHealth, Location returnLocation, ArenaInstance instance) {
        this.playerId = playerId;
        this.definition = definition;
        this.stage = stage;
        this.mobHealth = mobHealth;
        this.returnLocation = returnLocation.clone();
        this.instance = instance;
    }

    Player getPlayer() {
        return Bukkit.getPlayer(playerId);
    }

    LivingEntity getMob() {
        if (mobId == null) return null;
        return Bukkit.getEntity(mobId) instanceof LivingEntity living ? living : null;
    }

    void removeMob() {
        LivingEntity mob = getMob();
        if (mob != null) {
            mob.remove();
        }
        mobId = null;
    }

    void cleanupUi() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        if (healthBar != null) {
            healthBar.removeAll();
            healthBar.setVisible(false);
            healthBar = null;
        }
    }
}
