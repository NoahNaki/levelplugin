package me.nakilex.levelplugin.fasttravel.display;

import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import me.nakilex.levelplugin.fasttravel.data.Waystone;
import me.nakilex.levelplugin.fasttravel.data.WaystoneType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles client-side display of unlocked waystones using simple block changes.
 */
public class ClientWaystoneManager implements Listener {
    private final FastTravelManager manager;
    private final ProtocolManager protocol;
    private final Map<UUID, Map<Location, BlockData>> shown = new HashMap<>();

    public ClientWaystoneManager(FastTravelManager manager) {
        this.manager = manager;
        this.protocol = ProtocolLibrary.getProtocolManager();
    }

    /** Show the active waystone block to a single player. */
    public void show(Player player, Waystone ws) {
        Location loc = ws.getLocation();

        // Hide any furniture entities at this location for this player
        for (Entity ent : loc.getWorld().getNearbyEntities(loc, 1.0, 1.5, 1.0)) {
            if (ent instanceof Player) continue;
            try {
                PacketContainer destroy = protocol.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
                destroy.getIntLists().write(0, java.util.Collections.singletonList(ent.getEntityId()));
                protocol.sendServerPacket(player, destroy);
            } catch (Exception ignore) {}
        }

        BlockData prev = loc.getBlock().getBlockData();
        BlockData data = getData(ws.getType());
        player.sendBlockChange(loc, data);
        shown.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(loc, prev);
    }

    /** Re-send unlocked waystones to the player. */
    public void refresh(Player player) {
        for (String name : manager.getUnlocked(player)) {
            Waystone ws = manager.getWaystone(name);
            if (ws != null) {
                show(player, ws);
            }
        }
    }

    private BlockData getData(WaystoneType type) {
        Material mat = type == WaystoneType.TOWN ? Material.LODESTONE : Material.NETHER_BRICKS;
        return mat.createBlockData();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> refresh(event.getPlayer()), 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Map<Location, BlockData> map = shown.remove(event.getPlayer().getUniqueId());
        if (map != null) {
            for (Map.Entry<Location, BlockData> e : map.entrySet()) {
                event.getPlayer().sendBlockChange(e.getKey(), e.getValue());
            }
        }
    }
}
