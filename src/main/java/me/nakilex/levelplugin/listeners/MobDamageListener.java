package me.nakilex.levelplugin.listeners;

import me.nakilex.levelplugin.mob.CustomMob;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
        // Check if the damager is a player
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        Entity entity = event.getEntity();
        double damage = event.getFinalDamage();

        // Update damage tracking map
        damageMap.putIfAbsent(entity.getUniqueId(), new HashMap<>());
        Map<UUID, Double> playerDamage = damageMap.get(entity.getUniqueId());
        playerDamage.put(player.getUniqueId(), playerDamage.getOrDefault(player.getUniqueId(), 0.0) + damage);

        // Update health display if the entity is a custom mob
        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;
            PersistentDataContainer pdc = livingEntity.getPersistentDataContainer();

            if (!pdc.has(CustomMob.MOB_ID_KEY, PersistentDataType.STRING)) return;

            // Retrieve metadata
            String level = pdc.has(CustomMob.LEVEL_KEY, PersistentDataType.INTEGER)
                ? String.valueOf(pdc.get(CustomMob.LEVEL_KEY, PersistentDataType.INTEGER))
                : "1";

            double currentHealth = Math.max(livingEntity.getHealth() - damage, 0); // Ensure no negative health
            double maxHealth = livingEntity.getMaxHealth();

            // Reformat the custom mob's name
            String levelPrefix = ChatColor.GRAY + "[Lv " + level + "]  ";
            String mobName = ChatColor.WHITE + getMobName(livingEntity.getCustomName()) + "  ";
            String healthText = ChatColor.RED + String.format("%.0f", currentHealth) + "/" + String.format("%.0f", maxHealth) + " ♥";

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
