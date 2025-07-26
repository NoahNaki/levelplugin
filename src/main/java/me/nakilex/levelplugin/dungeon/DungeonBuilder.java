package me.nakilex.levelplugin.dungeon;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Simplified in‑world dungeon builder using connector holograms. This is not a
 * full implementation but demonstrates the basic idea of placing rooms via
 * hologram interaction rather than the old GUI layout editor.
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
        ItemStack wool = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = wool.getItemMeta();
        if (meta != null) meta.setDisplayName(ChatColor.GREEN + "Place Entrance");
        wool.setItemMeta(meta);
        player.getInventory().addItem(wool);
        player.sendMessage(ChatColor.YELLOW + "Right-click to place the entrance at your feet.");
    }

    public void undo(Player player) {
        Session s = sessions.get(player.getUniqueId());
        if (s != null) s.undo();
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Session s = sessions.get(player.getUniqueId());
        if (s == null) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) return;
        ItemStack hand = event.getItem();
        if (hand == null || hand.getType() != Material.LIME_WOOL) return;
        event.setCancelled(true);
        if (!s.placingEntrance) return;
        Location loc;
        if (event.getClickedBlock() != null) {
            loc = event.getClickedBlock().getLocation().add(0, 1, 0);
        } else {
            loc = player.getLocation().getBlock().getLocation();
        }
        DungeonManager.PasteResult result = manager.pasteRoom(s.dungeon, manager.getEntrance(), 0, loc);
        player.sendMessage(ChatColor.GRAY + String.format("Overlap: %.1f%%", result.overlap() * 100));
        if (!result.success()) {
            player.sendMessage(ChatColor.RED + "Cannot place entrance here.");
            return;
        }
        s.history.push(new History(null, spawnConnectors(s, loc, manager.getEntrance(), 0, null),
                new Dungeon.RoomInstance(manager.getEntrance(), 0, loc.clone())));
        s.placingEntrance = false;
        player.sendMessage(ChatColor.GREEN + "Entrance placed. Use holograms to add rooms.");
    }

    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        Session s = sessions.get(player.getUniqueId());
        if (s == null) return;
        if (!(event.getRightClicked() instanceof Interaction inter)) return;
        ConnectorInfo info = s.connectors.get(inter.getEntityId());
        if (info == null) return;
        event.setCancelled(true);
        s.pending = info;
        player.openInventory(createRoomSelect());
    }

    @EventHandler
    public void onInv(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Session s = sessions.get(player.getUniqueId());
        if (s == null) return;
        if (s.pending == null) return;
        String title = event.getView().getTitle();
        if (!title.startsWith(ChatColor.DARK_GREEN + "Select")) return;
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        if (title.contains("Room")) {
            if (item.getType() == Material.YELLOW_WOOL) {
                player.openInventory(createVariantSelect());
            }
        } else if (title.contains("Variant")) {
            RoomTemplate templ = switch (item.getType()) {
                case RED_WOOL -> manager.getDeadEnd();
                case ORANGE_WOOL -> manager.getStraight();
                case GREEN_WOOL -> manager.getCornerLeft();
                case LIME_WOOL -> manager.getCornerRight();
                case BLUE_WOOL -> manager.getTJunction();
                case PURPLE_WOOL -> manager.getCrossroad();
                case BLACK_WOOL -> manager.getBoss();
                default -> null;
            };
            if (templ != null) {
                placeVariant(s, templ);
                player.closeInventory();
            }
        }
    }

    private void placeVariant(Session s, RoomTemplate templ) {
        ConnectorInfo info = s.pending;
        s.pending = null;
        Location base = info.location;
        int rotation = 0;
        RoomTemplate.Connector match = null;

        // Special handling for left/right corners to ensure exit orientation
        if (templ == manager.getCornerLeft() || templ == manager.getCornerRight()) {
            boolean right = templ == manager.getCornerRight();
            Direction exitDir = rotate(info.facing, right ? 1 : 3);
            outer:
            for (int r = 0; r < 4; r++) {
                for (RoomTemplate.Connector c : templ.getConnectors()) {
                    if (rotate(c.facing, r) != info.facing.opposite()) continue;
                    for (RoomTemplate.Connector o : templ.getConnectors()) {
                        if (o == c) continue;
                        if (rotate(o.facing, r) == exitDir) {
                            rotation = r;
                            match = c;
                            break outer;
                        }
                    }
                }
            }
        }

        if (match == null) {
            for (int r = 0; r < 4; r++) {
                for (RoomTemplate.Connector c : templ.getConnectors()) {
                    if (rotate(c.facing, r) == info.facing.opposite()) {
                        rotation = r;
                        match = c;
                        break;
                    }
                }
                if (match != null) break;
            }
        }
        if (match == null) return;
        int[] vec = RoomTemplate.rotate(match.x - (int) Math.round(templ.getCenterX()),
                match.z - (int) Math.round(templ.getCenterZ()), rotation);
        Location center = base.clone().subtract(vec[0], match.bottomY - templ.getConnectorMinY(), vec[1]);
        DungeonManager.PasteResult result = manager.pasteRoom(s.dungeon, templ, rotation, center);
        s.player.sendMessage(ChatColor.GRAY + String.format("Overlap: %.1f%%", result.overlap() * 100));
        if (!result.success()) {
            s.player.sendMessage(ChatColor.RED + "Room collides with existing blocks.");
            return;
        }
        removeConnector(s, info);
        List<ConnectorInfo> added = spawnConnectors(s, center, templ, rotation, info);
        s.history.push(new History(info, added, new Dungeon.RoomInstance(templ, rotation, center.clone())));
    }

    private List<ConnectorInfo> spawnConnectors(Session s, Location center, RoomTemplate templ, int rotation, ConnectorInfo used) {
        List<ConnectorInfo> list = new ArrayList<>();
        for (RoomTemplate.Connector c : templ.getConnectors()) {
            Direction dir = rotate(c.facing, rotation);
            if (used != null && dir == used.facing.opposite()) continue;
            int[] vec = RoomTemplate.rotate(c.x - (int) Math.round(templ.getCenterX()),
                    c.z - (int) Math.round(templ.getCenterZ()), rotation);
            Location loc = center.clone().add(vec[0], c.bottomY - templ.getConnectorMinY(), vec[1]);
            ConnectorInfo info = spawnConnector(s, loc, dir);
            s.connectors.put(info.interaction.getEntityId(), info);
            list.add(info);
        }
        return list;
    }

    private ConnectorInfo spawnConnector(Session s, Location loc, Direction dir) {
        Interaction inter = loc.getWorld().spawn(loc, Interaction.class, i -> {
            i.setInvulnerable(true);
            i.setGravity(false);
            i.setInteractionWidth(2.0f);
            i.setInteractionHeight(2.0f);
            i.addScoreboardTag("dungeon_hologram");
        });
        TextDisplay display = (TextDisplay) loc.getWorld().spawn(loc.clone().add(0, 1.2, 0), TextDisplay.class);
        display.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
        display.setText(ChatColor.AQUA + "Place room");
        display.setShadowRadius(0);
        display.setShadowStrength(0);
        return new ConnectorInfo(loc, dir, s.player, inter, display);
    }

    private void removeConnector(Session s, ConnectorInfo info) {
        info.interaction.remove();
        info.display.remove();
        s.connectors.remove(info.interaction.getEntityId());
    }

    private Inventory createRoomSelect() {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.DARK_GREEN + "Select Room");
        ItemStack hall = new ItemStack(Material.YELLOW_WOOL);
        ItemMeta meta = hall.getItemMeta();
        if (meta != null) meta.setDisplayName(ChatColor.YELLOW + "Hallway");
        hall.setItemMeta(meta);
        inv.setItem(0, hall);
        return inv;
    }

    private Inventory createVariantSelect() {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.DARK_GREEN + "Select Variant");
        inv.setItem(0, item(Material.RED_WOOL, ChatColor.RED + "Dead End"));
        inv.setItem(1, item(Material.ORANGE_WOOL, ChatColor.GOLD + "Straight"));
        inv.setItem(2, item(Material.GREEN_WOOL, ChatColor.GREEN + "Corner Left"));
        inv.setItem(3, item(Material.LIME_WOOL, ChatColor.GREEN + "Corner Right"));
        inv.setItem(4, item(Material.BLUE_WOOL, ChatColor.BLUE + "T-Junction"));
        inv.setItem(5, item(Material.PURPLE_WOOL, ChatColor.LIGHT_PURPLE + "Crossroad"));
        inv.setItem(8, item(Material.BLACK_WOOL, ChatColor.DARK_GRAY + "Boss"));
        return inv;
    }

    private ItemStack item(Material mat, String name) {
        ItemStack is = new ItemStack(mat);
        ItemMeta im = is.getItemMeta();
        if (im != null) im.setDisplayName(name);
        is.setItemMeta(im);
        return is;
    }

    private Direction rotate(Direction dir, int rot) {
        return Direction.values()[(dir.ordinal() + rot) & 3];
    }

    private static class ConnectorInfo {
        final Location location;
        final Direction facing;
        final Player player;
        final Interaction interaction;
        final TextDisplay display;
        ConnectorInfo(Location loc, Direction facing, Player player, Interaction i, TextDisplay d) {
            this.location = loc;
            this.facing = facing;
            this.player = player;
            this.interaction = i;
            this.display = d;
        }
    }

    private static class History {
        final ConnectorInfo used;
        final List<ConnectorInfo> added;
        final Dungeon.RoomInstance instance;
        History(ConnectorInfo used, List<ConnectorInfo> added, Dungeon.RoomInstance inst) {
            this.used = used;
            this.added = added;
            this.instance = inst;
        }
    }

    private class Session {
        final Player player;
        final Dungeon dungeon;
        final Deque<History> history = new ArrayDeque<>();
        final Map<Integer, ConnectorInfo> connectors = new HashMap<>();
        boolean placingEntrance = true;
        ConnectorInfo pending;
        Session(Player player) {
            this.player = player;
            this.dungeon = new Dungeon(player.getWorld(), player.getName() + "_builder");
        }
        void undo() {
            History h = history.poll();
            if (h == null) return;
            World world = h.instance.center.getWorld();
            for (RoomTemplate.BlockDef b : h.instance.template.getBlocks()) {
                Material mat = b.data.getMaterial();
                if (mat == Material.REDSTONE_BLOCK || mat == Material.PINK_WOOL) continue;
                int[] vec = RoomTemplate.rotate(b.x - (int) Math.round(h.instance.template.getCenterX()),
                        b.z - (int) Math.round(h.instance.template.getCenterZ()), h.instance.rotation);
                Location l = h.instance.center.clone().add(vec[0], b.y - h.instance.template.getConnectorMinY(), vec[1]);
                world.getBlockAt(l).setType(Material.AIR, false);
            }
            for (ConnectorInfo c : h.added) {
                DungeonBuilder.this.removeConnector(this, c);
            }
            if (h.used != null) {
                ConnectorInfo restored = DungeonBuilder.this.spawnConnector(this, h.used.location, h.used.facing);
                connectors.put(restored.interaction.getEntityId(), restored);
            }
        }
    }
}
