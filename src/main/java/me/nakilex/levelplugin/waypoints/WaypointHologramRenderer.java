package me.nakilex.levelplugin.waypoints;

import me.nakilex.levelplugin.lootchests.utils.LocationUtils;
import me.nakilex.levelplugin.utils.MultiLineHologram;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Manages hologram labels for waypoint targets per player.
 */
public class WaypointHologramRenderer {
    private final double hoverOffset;
    private final double moveThresholdSquared;
    private final Map<UUID, MultiLineHologram> holograms = new HashMap<>();
    private final Map<UUID, Location> lastHologramLocations = new HashMap<>();
    private final Map<UUID, List<String>> lastHologramLines = new HashMap<>();

    public WaypointHologramRenderer(double hoverOffset, double moveThresholdSquared) {
        this.hoverOffset = hoverOffset;
        this.moveThresholdSquared = moveThresholdSquared;
    }

    public void update(Player player, Location target, List<String> hologramLines) {
        if (player == null || hologramLines == null || hologramLines.isEmpty()) {
            clear(player);
            return;
        }

        Location hologramLocation = resolveHologramLocation(target);
        if (hologramLocation == null) {
            clear(player);
            return;
        }

        UUID uuid = player.getUniqueId();
        MultiLineHologram hologram = holograms.get(uuid);
        Location lastLocation = lastHologramLocations.get(uuid);
        List<String> lastLines = lastHologramLines.get(uuid);

        boolean shouldRespawn = hologram == null
                || lastLocation == null
                || !Objects.equals(lastLocation.getWorld(), hologramLocation.getWorld())
                || lastLocation.distanceSquared(hologramLocation) > moveThresholdSquared;

        if (shouldRespawn) {
            if (hologram != null) {
                hologram.despawn();
            }
            hologram = new MultiLineHologram(hologramLocation, hologramTag(uuid));
            hologram.spawn(hologramLines);
            holograms.put(uuid, hologram);
        } else if (!hologramLines.equals(lastLines)) {
            hologram.setLines(hologramLines);
        }

        lastHologramLocations.put(uuid, hologramLocation);
        lastHologramLines.put(uuid, new ArrayList<>(hologramLines));
    }

    public void clear(Player player) {
        if (player == null) {
            return;
        }
        clear(player.getUniqueId());
    }

    public void clear(UUID uuid) {
        MultiLineHologram hologram = holograms.remove(uuid);
        if (hologram != null) {
            hologram.despawn();
        }
        lastHologramLocations.remove(uuid);
        lastHologramLines.remove(uuid);
    }

    public void clearAll() {
        for (UUID uuid : new ArrayList<>(holograms.keySet())) {
            clear(uuid);
        }
    }

    private Location resolveHologramLocation(Location target) {
        if (target == null) {
            return null;
        }
        Location centered = LocationUtils.centerOnBlock(target);
        Location surface = LocationUtils.surfaceBelow(centered, true);
        if (surface == null) {
            return null;
        }
        return surface.clone().add(0, hoverOffset, 0);
    }

    private String hologramTag(UUID uuid) {
        return "quest_waypoint_" + uuid;
    }
}
