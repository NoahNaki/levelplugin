package me.nakilex.levelplugin.debug;

import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.lootchests.utils.LocationUtils;
import me.nakilex.levelplugin.utils.NexoUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Debug helper that spawns a Nexo beacon furniture entity in front of players and keeps it updated.
 */
public class BeaconEntityDebugManager implements Listener {
    private static final String FURNITURE_ID = "base_beacon_magenta_inventory";
    private static final double FOLLOW_DISTANCE = 20.0;

    private final Main plugin;
    private final Map<UUID, BeaconState> active = new HashMap<>();

    public BeaconEntityDebugManager(Main plugin) {
        this.plugin = plugin;
    }

    public ToggleOutcome toggle(Player player) {
        if (player == null) {
            return new ToggleOutcome(false, false, ChatColor.RED + "Players only.");
        }
        UUID id = player.getUniqueId();
        if (active.containsKey(id)) {
            remove(player);
            return new ToggleOutcome(true, false, null);
        }
        FurnitureMechanic mechanic = NexoFurniture.furnitureMechanic(FURNITURE_ID);
        if (mechanic == null) {
            plugin.getLogger().warning("[BeaconEntityDebug] Unknown furniture '" + FURNITURE_ID + "'.");
            NexoUtil.logAvailableFurnitureIds(plugin.getLogger());
            return new ToggleOutcome(false, false,
                    ChatColor.RED + "Nexo furniture '" + FURNITURE_ID + "' is not registered.");
        }
        BeaconState state = new BeaconState();
        state.display = spawnBeacon(player);
        if (state.display == null) {
            return new ToggleOutcome(false, false, ChatColor.RED + "Could not spawn beacon entity.");
        }
        state.task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> updateBeacon(player), 1L, 1L);
        active.put(id, state);
        return new ToggleOutcome(true, true, null);
    }

    public void remove(Player player) {
        if (player == null) return;
        BeaconState state = active.remove(player.getUniqueId());
        if (state == null) return;
        if (state.task != null) {
            state.task.cancel();
        }
        if (state.display != null && !state.display.isDead()) {
            NexoFurniture.remove(state.display);
        }
    }

    public void removeAll() {
        for (UUID id : new HashMap<>(active).keySet()) {
            Player player = plugin.getServer().getPlayer(id);
            if (player != null) {
                remove(player);
            } else {
                BeaconState state = active.remove(id);
                if (state != null && state.display != null && !state.display.isDead()) {
                    NexoFurniture.remove(state.display);
                }
                if (state != null && state.task != null) {
                    state.task.cancel();
                }
            }
        }
        active.clear();
    }

    private void updateBeacon(Player player) {
        if (player == null || !player.isOnline()) {
            if (player != null) {
                remove(player);
            }
            return;
        }
        BeaconState state = active.get(player.getUniqueId());
        if (state == null) return;
        if (state.display == null || state.display.isDead()) {
            state.display = spawnBeacon(player);
            return;
        }
        Location target = getBeaconTarget(player);
        if (target != null) {
            state.display.teleport(target);
        }
    }

    private ItemDisplay spawnBeacon(Player player) {
        Location target = getBeaconTarget(player);
        if (target == null) return null;
        return NexoFurniture.place(FURNITURE_ID, target, 0f, BlockFace.NORTH);
    }

    private Location getBeaconTarget(Player player) {
        Location base = player.getLocation();
        Vector direction = base.getDirection().clone();
        direction.setY(0);
        if (direction.lengthSquared() < 0.001) {
            direction = base.getDirection();
        }
        direction.normalize();
        Location target = base.clone().add(direction.multiply(FOLLOW_DISTANCE));
        target = LocationUtils.aboveSurface(target);
        target = LocationUtils.centerOnBlock(target);
        if (target != null) {
            target.setYaw(base.getYaw());
            target.setPitch(0f);
        }
        return target;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        remove(event.getPlayer());
    }

    public record ToggleOutcome(boolean success, boolean enabled, String errorMessage) {}

    private static class BeaconState {
        private ItemDisplay display;
        private BukkitTask task;
    }
}
