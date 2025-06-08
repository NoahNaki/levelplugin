package me.nakilex.levelplugin.fasttravel.display;

import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import me.nakilex.levelplugin.fasttravel.data.Waystone;
import me.nakilex.levelplugin.fasttravel.data.WaystoneType;
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
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages client side hiding/showing of waystones.
 */
public class ClientWaystoneManager implements Listener {
    public static final NamespacedKey KEY = new NamespacedKey("levelplugin", "waystone-id");

    private final FastTravelManager manager;
    private final Map<UUID, Map<Location, DisplayInfo>> displays = new HashMap<>();

    private record DisplayInfo(BlockData previous, ItemDisplay display) {}

    public ClientWaystoneManager(FastTravelManager manager) {
        this.manager = manager;
    }

    /** Show the locked state to the player. */
    public void lock(Player player, Waystone ws) {
        Location loc = ws.getLocation();
        Map<Location, DisplayInfo> map = displays.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        if (map.containsKey(loc)) return;

        // hide real furniture entities
        hideFurniture(player, loc);

        BlockData prev = loc.getBlock().getBlockData();
        player.sendBlockChange(loc, Material.AIR.createBlockData());

        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(40007); // inert model
            item.setItemMeta(meta);
        }

        ItemDisplay disp = spawnDisplay(loc, item, ws.getName());
        // ensure only the given player can see and interact with this display
        for (Player p : Bukkit.getOnlinePlayers()) if (!p.equals(player)) p.hideEntity(manager.getPlugin(), disp);
        map.put(loc, new DisplayInfo(prev, disp));
    }

    /** Show the real waystone to the player. */
    public void unlock(Player player, Waystone ws) {
        Location loc = ws.getLocation();
        Map<Location, DisplayInfo> map = displays.get(player.getUniqueId());
        if (map != null) {
            DisplayInfo info = map.remove(loc);
            if (info != null) {
                player.sendBlockChange(loc, info.previous);
                info.display.remove();
            }
        }
        showFurniture(player, loc);
    }

    /** Update all waystones for a player based on unlock state. */
    public void refresh(Player player) {
        for (Waystone ws : manager.getWaystones()) {
            if (manager.isUnlocked(player, ws.getName())) {
                unlock(player, ws);
            } else {
                lock(player, ws);
            }
        }
    }

    private ItemDisplay spawnDisplay(Location loc, ItemStack item, String name) {
        return loc.getWorld().spawn(loc.clone().add(0.5, 0, 0.5), ItemDisplay.class, d -> {
            d.setItemStack(item);
            d.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
            // enlarge hitbox slightly so players can click it easily
            Transformation tf = new Transformation(new Vector3f(0f, 0f, 0f), new Quaternionf(), new Vector3f(2f,2f,2f), new Quaternionf());
            d.setTransformation(tf);
            d.setPersistent(false);
            d.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, name);
        });
    }

    private void hideFurniture(Player player, Location loc) {
        for (Entity ent : loc.getWorld().getNearbyEntities(loc, 1.0, 1.5, 1.0)) {
            if (ent instanceof Player) continue;
            player.hideEntity(manager.getPlugin(), ent);
        }
    }

    private void showFurniture(Player player, Location loc) {
        for (Entity ent : loc.getWorld().getNearbyEntities(loc, 1.0, 1.5, 1.0)) {
            if (ent instanceof Player) continue;
            player.showEntity(manager.getPlugin(), ent);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> refresh(event.getPlayer()), 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Map<Location, DisplayInfo> map = displays.remove(event.getPlayer().getUniqueId());
        if (map != null) {
            for (Map.Entry<Location, DisplayInfo> e : map.entrySet()) {
                event.getPlayer().sendBlockChange(e.getKey(), e.getValue().previous);
                e.getValue().display.remove();
            }
        }
    }
}
