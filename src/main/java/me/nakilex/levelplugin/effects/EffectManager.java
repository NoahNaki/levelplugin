// EffectManager.java
package me.nakilex.levelplugin.effects;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EffectManager {

    private final Map<UUID, SwordCircleEffect> activeEffects = new HashMap<>();

    public void startSwordCircleEffect(Player player) {
        UUID playerId = player.getUniqueId();

        // Remove existing effect if present
        if (activeEffects.containsKey(playerId)) {
            stopSwordCircleEffect(player);
        }

        // Create and start new effect
        SwordCircleEffect effect = new SwordCircleEffect();
        effect.spawnSwords(player.getLocation());
        activeEffects.put(playerId, effect);
    }

    public void stopSwordCircleEffect(Player player) {
        UUID playerId = player.getUniqueId();

        // Remove effect if active
        if (activeEffects.containsKey(playerId)) {
            activeEffects.get(playerId).removeSwords();
            activeEffects.remove(playerId);
        }
    }

    public boolean hasActiveEffect(Player player) {
        return activeEffects.containsKey(player.getUniqueId());
    }
}
