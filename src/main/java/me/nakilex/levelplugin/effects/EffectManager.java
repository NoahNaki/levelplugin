package me.nakilex.levelplugin.effects;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.tasks.SwordCircleTask;
import me.nakilex.levelplugin.tasks.SwordFireTask;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EffectManager {

    private final Map<UUID, SwordCircleEffect> activeCircleEffects = new HashMap<>();
    private final Map<UUID, SwordFireEffect> activeFireEffects = new HashMap<>();

    // Start Circle Sword Effect
    public void startSwordCircleEffect(Player player) {
        UUID playerId = player.getUniqueId();

        // Stop any active effects before starting a new one
        stopEffect(player);

        // Create and spawn effect
        SwordCircleEffect effect = new SwordCircleEffect();
        effect.spawnSwords(player.getLocation());
        activeCircleEffects.put(playerId, effect);

        // Start animation task
        new SwordCircleTask(effect.getArmorStands(), player)
            .runTaskTimer(Main.getInstance(), 0L, 1L); // Schedule task
    }

    // Start Sword Fire Effect
    public void startSwordFireEffect(Player player) {
        UUID playerId = player.getUniqueId();

        // Stop any active effects before starting a new one
        stopEffect(player);

        // Create and spawn effect
        SwordFireEffect effect = new SwordFireEffect();
        effect.spawnSwords(player.getLocation());
        activeFireEffects.put(playerId, effect);

        // Start animation task
        new SwordFireTask(effect.getArmorStands())
            .runTaskTimer(Main.getInstance(), 0L, 1L); // Schedule task
    }


    // Stop any active effects
    public void stopEffect(Player player) {
        UUID playerId = player.getUniqueId();

        // Stop Circle Effect if active
        if (activeCircleEffects.containsKey(playerId)) {
            activeCircleEffects.get(playerId).removeSwords();
            activeCircleEffects.remove(playerId);
        }

        // Stop Fire Effect if active
        if (activeFireEffects.containsKey(playerId)) {
            activeFireEffects.get(playerId).getArmorStands().forEach(armorStand -> armorStand.remove());
            activeFireEffects.remove(playerId);
        }
    }

    // Check if player has an active effect
    public boolean hasActiveEffect(Player player) {
        UUID playerId = player.getUniqueId();
        return activeCircleEffects.containsKey(playerId) || activeFireEffects.containsKey(playerId);
    }
}
