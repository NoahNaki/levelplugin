package me.nakilex.levelplugin.dungeon;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import me.nakilex.levelplugin.utils.GuiUtil;

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
        setupInventory(player);
        player.sendMessage(ChatColor.YELLOW + "Right-click to place the entrance at your feet.");
    }

    public void edit(Player player, DungeonLayout layout) {
        Session s = new Session(player);
        sessions.put(player.getUniqueId(), s);
        setupInventory(player);

        // spawn existing rooms relative to the entrance
        Location origin = player.getLocation().getBlock().getLocation();

        int entranceX = -1, entranceZ = -1;
        for (int x = 0; x < DungeonLayout.WIDTH; x++) {
            for (int z = 0; z < DungeonLayout.HEIGHT; z++) {
                if (layout.get(x, z) == RoomType.ENTRANCE) { entranceX = x; entranceZ = z; break; }
            }
            if (entranceX != -1) break;
        }

        // place entrance first so it becomes the first room in the dungeon list
        if (entranceX != -1) {
            RoomType type = layout.get(entranceX, entranceZ);
            Set<Direction> dirs = new HashSet<>();
            if (layout.get(entranceX + 1, entranceZ) != RoomType.NONE) dirs.add(Direction.EAST);
            if (layout.get(entranceX - 1, entranceZ) != RoomType.NONE) dirs.add(Direction.WEST);
            if (layout.get(entranceX, entranceZ + 1) != RoomType.NONE) dirs.add(Direction.SOUTH);
            if (layout.get(entranceX, entranceZ - 1) != RoomType.NONE) dirs.add(Direction.NORTH);
            RoomTemplate templ = manager.chooseTemplate(type, dirs);
            int rotation = layout.getRotation(entranceX, entranceZ);
            Location center = origin.clone();
            String mob = layout.getMob(entranceX, entranceZ);
            manager.pasteRoom(s.dungeon, templ, rotation, center, mob);
            for (RoomTemplate.Connector c : templ.getConnectors()) {
                Direction dir = rotate(c.facing, rotation);
                if (dirs.contains(dir)) continue;
                int[] vec = RoomTemplate.rotate(c.x - (int)Math.round(templ.getCenterX()),
                        c.z - (int)Math.round(templ.getCenterZ()), rotation);
                Location loc = center.clone().add(vec[0], c.bottomY - templ.getConnectorMinY(), vec[1]);
                ConnectorInfo info = spawnConnector(s, loc, dir);
                s.connectors.put(info.interaction.getEntityId(), info);
            }
        }

        for (int x = 0; x < DungeonLayout.WIDTH; x++) {
            for (int z = 0; z < DungeonLayout.HEIGHT; z++) {
                if (x == entranceX && z == entranceZ) continue;
                RoomType type = layout.get(x, z);
                if (type == RoomType.NONE) continue;

                Set<Direction> dirs = new HashSet<>();
                if (layout.get(x + 1, z) != RoomType.NONE) dirs.add(Direction.EAST);
                if (layout.get(x - 1, z) != RoomType.NONE) dirs.add(Direction.WEST);
                if (layout.get(x, z + 1) != RoomType.NONE) dirs.add(Direction.SOUTH);
                if (layout.get(x, z - 1) != RoomType.NONE) dirs.add(Direction.NORTH);

                RoomTemplate templ = manager.chooseTemplate(type, dirs);
                int rotation = (type == RoomType.COMBAT || type == RoomType.BOSS || type == RoomType.ENTRANCE
                        || type == RoomType.EXIT
                        || type == RoomType.TJUNCTION_LEFT || type == RoomType.TJUNCTION_RIGHT)
                        ? layout.getRotation(x, z)
                        : manager.findRotation(templ, dirs);
                int diffX = layout.getOffsetX(x, z);
                int diffZ = layout.getOffsetZ(x, z);
                Location center = origin.clone().add(diffX, 0, diffZ);
                String mob = layout.getMob(x, z);
                manager.pasteRoom(s.dungeon, templ, rotation, center, mob);

                // spawn connectors only for open sides
                for (RoomTemplate.Connector c : templ.getConnectors()) {
                    Direction dir = rotate(c.facing, rotation);
                    if (dirs.contains(dir)) continue; // neighbour already present
                    int[] vec = RoomTemplate.rotate(c.x - (int)Math.round(templ.getCenterX()),
                            c.z - (int)Math.round(templ.getCenterZ()), rotation);
                    Location loc = center.clone().add(vec[0], c.bottomY - templ.getConnectorMinY(), vec[1]);
                    ConnectorInfo info = spawnConnector(s, loc, dir);
                    s.connectors.put(info.interaction.getEntityId(), info);
                }
            }
        }

        s.placingEntrance = (entranceX == -1);
        if (entranceX == -1) {
            player.sendMessage(ChatColor.YELLOW + "Right-click to place the entrance at your feet.");
        }
    }

    private void setupInventory(Player player) {
        player.getInventory().clear();
        ItemStack wool = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = wool.getItemMeta();
        if (meta != null) meta.setDisplayName(ChatColor.GREEN + "Place Entrance");
        wool.setItemMeta(meta);
        player.getInventory().setItem(0, wool);
        ItemStack save = new ItemStack(Material.EMERALD);
        ItemMeta sm = save.getItemMeta();
        if (sm != null) sm.setDisplayName(ChatColor.AQUA + "Save");
        save.setItemMeta(sm);
        player.getInventory().setItem(7, save);
        ItemStack cancel = new ItemStack(Material.BARRIER);
        ItemMeta cm = cancel.getItemMeta();
        if (cm != null) cm.setDisplayName(ChatColor.RED + "Cancel");
        cancel.setItemMeta(cm);
        player.getInventory().setItem(8, cancel);
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
        if (hand == null) return;
        Material type = hand.getType();
        if (type == Material.EMERALD) {
            event.setCancelled(true);
            s.awaitingName = true;
            player.sendMessage(ChatColor.YELLOW + "Type dungeon name in chat or 'cancel'.");
            return;
        }
        if (type == Material.BARRIER) {
            event.setCancelled(true);
            s.cancel();
            sessions.remove(player.getUniqueId());
            player.getInventory().clear();
            player.sendMessage(ChatColor.RED + "Dungeon build cancelled.");
            return;
        }
        if (type != Material.LIME_WOOL) return;
        event.setCancelled(true);
        if (!s.placingEntrance) return;
        Location loc;
        if (event.getClickedBlock() != null) {
            loc = event.getClickedBlock().getLocation().add(0, 1, 0);
        } else {
            loc = player.getLocation().getBlock().getLocation();
        }
        Direction facing = Direction.fromYaw(player.getLocation().getYaw());
        RoomTemplate entrance = manager.getEntrance();
        RoomTemplate.Connector conn = entrance.getConnectors().get(0);
        int rot = 0;
        for (int r = 0; r < 4; r++) {
            if (rotate(conn.facing, r) == facing) { rot = r; break; }
        }
        DungeonManager.PasteResult result = manager.pasteRoom(s.dungeon, entrance, rot, loc, null);
        player.sendMessage(ChatColor.GRAY + String.format("Overlap: %.1f%%", result.overlap() * 100));
        if (!result.success()) {
            player.sendMessage(ChatColor.RED + "Cannot place entrance here.");
            return;
        }
        s.history.push(new History(null, spawnConnectors(s, loc, entrance, rot, null),
                result.instance(), result.replaced()));
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
        if (s == null || s.pending == null) return;

        String rawTitle = ChatColor.stripColor(event.getView().getTitle());
        if (!rawTitle.startsWith("Select") && !rawTitle.endsWith("Variants")) return;

        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());

        switch (rawTitle) {
            case "Select Room" -> {
                Material type = item.getType();
                if (type == Material.YELLOW_WOOL) {
                    player.openInventory(createVariantSelect());
                } else if (type == Material.RED_WOOL) {
                    player.openInventory(createCombatVariantSelect());
                } else if (type == Material.BLACK_WOOL) {
                    placeVariant(s, manager.getBoss());
                    player.closeInventory();
                } else if (type == Material.OBSIDIAN) {
                    placeVariant(s, manager.getExit());
                    player.closeInventory();
                }
            }
            case "Hallway Variants" -> {
                if (name.equalsIgnoreCase("Back")) {
                    player.openInventory(createRoomSelect());
                    return;
                }
                RoomTemplate templ = switch (item.getType()) {
                    case RED_WOOL -> manager.getDeadEnd();
                    case ORANGE_WOOL -> manager.getStraight();
                    case GREEN_WOOL -> manager.getCornerLeft();
                    case LIME_WOOL -> manager.getCornerRight();
                    case BLUE_WOOL -> manager.getTJunctionLeft();
                    case CYAN_WOOL -> manager.getTJunctionRight();
                    case PURPLE_WOOL -> manager.getCrossroad();
                    default -> null;
                };
                if (templ != null) {
                    placeVariant(s, templ);
                    player.closeInventory();
                }
            }
            case "Combat Variants" -> {
                if (name.equalsIgnoreCase("Back")) {
                    player.openInventory(createRoomSelect());
                    return;
                }
                RoomTemplate templ = switch (item.getType()) {
                    case GRAY_WOOL -> manager.getCombatLeft();
                    case LIGHT_GRAY_WOOL -> manager.getCombatRight();
                    default -> null;
                };
                if (templ != null) {
                    s.selectedTemplate = templ;
                    player.openInventory(createMobSelect());
                }
            }
            case "Select Mob" -> {
                if (name.equalsIgnoreCase("Back")) {
                    player.openInventory(createCombatVariantSelect());
                    return;
                }
                s.selectedMob = name;
                if (s.selectedTemplate != null) {
                    placeVariant(s, s.selectedTemplate);
                    player.closeInventory();
                    s.selectedTemplate = null;
                }
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Session s = sessions.get(event.getPlayer().getUniqueId());
        if (s == null || !s.awaitingName) return;
        event.setCancelled(true);
        String msg = event.getMessage().trim();
        if (msg.equalsIgnoreCase("cancel")) {
            s.awaitingName = false;
            event.getPlayer().sendMessage(ChatColor.RED + "Save cancelled.");
            return;
        }
        Bukkit.getScheduler().runTask(manager.getPlugin(), () -> {
            DungeonLayout layout = s.buildLayout();
            if (!layout.hasEntrance() || !layout.hasExit()) {
                event.getPlayer().sendMessage(ChatColor.RED + "Dungeon requires an entrance and exit.");
                s.awaitingName = false;
                return;
            }
            manager.saveLayout(msg, layout);
            s.cancel();
            event.getPlayer().sendMessage(ChatColor.GREEN + "Dungeon saved as '" + msg + "'");
            event.getPlayer().getInventory().clear();
            sessions.remove(event.getPlayer().getUniqueId());
        });
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

        // Handle T-junction variants with orientation-specific side exits
        if (match == null && (templ == manager.getTJunctionLeft() || templ == manager.getTJunctionRight())) {
            boolean right = templ == manager.getTJunctionRight();
            Direction sideDir = rotate(info.facing, right ? 1 : 3);
            Direction forwardDir = info.facing;
            outer:
            for (int r = 0; r < 4; r++) {
                for (RoomTemplate.Connector c : templ.getConnectors()) {
                    if (rotate(c.facing, r) != info.facing.opposite()) continue;
                    boolean foundSide = false;
                    boolean foundForward = false;
                    for (RoomTemplate.Connector o : templ.getConnectors()) {
                        if (o == c) continue;
                        Direction d = rotate(o.facing, r);
                        if (d == sideDir) foundSide = true;
                        if (d == forwardDir) foundForward = true;
                    }
                    if (foundSide && foundForward) {
                        rotation = r;
                        match = c;
                        break outer;
                    }
                }
            }
        }

        if (match == null) {
            List<RoomTemplate.Connector> entrances = new ArrayList<>();
            for (RoomTemplate.Connector c : templ.getConnectors()) {
                if (c.entrance) entrances.add(c);
            }

            if (entrances.size() == 1) {
                RoomTemplate.Connector c = entrances.get(0);
                rotation = (info.facing.opposite().ordinal() - c.facing.ordinal()) & 3;
                match = c;
            } else {
                outer:
                for (int r = 0; r < 4; r++) {
                    for (RoomTemplate.Connector c : entrances) {
                        if (rotate(c.facing, r) == info.facing.opposite()) {
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
        String mob = (templ == manager.getCombatLeft() || templ == manager.getCombatRight()) ? s.selectedMob : null;
        DungeonManager.PasteResult result = manager.pasteRoom(s.dungeon, templ, rotation, center, mob);
        s.player.sendMessage(ChatColor.GRAY + String.format("Overlap: %.1f%%", result.overlap() * 100));
        if (!result.success()) {
            s.player.sendMessage(ChatColor.RED + "Room collides with existing blocks.");
            return;
        }
        removeConnector(s, info);
        List<ConnectorInfo> added = spawnConnectors(s, center, templ, rotation, info);
        s.history.push(new History(info, added, result.instance(), result.replaced()));
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
        display.addScoreboardTag("dungeon_hologram");
        return new ConnectorInfo(loc, dir, s.player, inter, display);
    }

    private void removeConnector(Session s, ConnectorInfo info) {
        info.interaction.remove();
        info.display.remove();
        s.connectors.remove(info.interaction.getEntityId());
    }

    private Inventory createRoomSelect() {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GREEN + "Select Room");

        ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        ItemStack hall = item(Material.YELLOW_WOOL, ChatColor.YELLOW + "Hallway");
        ItemStack boss = item(Material.BLACK_WOOL, ChatColor.DARK_GRAY + "Boss Room");
        ItemStack combat = item(Material.RED_WOOL, ChatColor.RED + "Combat Room");
        ItemStack exitRoom = item(Material.OBSIDIAN, ChatColor.DARK_PURPLE + "Exit Room");
        ItemMeta cMeta = combat.getItemMeta();
        if (cMeta != null) {
            cMeta.setLore(Arrays.asList(ChatColor.WHITE + "Left-click to place",
                    ChatColor.WHITE + "Right-click to edit"));
            combat.setItemMeta(cMeta);
        }

        inv.setItem(10, hall);
        inv.setItem(12, boss);
        inv.setItem(14, combat);
        inv.setItem(16, exitRoom);
        return inv;
    }

    private Inventory createVariantSelect() {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GREEN + "Hallway Variants");

        ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        inv.setItem(10, item(Material.RED_WOOL, ChatColor.RED + "Dead End"));
        inv.setItem(12, item(Material.ORANGE_WOOL, ChatColor.GOLD + "Straight"));
        inv.setItem(14, item(Material.GREEN_WOOL, ChatColor.GREEN + "Corner Left"));
        inv.setItem(16, item(Material.LIME_WOOL, ChatColor.GREEN + "Corner Right"));
        inv.setItem(20, item(Material.BLUE_WOOL, ChatColor.BLUE + "T-Junction Left"));
        inv.setItem(22, item(Material.CYAN_WOOL, ChatColor.BLUE + "T-Junction Right"));
        inv.setItem(24, item(Material.PURPLE_WOOL, ChatColor.LIGHT_PURPLE + "Crossroad"));

        inv.setItem(18, GuiUtil.getNexoItem("arrow_left", ChatColor.YELLOW + "Back"));
        return inv;
    }

    private Inventory createCombatVariantSelect() {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GREEN + "Combat Variants");

        ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        inv.setItem(11, item(Material.GRAY_WOOL, ChatColor.GRAY + "Combat Left"));
        inv.setItem(15, item(Material.LIGHT_GRAY_WOOL, ChatColor.GRAY + "Combat Right"));

        inv.setItem(18, GuiUtil.getNexoItem("arrow_left", ChatColor.YELLOW + "Back"));
        return inv;
    }

    private Inventory createMobSelect() {
        Set<String> mobs = manager.getAvailableMobs();
        int size = ((mobs.size() - 1) / 9 + 1) * 9;
        Inventory inv = Bukkit.createInventory(null, size, ChatColor.DARK_GREEN + "Select Mob");
        ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < size; i++) inv.setItem(i, filler);
        int idx = 0;
        for (String m : mobs) {
            ItemStack is = new ItemStack(Material.PAPER);
            ItemMeta im = is.getItemMeta();
            if (im != null) im.setDisplayName(ChatColor.WHITE + m);
            is.setItemMeta(im);
            inv.setItem(idx++, is);
        }
        inv.setItem(size - 1, GuiUtil.getNexoItem("arrow_left", ChatColor.YELLOW + "Back"));
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
        final Map<Location, BlockData> replaced;
        History(ConnectorInfo used, List<ConnectorInfo> added,
                Dungeon.RoomInstance inst, Map<Location, BlockData> replaced) {
            this.used = used;
            this.added = added;
            this.instance = inst;
            this.replaced = replaced;
        }
    }

    private class Session {
        final Player player;
        final Dungeon dungeon;
        final Deque<History> history = new ArrayDeque<>();
        final Map<Integer, ConnectorInfo> connectors = new HashMap<>();
        boolean placingEntrance = true;
        ConnectorInfo pending;
        boolean awaitingName = false;
        String selectedMob = null;
        RoomTemplate selectedTemplate = null;
        Session(Player player) {
            this.player = player;
            this.dungeon = new Dungeon(player.getWorld(), player.getName() + "_builder");
        }
        void undo() {
            History h = history.poll();
            if (h == null) return;
            World world = h.instance.center.getWorld();
            for (Map.Entry<Location, BlockData> e : h.replaced.entrySet()) {
                world.getBlockAt(e.getKey()).setBlockData(e.getValue(), false);
            }
            for (ConnectorInfo c : h.added) {
                DungeonBuilder.this.removeConnector(this, c);
            }
            // remove any stray holograms within connector locations
            for (RoomTemplate.Connector rc : h.instance.template.getConnectors()) {
                int[] vec = RoomTemplate.rotate(rc.x - (int) Math.round(h.instance.template.getCenterX()),
                        rc.z - (int) Math.round(h.instance.template.getCenterZ()), h.instance.rotation);
                Location loc = h.instance.center.clone().add(vec[0], rc.bottomY - h.instance.template.getConnectorMinY(), vec[1]);
                for (var ent : loc.getWorld().getNearbyEntities(loc, 1.5, 2.5, 1.5)) {
                    if (ent.getScoreboardTags().contains("dungeon_hologram")) {
                        ent.remove();
                        connectors.remove(ent.getEntityId());
                    }
                }
            }
            if (h.used != null) {
                ConnectorInfo restored = DungeonBuilder.this.spawnConnector(this, h.used.location, h.used.facing);
                connectors.put(restored.interaction.getEntityId(), restored);
            }
            dungeon.removeRoom(h.instance);
        }

        void cancel() {
            while (!history.isEmpty()) {
                undo();
            }
            for (ConnectorInfo c : new ArrayList<>(connectors.values())) {
                DungeonBuilder.this.removeConnector(this, c);
            }
            dungeon.delete();
        }

        DungeonLayout buildLayout() {
            DungeonLayout layout = new DungeonLayout();
            if (dungeon.getRooms().isEmpty()) return layout;
            Dungeon.RoomInstance first = dungeon.getRooms().get(0);
            int originX = first.center.getBlockX();
            int originZ = first.center.getBlockZ();
            // Grid coordinates still use the manager step, but store exact
            // offsets relative to the entrance so rooms can be recreated even
            // if some templates use slightly different spacing.
            int step = manager.getStep();
            int offX = DungeonLayout.WIDTH / 2;
            int offZ = DungeonLayout.HEIGHT / 2;
            for (int i = 0; i < dungeon.getRooms().size(); i++) {
                Dungeon.RoomInstance r = dungeon.getRooms().get(i);
                int diffX = r.center.getBlockX() - originX;
                int diffZ = r.center.getBlockZ() - originZ;
                int dx = Math.round((float) diffX / step);
                int dz = Math.round((float) diffZ / step);
                RoomTemplate t = r.template;
                RoomType type = i == 0 ? RoomType.ENTRANCE :
                        (t == manager.getBoss() ? RoomType.BOSS :
                                (t == manager.getCombatLeft() || t == manager.getCombatRight()
                                        ? RoomType.COMBAT :
                                        (t == manager.getExit() ? RoomType.EXIT :
                                                (t == manager.getTJunctionLeft() ? RoomType.TJUNCTION_LEFT :
                                                        (t == manager.getTJunctionRight() ? RoomType.TJUNCTION_RIGHT : RoomType.HALLWAY)))));
                int lx = offX + dx;
                int lz = offZ + dz;
                layout.set(lx, lz, type);
                layout.setRotation(lx, lz, r.rotation);
                if (type == RoomType.COMBAT) {
                    layout.setMob(lx, lz, r.mob);
                }
                layout.setOffset(lx, lz, diffX, diffZ);
            }
            layout.setStep(step);
            return layout;
        }
    }
}
