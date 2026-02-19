package me.nakilex.levelplugin.debug;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ModelEngineUtil;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Spawns and manages stationary ModelEngine fireball debug entities for players.
 */
public class FireballDebugManager {
    private static final String FIREBALL_MODEL_ID = "fireball";
    private static final double SPAWN_DISTANCE = 2.0;

    private final Main plugin;
    private final Map<UUID, ArmorStand> debugFireballs = new HashMap<>();

    public FireballDebugManager(Main plugin) {
        this.plugin = plugin;
    }

    public String spawn(Player player, float yaw, float pitch) {
        if (player == null) {
            return "Players only.";
        }
        remove(player);

        Location location = player.getEyeLocation().clone()
                .add(player.getEyeLocation().getDirection().normalize().multiply(SPAWN_DISTANCE));
        location.setYaw(yaw);
        location.setPitch(pitch);

        ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class, spawned -> {
            spawned.setInvisible(true);
            spawned.setMarker(true);
            spawned.setGravity(false);
            spawned.setSilent(true);
            spawned.setCollidable(false);
            spawned.setInvulnerable(true);
            spawned.setRotation(yaw, pitch);
        });

        ModelEngineUtil.ModelApplyResult result = ModelEngineUtil.applyModels(stand, List.of(FIREBALL_MODEL_ID), plugin);
        if (!result.failed().isEmpty()) {
            stand.remove();
            return "Failed to apply fireball model: " + String.join(", ", result.failed());
        }

        debugFireballs.put(player.getUniqueId(), stand);
        return describe(player);
    }

    public String rotate(Player player, float yaw, float pitch) {
        ArmorStand stand = get(player);
        if (stand == null) {
            return "No debug fireball found. Use /debug fireball spawn first.";
        }
        Location updated = stand.getLocation();
        updated.setYaw(yaw);
        updated.setPitch(pitch);
        stand.teleport(updated);
        stand.setRotation(yaw, pitch);
        return describe(player);
    }

    public String describe(Player player) {
        ArmorStand stand = get(player);
        if (stand == null) {
            return "No debug fireball spawned.";
        }
        Location location = stand.getLocation();
        return String.format("Debug fireball rotation: yaw=%.2f pitch=%.2f at (%.2f, %.2f, %.2f)",
                location.getYaw(),
                location.getPitch(),
                location.getX(),
                location.getY(),
                location.getZ());
    }

    public boolean remove(Player player) {
        ArmorStand stand = get(player);
        if (stand == null) {
            return false;
        }
        debugFireballs.remove(player.getUniqueId());
        if (stand.isValid()) {
            stand.remove();
        }
        return true;
    }

    private ArmorStand get(Player player) {
        if (player == null) {
            return null;
        }
        ArmorStand stand = debugFireballs.get(player.getUniqueId());
        if (stand == null) {
            return null;
        }
        if (!stand.isValid()) {
            debugFireballs.remove(player.getUniqueId());
            return null;
        }
        return stand;
    }
}
