package me.nakilex.levelplugin.dungeon;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Simple in-world dungeon editor using holograms for room placement.
 */
public class DungeonBuilder implements Listener {
    private final DungeonManager manager;
    private final Map<UUID, Session> sessions = new HashMap<>();

    public DungeonBuilder(DungeonManager manager) {
        this.manager = manager;
    }

    public void start(Player player) {
        Session s = new Session(player);
        sessions.put(player.getUniqueId(), s);
        player.getInventory().clear();
        player.getInventory().addItem(createItem(Material.LIME_WOOL, ChatColor.GREEN + "Place Entrance"));
        player.sendMessage(ChatColor.YELLOW + "Dungeon edit mode: right-click to place entrance");
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m != null) m.setDisplayName(name);
        it.setItemMeta(m);
        return it;
    }

    private static class Session {
        final Player player;
        final Dungeon dungeon;
        final Map<Integer, ConnectorInfo> connectors = new HashMap<>();
        int nextId = 0;
        boolean entrancePlaced = false;
        int pending = -1;

        Session(Player player) {
            this.player = player;
            this.dungeon = new Dungeon(player.getWorld(), player.getName() + "_edit");
        }
    }

    private static class ConnectorInfo {
        final Location location;
        final Direction facing;
        final List<org.bukkit.entity.Entity> entities;
        ConnectorInfo(Location loc, Direction facing, List<org.bukkit.entity.Entity> entities) {
            this.location = loc; this.facing = facing; this.entities = entities;
        }
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        Session s = sessions.get(e.getPlayer().getUniqueId());
        if (s == null) return;
        ItemStack item = e.getItem();
        if (item == null) return;
        if (item.getType() == Material.LIME_WOOL && e.getAction().toString().contains("RIGHT")) {
            if (s.entrancePlaced) {
                e.getPlayer().sendMessage(ChatColor.RED + "Entrance already placed");
                return;
            }
            Location loc = e.getPlayer().getLocation();
            manager.pasteRoom(s.dungeon, manager.getEntranceTemplate(), 0, loc);
            spawnConnectors(s, manager.getEntranceTemplate(), 0, loc);
            s.entrancePlaced = true;
            e.setCancelled(true);
        }
    }

    private void spawnConnectors(Session s, RoomTemplate templ, int rotation, Location center) {
        for (RoomTemplate.Connector c : templ.getConnectors()) {
            int[] vec = RoomTemplate.rotate(c.x - (int)Math.round(templ.getCenterX()), c.z - (int)Math.round(templ.getCenterZ()), rotation);
            int wx = center.getBlockX() + vec[0];
            int wy = center.getBlockY() + (c.bottomY - templ.getConnectorMinY());
            int wz = center.getBlockZ() + vec[1];
            Location holo = new Location(center.getWorld(), wx + 0.5, wy + 1.1, wz + 0.5);
            int id = s.nextId++;
            List<org.bukkit.entity.Entity> ents = spawnHologram(s.player, holo, id);
            s.connectors.put(id, new ConnectorInfo(new Location(center.getWorld(), wx, wy, wz), Direction.values()[(c.facing.ordinal() + rotation) & 3], ents));
        }
    }

    private List<org.bukkit.entity.Entity> spawnHologram(Player viewer, Location at, int id) {
        Interaction click = at.getWorld().spawn(at, Interaction.class, it -> {
            it.setInteractionWidth(1f);
            it.setInteractionHeight(1f);
            it.addScoreboardTag("dungeon_holo:" + id);
        });
        TextDisplay td = at.getWorld().spawn(at, TextDisplay.class, d -> {
            d.setBillboard(Display.Billboard.CENTER);
            d.setShadowRadius(0f);
            d.setShadowStrength(0f);
            d.setText(ChatColor.AQUA + "Place room");
            d.addScoreboardTag("dungeon_holo:" + id);
        });
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(viewer)) {
                p.hideEntity(Main.getInstance(), click);
                p.hideEntity(Main.getInstance(), td);
            }
        }
        return java.util.Arrays.asList(click, td);
    }

    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent e) {
        Session s = sessions.get(e.getPlayer().getUniqueId());
        if (s == null) return;
        for (String tag : e.getRightClicked().getScoreboardTags()) {
            if (tag.startsWith("dungeon_holo:")) {
                e.setCancelled(true);
                int id = Integer.parseInt(tag.substring("dungeon_holo:".length()));
                s.pending = id;
                e.getPlayer().openInventory(createSelectInv());
                return;
            }
        }
    }

    private Inventory createSelectInv() {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.DARK_GREEN + "Select Room");
        inv.setItem(0, createItem(Material.YELLOW_WOOL, ChatColor.YELLOW + "Hallway"));
        return inv;
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        Session s = sessions.get(e.getWhoClicked().getUniqueId());
        if (s == null) return;
        if (s.pending < 0) return;
        if (e.getView().getTitle().contains("Select Room")) {
            e.setCancelled(true);
            ItemStack it = e.getCurrentItem();
            if (it == null) return;
            if (it.getType() == Material.YELLOW_WOOL) {
                e.getWhoClicked().openInventory(createVariantInv());
            }
        } else if (e.getView().getTitle().contains("Hallway Variant")) {
            e.setCancelled(true);
            ItemStack it = e.getCurrentItem();
            if (it == null) return;
            RoomTemplate tmpl = null;
            if (it.getType() == Material.RED_WOOL) tmpl = manager.getDeadEndTemplate();
            if (it.getType() == Material.ORANGE_WOOL) tmpl = manager.getStraightTemplate();
            if (it.getType() == Material.GREEN_WOOL) tmpl = manager.getCornerTemplate();
            if (it.getType() == Material.BLUE_WOOL) tmpl = manager.getTJunctionTemplate();
            if (it.getType() == Material.PURPLE_WOOL) tmpl = manager.getCrossroadTemplate();
            if (tmpl != null) {
                ConnectorInfo info = s.connectors.remove(s.pending);
                if (info != null) info.entities.forEach(org.bukkit.entity.Entity::remove);
                int rot = yawToRotation(e.getWhoClicked().getLocation().getYaw());
                Location center = computeCenter(tmpl, rot, info.location, info.facing.opposite());
                manager.pasteRoom(s.dungeon, tmpl, rot, center);
                spawnConnectors(s, tmpl, rot, center);
            }
            s.pending = -1;
            e.getWhoClicked().closeInventory();
        }
    }

    private Inventory createVariantInv() {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.DARK_GREEN + "Hallway Variant");
        inv.setItem(0, createItem(Material.RED_WOOL, ChatColor.RED + "Dead End"));
        inv.setItem(1, createItem(Material.ORANGE_WOOL, ChatColor.GOLD + "Straight"));
        inv.setItem(2, createItem(Material.GREEN_WOOL, ChatColor.GREEN + "Corner"));
        inv.setItem(3, createItem(Material.BLUE_WOOL, ChatColor.BLUE + "T-Junction"));
        inv.setItem(4, createItem(Material.PURPLE_WOOL, ChatColor.LIGHT_PURPLE + "Crossroad"));
        return inv;
    }

    private int yawToRotation(float yaw) {
        return Math.floorMod(Math.round(yaw / 90f), 4);
    }

    private Location computeCenter(RoomTemplate tmpl, int rotation, Location target, Direction facing) {
        for (RoomTemplate.Connector c : tmpl.getConnectors()) {
            Direction dir = Direction.values()[(c.facing.ordinal() + rotation) & 3];
            if (dir == facing) {
                int[] vec = RoomTemplate.rotate(c.x - (int)Math.round(tmpl.getCenterX()), c.z - (int)Math.round(tmpl.getCenterZ()), rotation);
                int cx = target.getBlockX() - vec[0];
                int cy = target.getBlockY() - (c.bottomY - tmpl.getConnectorMinY());
                int cz = target.getBlockZ() - vec[1];
                return new Location(target.getWorld(), cx, cy, cz);
            }
        }
        return target;
    }
}
