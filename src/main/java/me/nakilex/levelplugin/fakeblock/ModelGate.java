package me.nakilex.levelplugin.fakeblock;

import com.nexomc.nexo.api.NexoFurniture;
import me.nakilex.levelplugin.lootchests.utils.LocationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a furniture model that can be toggled between two states.
 */
public class ModelGate {
    private final String id;
    private final Location location;
    private final String openModel;
    private final String closedModel;
    /** Whether the gate is closed by default for players that have no
     * personalised setting. */
    private boolean defaultClosed;

    /** Per-player gate states. True means closed. */
    private final Map<UUID, Boolean> playerStates = new HashMap<>();

    private ItemDisplay openEntity;
    private ItemDisplay closedEntity;

    /** Access the spawned open model entity. */
    public ItemDisplay getOpenEntity() { return openEntity; }

    /** Access the spawned closed model entity. */
    public ItemDisplay getClosedEntity() { return closedEntity; }

    /** Returns true if the given entity is part of this gate. */
    public boolean matchesEntity(org.bukkit.entity.Entity e) {
        if (e == null) return false;
        if (openEntity != null && openEntity.getUniqueId().equals(e.getUniqueId())) return true;
        return closedEntity != null && closedEntity.getUniqueId().equals(e.getUniqueId());
    }

    public ModelGate(String id, Location location, String openModel, String closedModel, boolean closed) {
        this.id = id.toLowerCase();
        this.location = location;
        this.openModel = openModel;
        this.closedModel = closedModel;
        this.defaultClosed = closed;
    }

    public String getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public String getOpenModel() {
        return openModel;
    }

    public String getClosedModel() {
        return closedModel;
    }

    /** Returns true if this gate is considered a town waystone.
     * A blue beacon indicates a town, red indicates a dungeon. */
    public boolean isTown() {
        return openModel.toLowerCase().contains("blue");
    }

    public boolean isDefaultClosed() {
        return defaultClosed;
    }

    public void setDefaultClosed(boolean closed) {
        this.defaultClosed = closed;
    }

    public boolean isClosed(UUID player) {
        return playerStates.getOrDefault(player, defaultClosed);
    }

    public void setClosed(UUID player, boolean closed) {
        playerStates.put(player, closed);
    }

    /** Toggle state for a single player. */
    public void toggle(UUID player) {
        setClosed(player, !isClosed(player));
    }

    /** Spawn the underlying entities for the open and closed models. */
    public void spawnEntities(Plugin plugin) {
        removeAll();
        Location centered = LocationUtils.centerOnBlock(location);
        if (centered == null) {
            plugin.getLogger().warning("[ModelGate] Unable to spawn gate '" + id + "'; location is null");
            return;
        }

        // ensure any lingering furniture from previous sessions is removed
        NexoFurniture.remove(centered);
        openEntity = NexoFurniture.place(openModel, centered, 0f, BlockFace.NORTH);
        closedEntity = NexoFurniture.place(closedModel, centered, 0f, BlockFace.NORTH);

        plugin.getLogger().info("[ModelGate] Spawned gate '" + id + "' at " + centered);
        if (openEntity == null || closedEntity == null) {
            plugin.getLogger().warning("[ModelGate] Failed to spawn one or more entities for gate '" + id + "'");
        }
    }

    /** Update which entity the player can see based on their state. */
    public void apply(Player player, Plugin plugin) {
        if (openEntity == null || closedEntity == null) {
            spawnEntities(plugin);
        }
        boolean closed = isClosed(player.getUniqueId());
        if (openEntity != null) {
            if (closed) player.hideEntity(plugin, openEntity); else player.showEntity(plugin, openEntity);
        }
        if (closedEntity != null) {
            if (closed) player.showEntity(plugin, closedEntity); else player.hideEntity(plugin, closedEntity);
        }
    }

    /** Spawn for all currently online players. */
    public void applyAll(Plugin plugin) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            apply(p, plugin);
        }
    }

    public void removeAll() {
        if (openEntity != null) {
            NexoFurniture.remove(openEntity);
            openEntity = null;
        }
        if (closedEntity != null) {
            NexoFurniture.remove(closedEntity);
            closedEntity = null;
        }
    }
}
