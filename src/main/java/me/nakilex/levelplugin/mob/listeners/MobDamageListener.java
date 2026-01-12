package me.nakilex.levelplugin.mob.listeners;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks how much damage each player deals to each entity.
 * Updates mob health display dynamically during combat.
 */
public class MobDamageListener implements Listener {

    // Data structure: maps each mob's UUID to a map of (player-uuid -> damage dealt)
    private static final Map<UUID, Map<UUID, Double>> damageMap = new HashMap<>();

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Player player = null;
        Entity damager = event.getDamager();

        // —— Identify the attacking player ——
        if (damager instanceof Player) {
            player = (Player) damager;

            // —— Requirement check on the held weapon ——
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand != null && mainHand.getType() != Material.AIR) {
                CustomItem inst = ItemManager.getInstance()
                    .getCustomItemFromItemStack(mainHand);
                if (inst != null) {
                    int playerLevel = LevelManager.getInstance().getLevel(player);
                    int reqLevel    = inst.getLevelRequirement();

                    String clsReqRaw = inst.getClassRequirement();
                    me.nakilex.levelplugin.player.classes.data.PlayerClass reqClass = null;
                    if (clsReqRaw != null && !clsReqRaw.isBlank()) {
                        reqClass = me.nakilex.levelplugin.player.classes.data.PlayerClass.fromString(clsReqRaw);
                    }

                    me.nakilex.levelplugin.player.classes.data.PlayerClass playerClass =
                            StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass;

                    // Cancel if wrong class
                    if (!me.nakilex.levelplugin.player.classes.data.ClassUtil.meetsRequirement(playerClass, reqClass)) {
                        player.sendMessage(ChatColor.RED +
                            "You cannot wield your " + inst.getBaseName() + " as a " + playerClass.name().toLowerCase() + ".");
                        event.setCancelled(true);
                        return;
                    }

                    // Cancel if below level
                    if (playerLevel < reqLevel) {
                        player.sendMessage(ChatColor.RED +
                            "You must be level " + reqLevel +
                            " to use your " + inst.getBaseName() + "!");
                        event.setCancelled(true);
                        return;
                    }

                }
            }

        } else if (damager instanceof Arrow arrow && arrow.hasMetadata("BasicAttack")) {
            UUID shooterId = (UUID) arrow.getMetadata("BasicAttack").get(0).value();
            player = Bukkit.getPlayer(shooterId);
        }

        if (player == null) return;

        Entity entity = event.getEntity();
        double damage = event.getFinalDamage();

        // —— Update damage tracking map ——
        damageMap.putIfAbsent(entity.getUniqueId(), new HashMap<>());
        Map<UUID, Double> playerDamage = damageMap.get(entity.getUniqueId());
        playerDamage.put(
            player.getUniqueId(),
            playerDamage.getOrDefault(player.getUniqueId(), 0.0) + damage
        );

        // —— Optionally update mob health display ——
        if (entity instanceof LivingEntity livingEntity) {
            double currentHealth = Math.max(livingEntity.getHealth() - damage, 0);
            double maxHealth = livingEntity.getMaxHealth();

            String healthText = ChatColor.RED +
                String.format("%.0f", currentHealth) +
                "/" +
                String.format("%.0f", maxHealth) +
                " ♥";

            livingEntity.setCustomName(getMobName(livingEntity.getCustomName()) + "  " + healthText);
        }
    }





    /**
     * Extracts the mob's original name (removes health and level prefixes).
     */
    private String getMobName(String fullName) {
        if (fullName == null) return "Mob";
        String[] parts = fullName.split("  ");
        return parts.length > 1 ? parts[1] : "Mob"; // Default to "Mob" if parsing fails
    }

    /**
     * Returns the damage map for a specific mob (if any).
     */
    public static Map<UUID, Double> getDamageMapForEntity(UUID entityUUID) {
        return damageMap.getOrDefault(entityUUID, new HashMap<>());
    }

    /**
     * Removes the entry once the mob dies, so we don't keep stale data in memory.
     */
    public static void clearDamageRecord(UUID entityUUID) {
        damageMap.remove(entityUUID);
    }

    /**
     * Returns the participating players for an entity and clears their damage record.
     */
    public static Set<Player> getParticipantsAndClear(UUID entityUUID) {
        Map<UUID, Double> damage = damageMap.remove(entityUUID);
        if (damage == null || damage.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Player> participants = new HashSet<>();
        for (UUID playerId : damage.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                participants.add(player);
            }
        }
        return participants;
    }
}
