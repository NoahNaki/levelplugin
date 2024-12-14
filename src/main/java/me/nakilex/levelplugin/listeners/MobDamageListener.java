package me.nakilex.levelplugin.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks how much damage each player deals to each entity.
 * We read from this map when the entity dies in PlayerKillListener.
 */
public class MobDamageListener implements Listener {

    // Data structure: maps each mob's UUID to a map of (player-uuid -> damage dealt)
    private static final Map<UUID, Map<UUID, Double>> damageMap = new HashMap<>();

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();
        Entity entity = event.getEntity();
        double damage = event.getFinalDamage();

        // Store damage
        damageMap.putIfAbsent(entity.getUniqueId(), new HashMap<>());
        Map<UUID, Double> playerDamage = damageMap.get(entity.getUniqueId());
        playerDamage.put(player.getUniqueId(), playerDamage.getOrDefault(player.getUniqueId(), 0.0) + damage);
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
