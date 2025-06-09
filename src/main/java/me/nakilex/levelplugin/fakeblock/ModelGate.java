package me.nakilex.levelplugin.fakeblock;

import com.nexomc.nexo.api.NexoFurniture;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

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

    /** Spawn the appropriate model for a specific player. */
    public void spawn(Player player) {
        NexoFurniture.remove(location, player);
        String model = isClosed(player.getUniqueId()) ? closedModel : openModel;
        // The Nexo API does not expose a stable per-player spawn method in all
        // versions.  Try reflection first and fall back to a global spawn.
        try {
            var method = NexoFurniture.class.getMethod(
                "place", String.class, Location.class, float.class,
                BlockFace.class, Player.class);
            method.invoke(null, model, location, 0f, BlockFace.NORTH, player);
        } catch (NoSuchMethodException ignored) {
            // Older API - spawn globally as a fallback
            NexoFurniture.place(model, location, 0f, BlockFace.NORTH);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** Spawn for all currently online players. */
    public void spawnAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            spawn(p);
        }
    }

    public void removeAll() {
        NexoFurniture.remove(location, null);
    }
}
