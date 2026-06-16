package me.nakilex.levelplugin.cooking.runtime;

import me.nakilex.levelplugin.cooking.model.CookingWorkstationType;
import me.nakilex.levelplugin.cooking.util.CookingLocationKey;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory registry for currently placed cooking workstations. */
public class PlacedCookingWorkstationRegistry {
    private final Map<CookingLocationKey, PlacedCookingWorkstation> byLocation = new ConcurrentHashMap<>();

    public PlacedCookingWorkstation register(Block block, CookingWorkstationType type, UUID placedBy) {
        return register(CookingLocationKey.of(block), type, placedBy);
    }

    public PlacedCookingWorkstation register(CookingLocationKey key, CookingWorkstationType type, UUID placedBy) {
        return register(key, type, placedBy, true);
    }

    public PlacedCookingWorkstation registerTransient(Location location, CookingWorkstationType type) {
        return register(CookingLocationKey.of(location), type, null, false);
    }

    private PlacedCookingWorkstation register(CookingLocationKey key, CookingWorkstationType type, UUID placedBy, boolean persistent) {
        PlacedCookingWorkstation placed = new PlacedCookingWorkstation(key, type, placedBy, persistent);
        byLocation.put(key, placed);
        return placed;
    }

    public Optional<PlacedCookingWorkstation> find(Block block) {
        return block == null ? Optional.empty() : find(CookingLocationKey.of(block));
    }

    public Optional<PlacedCookingWorkstation> find(Location location) {
        return location == null || location.getWorld() == null ? Optional.empty() : find(CookingLocationKey.of(location));
    }

    public Optional<PlacedCookingWorkstation> find(CookingLocationKey key) {
        return Optional.ofNullable(byLocation.get(key));
    }

    public Optional<PlacedCookingWorkstation> unregister(Block block) {
        if (block == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byLocation.remove(CookingLocationKey.of(block)));
    }

    public Collection<PlacedCookingWorkstation> all() {
        return List.copyOf(byLocation.values());
    }

    public int size() {
        return byLocation.size();
    }

    public void clear() {
        byLocation.clear();
    }
}
