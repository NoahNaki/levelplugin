package me.nakilex.levelplugin.mob.listeners;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.mob.data.CustomMob;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
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
                    String clsReq   = inst.getClassRequirement();

                    // Parse the required class (default to VILLAGER if invalid)
                    PlayerClass requiredClass;
                    try {
                        requiredClass = PlayerClass.valueOf(clsReq.toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        requiredClass = PlayerClass.VILLAGER;
                    }

                    PlayerClass playerClass = StatsManager.getInstance()
                        .getPlayerStats(player.getUniqueId())
                        .playerClass;

                    // Cancel if below level
                    if (playerLevel < reqLevel) {
                        player.sendMessage(ChatColor.RED +
                            "You must be level " + reqLevel +
                            " to use your " + inst.getBaseName() + "!");
                        event.setCancelled(true);
                        return;
                    }

                    // Cancel if wrong class
                    if (requiredClass != PlayerClass.VILLAGER
                        && requiredClass != playerClass) {
                        player.sendMessage(ChatColor.RED +
                            "Only " +
                            requiredClass.name().toLowerCase() +
                            "s may use your " + inst.getBaseName() + "!");
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

        // —— Update mob health display for custom mobs ——
        if (entity instanceof LivingEntity livingEntity) {
            PersistentDataContainer pdc = livingEntity.getPersistentDataContainer();
            if (!pdc.has(CustomMob.MOB_ID_KEY, PersistentDataType.STRING)) return;

            String level = pdc.has(CustomMob.LEVEL_KEY, PersistentDataType.INTEGER)
                ? String.valueOf(pdc.get(CustomMob.LEVEL_KEY, PersistentDataType.INTEGER))
                : "1";

            double currentHealth = Math.max(livingEntity.getHealth() - damage, 0);
            double maxHealth = livingEntity.getMaxHealth();

            String levelPrefix = ChatColor.GRAY + "[Lv " + level + "]  ";
            String mobName = ChatColor.WHITE + getMobName(livingEntity.getCustomName()) + "  ";
            String healthText = ChatColor.RED +
                String.format("%.0f", currentHealth) +
                "/" +
                String.format("%.0f", maxHealth) +
                " ♥";

            livingEntity.setCustomName(levelPrefix + mobName + healthText);
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
}
