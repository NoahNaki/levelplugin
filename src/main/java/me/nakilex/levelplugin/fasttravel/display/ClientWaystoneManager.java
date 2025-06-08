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
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles client-side display of unlocked waystones using simple block changes.
 */
public class ClientWaystoneManager implements Listener {
    public static final NamespacedKey KEY = new NamespacedKey("levelplugin", "waystone-id");

    private final FastTravelManager manager;
    private final ProtocolManager protocol;
    private final Map<UUID, Map<Location, DisplayInfo>> shown = new HashMap<>();

    private record DisplayInfo(BlockData previous, ItemDisplay display) {}

    public ClientWaystoneManager(FastTravelManager manager) {
        this.manager = manager;
        this.protocol = ProtocolLibrary.getProtocolManager();
    }

    /** Show the active waystone block to a single player. */
    public void show(Player player, Waystone ws) {
        Location loc = ws.getLocation();

        // Hide the inert furniture entity from this player only
        for (Entity ent : loc.getWorld().getNearbyEntities(loc, 1.0, 1.5, 1.0)) {
            if (ent instanceof Player) continue;
            try {
                PacketContainer destroy = protocol.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
                destroy.getIntLists().write(0, java.util.Collections.singletonList(ent.getEntityId()));
                protocol.sendServerPacket(player, destroy);
            } catch (Exception ignore) {}
        }

        // Remove any previous display we spawned here for this player
        Map<Location, DisplayInfo> map = shown.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        DisplayInfo old = map.get(loc);
        if (old != null) {
            old.display.remove();
            player.sendBlockChange(loc, old.previous);
        }

        BlockData prev = loc.getBlock().getBlockData();
        player.sendBlockChange(loc, Material.AIR.createBlockData());

        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            int cmd = ws.getType() == WaystoneType.TOWN ? 40001 : 40015;
            meta.setCustomModelData(cmd);
            item.setItemMeta(meta);
        }

        ItemDisplay disp = loc.getWorld().spawn(loc.clone().add(0.5, 0, 0.5), ItemDisplay.class, d -> {
            d.setItemStack(item);
            d.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
            d.setTransformation(new Transformation(new Vector3f(), new Vector3f(), new Vector3f(1,1,1), new Vector3f()));
            d.setPersistent(false);
            d.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, ws.getName());
        });

        // Hide from all other players
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(player)) p.hideEntity(manager.getPlugin(), disp);
        }

        map.put(loc, new DisplayInfo(prev, disp));
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
        Map<Location, DisplayInfo> map = shown.remove(event.getPlayer().getUniqueId());
        if (map != null) {
            for (Map.Entry<Location, DisplayInfo> e : map.entrySet()) {
                event.getPlayer().sendBlockChange(e.getKey(), e.getValue().previous);
                e.getValue().display.remove();
            }
        }
    }
}
