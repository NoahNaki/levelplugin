package me.nakilex.levelplugin.mob.listeners;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which players have damaged a given mob so rewards can be
 * distributed on death.
 */
public class MythicMobDamageTracker implements Listener {
    private final Map<UUID, Set<Player>> damageMap = new ConcurrentHashMap<>();

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity mob)) return;
        Player hitter = null;
        if (event.getDamager() instanceof Player p) {
            hitter = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            hitter = p;
        }
        if (hitter == null) return;
        damageMap.computeIfAbsent(mob.getUniqueId(), k -> ConcurrentHashMap.newKeySet()).add(hitter);
    }

    /**
     * Obtain the set of participants who damaged this mob and clear its entry.
     */
    public Set<Player> getParticipantsAndClear(UUID mobId) {
        Set<Player> set = damageMap.getOrDefault(mobId, Collections.emptySet());
        damageMap.remove(mobId);
        return set;
    }
}
