package me.nakilex.levelplugin.player.listener;

import me.nakilex.levelplugin.mob.listeners.MobDamageListener;
import me.nakilex.levelplugin.party.Party;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.mob.data.CustomMob;
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
    private final PartyManager partyManager;


    public PlayerKillListener(LevelManager levelManager, FileConfiguration mobConfig, PartyManager partyManager) {
        this.levelManager = levelManager;
        this.mobConfig = mobConfig;
        this.partyManager = partyManager;

    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Remove vanilla XP
        event.setDroppedExp(0);

        Entity entity = event.getEntity();
        EntityType entityType = entity.getType();

        // If it's not a Monster (zombie, skeleton, creeper, etc.), do nothing
        if (!(entity instanceof Monster)) {
            MobDamageListener.clearDamageRecord(entity.getUniqueId());
            return;
        }

        // Fetch PersistentDataContainer
        PersistentDataContainer pdc = entity.getPersistentDataContainer();

        // Check if the mob has a custom ID
        if (pdc.has(CustomMob.MOB_ID_KEY, PersistentDataType.STRING)) {
            // Retrieve the custom mob ID
            String mobID = pdc.get(CustomMob.MOB_ID_KEY, PersistentDataType.STRING);

            // Get the mob's XP directly from metadata (default to 10 if missing)
            int baseXP = pdc.getOrDefault(CustomMob.XP_KEY, PersistentDataType.INTEGER, 10);

            // Retrieve damage contributors
            Map<UUID, Double> damageContributors = MobDamageListener.getDamageMapForEntity(entity.getUniqueId());
            double totalDamage = 0.0;

            for (double dmg : damageContributors.values()) {
                totalDamage += dmg;
            }

            // Safety check: if somehow no damage was recorded, do nothing
            if (totalDamage <= 0) {
                MobDamageListener.clearDamageRecord(entity.getUniqueId());
                return;
            }

            // Award XP based on damage fraction
            for (Map.Entry<UUID, Double> entry : damageContributors.entrySet()) {
                UUID playerUUID = entry.getKey();
                double dmgDone = entry.getValue();
                double fraction = dmgDone / totalDamage;

                int xpAward = (int) Math.round(fraction * baseXP);

                Player contributor = Bukkit.getPlayer(playerUUID);
                if (contributor != null && contributor.isOnline()) {
                    // Check if the contributor is in a party
                    Party party = partyManager.getParty(playerUUID);
                    if (party != null) {
                        // Calculate XP multiplier based on party size
                        int size = party.getSize();
                        double multiplier = getXpMultiplier(size);
                        xpAward = (int) Math.round(xpAward * multiplier);
                    }

                    // Add XP
                    levelManager.addXP(contributor, xpAward);

                    // Send feedback
                    double percentage = fraction * 100.0;
                    contributor.sendMessage(String.format(
                        "§eYou dealt %.1f%% of the damage to %s and earned %d XP!",
                        percentage, mobID, xpAward
                    ));
                }
            }

            // Clear the damage record after distributing XP
            MobDamageListener.clearDamageRecord(entity.getUniqueId());

        } else {
            // Example default behavior for non-custom mobs
            // If you want to do a similar damage-based XP distribution for normal mobs,
            // replicate the same logic here (e.g., retrieving baseXP from your config,
            // distributing among contributors, etc.).

            int baseXP = mobConfig.getInt("mobs." + entityType.name(), 10);
            // Possibly do something similar here...
            MobDamageListener.clearDamageRecord(entity.getUniqueId());
        }
    }

    /**
     * Helper method to determine XP multiplier based on party size.
     *  1 Player = 1.0  (0% boost)
     *  2 Players = 1.25 (25% boost)
     *  3 Players = 1.30 (30% boost)
     *  4 Players = 1.40 (40% boost)
     *  5+ Players = 1.50 (50% boost)
     */
    private double getXpMultiplier(int partySize) {
        switch (partySize) {
            case 1:  return 1.0;
            case 2:  return 1.25;
            case 3:  return 1.30;
            case 4:  return 1.40;
            default: return 1.50; // For 5 to 8 (max)
        }
    }

}
