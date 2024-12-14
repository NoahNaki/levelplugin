package me.nakilex.levelplugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import me.nakilex.levelplugin.managers.LevelManager;

import java.util.Map;
import java.util.UUID;

public class PlayerKillListener implements Listener {

    private final LevelManager levelManager;
    private final FileConfiguration mobConfig;

    public PlayerKillListener(LevelManager levelManager, FileConfiguration mobConfig) {
        this.levelManager = levelManager;
        this.mobConfig = mobConfig;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Remove vanilla XP
        event.setDroppedExp(0);

        Entity entity = event.getEntity();
        EntityType entityType = entity.getType();

        // If it's not a Monster (zombie, skeleton, creeper, etc.), do nothing
        if (!(entity instanceof Monster)) {
            // Clear any stored damage so we don't accumulate data
            MobDamageListener.clearDamageRecord(entity.getUniqueId());
            return;
        }

        // Grab the base XP from config
        int baseXP = mobConfig.getInt("mobs." + entityType.name(), 10);

        // Retrieve damage info from MobDamageListener
        Map<UUID, Double> damageContributors = MobDamageListener.getDamageMapForEntity(entity.getUniqueId());
        double totalDamage = 0.0;

        for (double dmg : damageContributors.values()) {
            totalDamage += dmg;
        }

        // For each player who contributed damage, distribute XP proportionally
        if (totalDamage <= 0) {
            // if somehow no recorded damage, or totalDamage is 0, just skip
            MobDamageListener.clearDamageRecord(entity.getUniqueId());
            return;
        }

        for (Map.Entry<UUID, Double> entry : damageContributors.entrySet()) {
            UUID playerUUID = entry.getKey();
            double dmgDone = entry.getValue();
            double fraction = dmgDone / totalDamage;  // fraction of total damage
            int xpAward = (int) Math.round(fraction * baseXP);

            Player contributor = Bukkit.getPlayer(playerUUID);
            if (contributor != null && contributor.isOnline()) {
                levelManager.addXP(contributor, xpAward);

                // Verbose message with damage % and XP gained
                double percentage = fraction * 100.0;
                contributor.sendMessage(String.format(
                    "§eYou dealt %.1f%% of the damage to %s and earned %d XP!",
                    percentage, entityType.name(), xpAward
                ));
            }
        }

        // Clear the damage record for this entity
        MobDamageListener.clearDamageRecord(entity.getUniqueId());
    }
}
