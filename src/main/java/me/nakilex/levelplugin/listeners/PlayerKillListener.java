package me.nakilex.levelplugin.listeners;

import me.nakilex.levelplugin.managers.LevelManager;
import me.nakilex.levelplugin.mob.CustomMob;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

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

        // Fetch PersistentDataContainer
        PersistentDataContainer pdc = entity.getPersistentDataContainer();

        // Check if the mob has a custom ID
        if (pdc.has(CustomMob.MOB_ID_KEY, PersistentDataType.STRING)) {
            // Retrieve the mob ID
            String mobID = pdc.get(CustomMob.MOB_ID_KEY, PersistentDataType.STRING);

            // Get the mob's XP directly from metadata
            int baseXP = pdc.getOrDefault(CustomMob.XP_KEY, PersistentDataType.INTEGER, 10); // Default 10 if missing

            Map<UUID, Double> damageContributors = MobDamageListener.getDamageMapForEntity(entity.getUniqueId());
            double totalDamage = 0.0;

            for (double dmg : damageContributors.values()) {
                totalDamage += dmg;
            }

            if (totalDamage <= 0) {
                MobDamageListener.clearDamageRecord(entity.getUniqueId());
                return;
            }

            for (Map.Entry<UUID, Double> entry : damageContributors.entrySet()) {
                UUID playerUUID = entry.getKey();
                double dmgDone = entry.getValue();
                double fraction = dmgDone / totalDamage;
                int xpAward = (int) Math.round(fraction * baseXP);

                Player contributor = Bukkit.getPlayer(playerUUID);
                if (contributor != null && contributor.isOnline()) {
                    levelManager.addXP(contributor, xpAward);
                    double percentage = fraction * 100.0;
                    contributor.sendMessage(String.format(
                        "§eYou dealt %.1f%% of the damage to %s and earned %d XP!",
                        percentage, mobID, xpAward // Display custom ID instead of entity type
                    ));
                }
            }

            // Clear the damage record
            MobDamageListener.clearDamageRecord(entity.getUniqueId());

        } else {
            // Default behavior for non-custom mobs
            int baseXP = mobConfig.getInt("mobs." + entityType.name(), 10);
        }
    }
}
