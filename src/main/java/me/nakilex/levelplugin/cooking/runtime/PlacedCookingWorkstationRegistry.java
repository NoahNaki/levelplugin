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

/** In-memory registry for currently placed cooking workstations. Persistence comes in a later phase. */
public class PlacedCookingWorkstationRegistry {
    private final Map<CookingLocationKey, PlacedCookingWorkstation> byLocation = new ConcurrentHashMap<>();

    public PlacedCookingWorkstation register(Block block, CookingWorkstationType type, UUID placedBy) {
        CookingLocationKey key = CookingLocationKey.of(block);
        PlacedCookingWorkstation placed = new PlacedCookingWorkstation(key, type, placedBy);
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
