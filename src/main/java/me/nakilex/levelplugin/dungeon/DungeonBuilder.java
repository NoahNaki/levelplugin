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
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.HeadUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.mob.utils.MobNameUtil;
import me.nakilex.levelplugin.codex.CodexGuiUtil;
import me.nakilex.levelplugin.Main;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import me.nakilex.levelplugin.utils.InventorySerialUtil;
import java.io.File;
import java.io.IOException;

import java.util.*;
import java.awt.Point;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;

/**
 * Simplified in‑world dungeon builder using connector holograms. This is not a
 * full implementation but demonstrates the basic idea of placing rooms via
 * hologram interaction rather than the old GUI layout editor.
 */
public class DungeonBuilder implements Listener {
    private final DungeonManager manager;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, Location> storedReturns = new HashMap<>();
    private final Map<UUID, Location> pendingReturnOverrides = new HashMap<>();
    private final Map<UUID, StoredInventory> storedInventories = new HashMap<>();
    private final Map<UUID, Boolean> storedFlight = new HashMap<>();
    private final Map<UUID, Boolean> storedFlyingState = new HashMap<>();
    private final Map<UUID, EnumSet<TemplateType>> unlockedRooms = new HashMap<>();
    private final File sessionFile;
    private final FileConfiguration sessionConfig;

    // custom head textures for room icons
    private static final String CHEST_DECOR_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2UyZWI0NzUxZTNjNTBkNTBmZjE2MzUyNTc2NjYzZDhmZWRmZTNlMDRiMmYwYjhhMmFhODAzYjQxOTM2M2NhMSJ9fX0=";
    private static final String STONE_DECOR_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmQ0NDU0NDQ5NmVlMGFkMjc0MzE4ODQxZGZlMWViNjk0ZDA1NDA4MGQxMTJlMTMyYmVjOWU1ODM5YjJlNzYwMiJ9fX0=";
    private static final String LIBRARY_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDdhMzc0ZTIxYjgxYzBiMjFhYmViOGU5N2UxM2UwNzdkM2VkMWVkNDRmMmU5NTZjNjhmNjNhM2UxOWU4OTlmNiJ9fX0=";
    private static final String EXIT_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTkzYjhkYzkzZjAxODY2MGFhOTI1NmI5MWJiNzcwY2JjYmNjNjJhZTYxZTdhNjcxYzc1ZGM1NDQ1NjljMWE3OCJ9fX0=";
    private static final String TREASURE_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWM2ZTYwNGJkNTNkOTc4ODc1OTVhMDYyYjdjNTEyY2E0ZGJiZmU0OGJiNGFkY2VmNzEyNWQxZGIxMDNhYjdmZiJ9fX0=";
    private static final String BOSS_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzI2NzQzYjk5ODljNTlkNjI5NmVmZGE3NDhlNGVjNjc4YmNlNWQwN2FlODhmZmFjNzM3MmM0NTVjNmMyMDJhMiJ9fX0=";
    private static final String COMBAT_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzdkMjlkYmYzZDk4MjEzZWMyZmIwY2EyNWRhNzQ3NzllNTdiZDBjMTIzNDI2OGY4MjhhM2VjOTg2OWUxNWE5YyJ9fX0=";

    private static final EnumMap<TemplateType, Integer> ROOM_COSTS = new EnumMap<>(TemplateType.class);
    static {
        ROOM_COSTS.put(TemplateType.NONE, 0);
        ROOM_COSTS.put(TemplateType.ENTRANCE, 0);
        ROOM_COSTS.put(TemplateType.DEAD_END, 500);
        ROOM_COSTS.put(TemplateType.STRAIGHT, 500);
        ROOM_COSTS.put(TemplateType.CORNER_LEFT, 500);
        ROOM_COSTS.put(TemplateType.CORNER_RIGHT, 500);
        ROOM_COSTS.put(TemplateType.TJUNCTION, 650);
        ROOM_COSTS.put(TemplateType.TJUNCTION_LEFT, 650);
        ROOM_COSTS.put(TemplateType.TJUNCTION_RIGHT, 650);
        ROOM_COSTS.put(TemplateType.CROSSROAD, 800);
        ROOM_COSTS.put(TemplateType.HALLWAY, 450);
        ROOM_COSTS.put(TemplateType.TREASURE_LEFT, 1300);
        ROOM_COSTS.put(TemplateType.TREASURE_T_RIGHT, 1300);
        ROOM_COSTS.put(TemplateType.DECOR_STONE, 250);
        ROOM_COSTS.put(TemplateType.DECOR_CHEST, 250);
        ROOM_COSTS.put(TemplateType.COMBAT_LEFT, 1500);
        ROOM_COSTS.put(TemplateType.COMBAT_RIGHT, 1500);
        ROOM_COSTS.put(TemplateType.BOSS, 2500);
        ROOM_COSTS.put(TemplateType.LIBRARY, 1000);
        ROOM_COSTS.put(TemplateType.EXIT, 0);
    }

    public DungeonBuilder(DungeonManager manager) {
        this.manager = manager;
        File data = manager.getPlugin().getDataFolder();
        sessionFile = new File(data, "builder_sessions.yml");
        sessionConfig = YamlConfiguration.loadConfiguration(sessionFile);
        if (sessionConfig.isConfigurationSection("sessions")) {
            for (String key : sessionConfig.getConfigurationSection("sessions").getKeys(false)) {
                try {
                    UUID id = java.util.UUID.fromString(key);
                    String path = "sessions." + key + ".";
                    String world = sessionConfig.getString(path + "world");
                    if (world != null) {
                        World w = org.bukkit.Bukkit.getWorld(world);
                        if (w != null) {
                            double x = sessionConfig.getDouble(path + "x");
                            double y = sessionConfig.getDouble(path + "y");
                            double z = sessionConfig.getDouble(path + "z");
                            float yaw = (float) sessionConfig.getDouble(path + "yaw");
                            float pitch = (float) sessionConfig.getDouble(path + "pitch");
                            storedReturns.put(id, new Location(w, x, y, z, yaw, pitch));
                        }
                    }
                    String invData = sessionConfig.getString(path + "inventory");
                    String armorData = sessionConfig.getString(path + "armor");
                    String offhandData = sessionConfig.getString(path + "offhand");
                    ItemStack[] contents = invData != null ? InventorySerialUtil.itemStackArrayFromBase64(invData) : null;
                    ItemStack[] armor = armorData != null ? InventorySerialUtil.itemStackArrayFromBase64(armorData) : null;
                    ItemStack offhand = null;
                    if (offhandData != null) {
                        ItemStack[] arr = InventorySerialUtil.itemStackArrayFromBase64(offhandData);
                        if (arr.length > 0) offhand = arr[0];
                    }
                    if (contents != null || armor != null || offhand != null) {
                        storedInventories.put(id, new StoredInventory(
                                contents == null ? new ItemStack[0] : contents,
                                armor == null ? new ItemStack[0] : armor,
                                offhand
                        ));
                    }
                    List<String> unlocked = sessionConfig.getStringList(path + "unlocked");
                    if (!unlocked.isEmpty()) {
                        EnumSet<TemplateType> set = EnumSet.noneOf(TemplateType.class);
                        for (String value : unlocked) {
                            try {
                                set.add(TemplateType.valueOf(value));
                            } catch (IllegalArgumentException ignored) {}
                        }
                        if (!set.isEmpty()) {
                            unlockedRooms.put(id, set);
                        }
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    private void saveSessionData() {
        sessionConfig.set("sessions", null);
        java.util.Set<UUID> ids = new java.util.HashSet<>();
        ids.addAll(storedReturns.keySet());
        ids.addAll(storedInventories.keySet());
        ids.addAll(unlockedRooms.keySet());
        for (UUID id : ids) {
            String base = "sessions." + id;
            Location loc = storedReturns.get(id);
            if (loc != null && loc.getWorld() != null) {
                sessionConfig.set(base + ".world", loc.getWorld().getName());
                sessionConfig.set(base + ".x", loc.getX());
                sessionConfig.set(base + ".y", loc.getY());
                sessionConfig.set(base + ".z", loc.getZ());
                sessionConfig.set(base + ".yaw", loc.getYaw());
                sessionConfig.set(base + ".pitch", loc.getPitch());
            }
            StoredInventory inv = storedInventories.get(id);
            if (inv != null) {
                sessionConfig.set(base + ".inventory", InventorySerialUtil.itemStackArrayToBase64(inv.contents));
                sessionConfig.set(base + ".armor", InventorySerialUtil.itemStackArrayToBase64(inv.armor));
                if (inv.offhand != null) {
                    sessionConfig.set(base + ".offhand", InventorySerialUtil.itemStackArrayToBase64(new ItemStack[]{inv.offhand}));
                } else {
                    sessionConfig.set(base + ".offhand", null);
                }
            }
            EnumSet<TemplateType> unlocked = unlockedRooms.get(id);
            if (unlocked != null && !unlocked.isEmpty()) {
                List<String> values = new ArrayList<>(unlocked.size());
                for (TemplateType type : unlocked) {
                    values.add(type.name());
                }
                sessionConfig.set(base + ".unlocked", values);
            } else {
                sessionConfig.set(base + ".unlocked", null);
            }
        }
        try { sessionConfig.save(sessionFile); } catch (IOException ignored) {}
    }

    /** Cancel all active builder sessions, removing placed rooms and holograms. */
    public void cancelAll() {
        for (UUID id : new java.util.ArrayList<>(sessions.keySet())) {
            Session s = sessions.remove(id);
            if (s != null) {
                s.cancel();
                Player p = Bukkit.getPlayer(id);
                if (p != null) {
                    restoreStoredInventory(p);
                    ChatMessageUtil.send(p, MessageType.WARNING, "Dungeon build cancelled.");
                }
            }
        }
        saveSessionData();
    }

    /** Remove leftover edit worlds and return players to their saved locations. */
    public void cleanupOrphans() {
        for (World w : new java.util.ArrayList<>(Bukkit.getWorlds())) {
            if (w.getName().startsWith("dgn_edit_")) {
                for (Player p : new java.util.ArrayList<>(w.getPlayers())) {
                    Location back = storedReturns.remove(p.getUniqueId());
                    if (back == null) {
                        World main = Bukkit.getWorld("world");
                        back = main != null ? main.getSpawnLocation() : p.getLocation();
                    }
                    p.teleport(back);
                    restoreStoredInventory(p);
                    restoreFlightState(p);
                }
                manager.getPlugin().getWorldManager().deleteWorld(w.getName());
            }
        }
        saveSessionData();
    }

    public void start(Player player) {
        if (player == null) {
            return;
        }
        Location override = pendingReturnOverrides.remove(player.getUniqueId());
        start(player, override != null ? override : player.getLocation());
    }

    public void start(Player player, Location returnLocation) {
        if (player == null) {
            return;
        }
        Location override = pendingReturnOverrides.remove(player.getUniqueId());
        Location back;
        if (override != null) {
            back = override.clone();
        } else {
            back = returnLocation == null ? player.getLocation() : returnLocation.clone();
        }
        String worldName = "dgn_edit_" + player.getUniqueId();
        World world = manager.createVoidWorld(worldName);
        if (world == null) {
            ChatMessageUtil.send(player, MessageType.ERROR, "Failed to create edit world.");
            return;
        }
        Session s = new Session(player, world, back, true);
        sessions.put(player.getUniqueId(), s);
        storedReturns.put(player.getUniqueId(), back);
        storeInventory(player);
        storeFlightState(player);
        setupInventory(player);
        player.teleport(new Location(world, 0, 64, 0));
        player.setAllowFlight(true);
        player.setFlying(true);
        ChatMessageUtil.send(player, MessageType.INFO, "Right-click to place the entrance at your feet.");
    }

    public void setNextReturnLocation(Player player, Location location) {
        if (player == null) {
            return;
        }
        setNextReturnLocation(player.getUniqueId(), location);
    }

    public void setNextReturnLocation(UUID playerId, Location location) {
        if (playerId == null) {
            return;
        }
        if (location == null) {
            pendingReturnOverrides.remove(playerId);
        } else {
            pendingReturnOverrides.put(playerId, location.clone());
        }
    }

    public void edit(Player player, DungeonLayout layout) {
        Session s = new Session(player, player.getWorld(), player.getLocation(), false);
        sessions.put(player.getUniqueId(), s);
        storedReturns.put(player.getUniqueId(), player.getLocation());
        storeInventory(player);
        storeFlightState(player);
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
            RoomTemplate templ = manager.getTemplate(layout.getTemplate(entranceX, entranceZ));
            int rotation = layout.getRotation(entranceX, entranceZ);
            Location center = origin.clone();
            String mob = layout.getMob(entranceX, entranceZ);
            DungeonManager.PasteResult result = manager.pasteRoom(s.dungeon, templ, rotation, center, mob, true);
            List<ConnectorInfo> added = new ArrayList<>();
            for (RoomTemplate.Connector c : templ.getConnectors()) {
                Direction dir = rotate(c.facing, rotation);
                if (dirs.contains(dir)) continue;
                int[] vec = RoomTemplate.rotate(c.x - (int)Math.round(templ.getCenterX()),
                        c.z - (int)Math.round(templ.getCenterZ()), rotation);
                Location loc = center.clone().add(vec[0], c.bottomY - templ.getConnectorMinY(), vec[1]);
                ConnectorInfo info = spawnConnector(s, loc, dir);
                s.connectors.put(info.interaction.getEntityId(), info);
                added.add(info);
            }
            s.history.addLast(new History(null, added, result.instance(), result.replaced()));
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

                RoomTemplate templ = manager.getTemplate(layout.getTemplate(x, z));
                int rotation = layout.getRotation(x, z);
                int diffX = layout.getOffsetX(x, z);
                int diffZ = layout.getOffsetZ(x, z);
                Location center = origin.clone().add(diffX, 0, diffZ);
                String mob = layout.getMob(x, z);
                DungeonManager.PasteResult result = manager.pasteRoom(s.dungeon, templ, rotation, center, mob, true);

                // spawn connectors only for open sides
                List<ConnectorInfo> added = new ArrayList<>();
                for (RoomTemplate.Connector c : templ.getConnectors()) {
                    Direction dir = rotate(c.facing, rotation);
                    if (dirs.contains(dir)) continue; // neighbour already present
                    int[] vec = RoomTemplate.rotate(c.x - (int)Math.round(templ.getCenterX()),
                            c.z - (int)Math.round(templ.getCenterZ()), rotation);
                    Location loc = center.clone().add(vec[0], c.bottomY - templ.getConnectorMinY(), vec[1]);
                    ConnectorInfo info = spawnConnector(s, loc, dir);
                    s.connectors.put(info.interaction.getEntityId(), info);
                    added.add(info);
                }
                s.history.addLast(new History(null, added, result.instance(), result.replaced()));
            }
        }

        s.placingEntrance = (entranceX == -1);
        if (entranceX == -1) {
            ChatMessageUtil.send(player, MessageType.INFO, "Right-click to place the entrance at your feet.");
        }
    }

    public boolean isBuilding(Player player) {
        return player != null && isBuilding(player.getUniqueId());
    }

    public boolean isBuilding(java.util.UUID playerId) {
        return sessions.containsKey(playerId);
    }

    private boolean isBuilderMenuTitle(String rawTitle) {
        if (rawTitle == null) {
            return false;
        }
        String stripped = ChatColor.stripColor(rawTitle);
        return stripped.startsWith("Select") || stripped.endsWith("Variants");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        Session s = sessions.get(event.getPlayer().getUniqueId());
        if (s == null) return;
        if (s.pending != null && isBuilderMenuTitle(event.getView().getTitle())) return;
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> event.getPlayer().closeInventory());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClickGuard(InventoryClickEvent event) {
        Session s = sessions.get(event.getWhoClicked().getUniqueId());
        if (s == null) return;
        if (s.pending != null && isBuilderMenuTitle(event.getView().getTitle())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (isBuilding(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (isBuilding(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Session s = sessions.get(event.getPlayer().getUniqueId());
        if (s == null) return;
        event.setRespawnLocation(new Location(s.dungeon.getWorld(), 0, 0, 0));
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> s.resetPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVoidDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID) return;
        if (!(event.getEntity() instanceof Player player)) return;
        Session s = sessions.get(player.getUniqueId());
        if (s == null) return;
        event.setCancelled(true);
        s.resetPlayer();
    }

    private void setupInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setItemInOffHand(null);
        ItemStack wool = GuiUtil.getNexoItem("plus", ChatColor.GREEN + "Place Entrance");
        player.getInventory().setItem(0, wool);
        ItemStack undo = GuiUtil.getNexoItem("arrow_left", ChatColor.YELLOW + "Undo");
        player.getInventory().setItem(6, undo);

        ItemStack save = GuiUtil.getNexoItem("check", ChatColor.AQUA + "Save");
        player.getInventory().setItem(7, save);
        ItemStack cancel = GuiUtil.getNexoItem("cross", ChatColor.RED + "Cancel");
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
        ItemMeta meta = hand.getItemMeta();
        String name = meta != null ? ChatColor.stripColor(meta.getDisplayName()) : "";

        if (name.equalsIgnoreCase("Undo")) {
            event.setCancelled(true);
            s.undo();
            return;
        }
        if (name.equalsIgnoreCase("Save")) {
            event.setCancelled(true);
            s.awaitingName = true;
            ChatMessageUtil.send(player, MessageType.INFO, "Type dungeon name in chat or 'cancel'.");
            return;
        }
        if (name.equalsIgnoreCase("Cancel")) {
            event.setCancelled(true);
            s.cancel();
            sessions.remove(player.getUniqueId());
            ChatMessageUtil.send(player, MessageType.WARNING, "Dungeon build cancelled.");
            return;
        }
        if (!name.equalsIgnoreCase("Place Entrance")) return;
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
        if (entrance == null) {
            ChatMessageUtil.send(player, MessageType.ERROR, "Entrance template not loaded.");
            return;
        }
        if (entrance.getConnectors().isEmpty()) {
            ChatMessageUtil.send(player, MessageType.ERROR, "Entrance template missing connectors.");
            return;
        }
        RoomTemplate.Connector conn = entrance.getConnectors().get(0);
        int rot = 0;
        for (int r = 0; r < 4; r++) {
            if (rotate(conn.facing, r) == facing) { rot = r; break; }
        }
        DungeonManager.PasteResult result = manager.pasteRoom(s.dungeon, entrance, rot, loc, null, true);
        ChatMessageUtil.send(player, MessageType.INFO,
                ChatColor.GRAY + String.format("Overlap: %.1f%%", result.overlap() * 100));
        if (!result.success()) {
            ChatMessageUtil.send(player, MessageType.ERROR, "Cannot place entrance here.");
            return;
        }
        s.history.addLast(new History(null, spawnConnectors(s, loc, entrance, rot, null),
                result.instance(), result.replaced()));
        s.placingEntrance = false;
        ChatMessageUtil.send(player, MessageType.SUCCESS, "Entrance placed. Use holograms to add rooms.");
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
        player.openInventory(createRoomSelect(s));
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
        ItemMeta meta = item.getItemMeta();
        String name = ChatColor.stripColor(meta.getDisplayName());
        String id = meta.getLocalizedName();

        switch (rawTitle) {
            case "Select Room" -> {
                if (name.equalsIgnoreCase("Basic Room")) {
                    player.openInventory(createVariantSelect(s));
                } else if (name.equalsIgnoreCase("Hallway")) {
                    placeVariant(s, manager.getHallway());
                    player.closeInventory();
                } else if (name.equalsIgnoreCase("Treasure Room Left")) {
                    placeVariant(s, manager.getTreasureLeft());
                    player.closeInventory();
                } else if (name.equalsIgnoreCase("Treasure Room T-Section Right")) {
                    placeVariant(s, manager.getTreasureTRight());
                    player.closeInventory();
                } else if (name.equalsIgnoreCase("Decor Stone Room")) {
                    placeVariant(s, manager.getDecorStone());
                    player.closeInventory();
                } else if (name.equalsIgnoreCase("Decor Chest Room")) {
                    placeVariant(s, manager.getDecorChest());
                    player.closeInventory();
                } else if (name.equalsIgnoreCase("Combat Room")) {
                    player.openInventory(createCombatVariantSelect(s));
                } else if (name.equalsIgnoreCase("Boss Room")) {
                    s.selectedTemplate = manager.getBoss();
                    player.openInventory(createBossSelect(s));
                } else if (name.equalsIgnoreCase("Exit Room")) {
                    placeVariant(s, manager.getExit());
                    player.closeInventory();
                } else if (name.equalsIgnoreCase("Library")) {
                    placeVariant(s, manager.getLibrary());
                    player.closeInventory();
                }
            }
            case "Basic Room Variants" -> {
                if (name.equalsIgnoreCase("Back")) {
                    player.openInventory(createRoomSelect(s));
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
                    player.openInventory(createRoomSelect(s));
                    return;
                }
                RoomTemplate templ = switch (item.getType()) {
                    case GRAY_WOOL -> manager.getCombatLeft();
                    case LIGHT_GRAY_WOOL -> manager.getCombatRight();
                    default -> null;
                };
                if (templ != null) {
                    s.selectedTemplate = templ;
                    player.openInventory(createMobSelect(s, templ));
                }
            }
            case "Select Mob" -> {
                if (name.equalsIgnoreCase("Back")) {
                    player.openInventory(createCombatVariantSelect(s));
                    return;
                }
                if (id == null || id.isEmpty()) {
                    ChatMessageUtil.send(player, MessageType.ERROR,
                            "You must unlock this mob in the Codex before assigning it.");
                    return;
                }
                s.selectedMob = id;
                if (s.selectedTemplate != null) {
                    placeVariant(s, s.selectedTemplate);
                    player.closeInventory();
                    s.selectedTemplate = null;
                }
            }
            case "Select Boss" -> {
                if (name.equalsIgnoreCase("Back")) {
                    player.openInventory(createRoomSelect(s));
                    return;
                }
                if (id == null || id.isEmpty()) {
                    ChatMessageUtil.send(player, MessageType.ERROR,
                            "You must unlock this boss in the Codex before assigning it.");
                    return;
                }
                s.selectedMob = id;
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
            ChatMessageUtil.send(event.getPlayer(), MessageType.WARNING, "Save cancelled.");
            return;
        }
        Bukkit.getScheduler().runTask(manager.getPlugin(), () -> {
            DungeonLayout layout = s.buildLayout();
            if (!layout.hasEntrance() || !layout.hasExit() || !layout.hasBoss()) {
                ChatMessageUtil.send(event.getPlayer(), MessageType.ERROR,
                        "Dungeon requires an entrance, exit and boss room.");
                s.awaitingName = false;
                return;
            }
            event.getPlayer().sendTitle(ChatColor.YELLOW + "Saving...", "", 10, 70, 20);
            String display = msg;
            String key = DungeonManager.normalizeKey(display);
            if (manager.layoutExists(display)) {
                ChatMessageUtil.send(event.getPlayer(), MessageType.ERROR,
                        "A dungeon with that name already exists.");
                s.awaitingName = false;
                return;
            }
            manager.saveLayout(event.getPlayer(), key, display, layout);
            me.nakilex.levelplugin.Main.getInstance().getQuestManager()
                    .handleDungeonCreate(event.getPlayer(), key);
            s.cancel();
            ChatMessageUtil.send(event.getPlayer(), MessageType.SUCCESS,
                    "Dungeon saved as '" + ChatColor.YELLOW + key + ChatColor.GREEN + "'.");
            sessions.remove(event.getPlayer().getUniqueId());
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Session s = sessions.remove(event.getPlayer().getUniqueId());
        if (s != null) {
            s.cancel();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        UUID id = p.getUniqueId();
        if (storedReturns.containsKey(id) || storedInventories.containsKey(id) || p.getWorld().getName().startsWith("dgn_edit_")) {
            World original = p.getWorld();
            Location back = storedReturns.remove(id);
            if (back == null) {
                World main = Bukkit.getWorld("world");
                back = main != null ? main.getSpawnLocation() : p.getLocation();
            }
            p.teleport(back);
            restoreStoredInventory(p);
            restoreFlightState(p);
            saveSessionData();
            if (original.getName().startsWith("dgn_edit_")) {
                manager.getPlugin().getWorldManager().deleteWorld(original.getName());
            }
        }
    }

    private void placeVariant(Session s, RoomTemplate templ) {
        ConnectorInfo info = s.pending;
        s.pending = null;
        if (info == null) return;
        Location base = info.location;
        TemplateType type = manager.identifyTemplate(templ);
        int baseCost = getBaseRoomCost(templ);
        boolean unlocked = type != null && isTemplateUnlocked(s.player.getUniqueId(), type);
        int cost = baseCost > 0 && !unlocked ? baseCost : 0;
        me.nakilex.levelplugin.economy.managers.EconomyManager econ = Main.getInstance().getEconomyManager();
        if (cost > 0 && econ == null) {
            ChatMessageUtil.send(s.player, MessageType.ERROR,
                    "Economy service unavailable. Please try again later.");
            return;
        }
        if (cost > 0 && econ != null) {
            int balance = econ.getBalance(s.player);
            if (balance < cost) {
                ChatMessageUtil.send(s.player, MessageType.ERROR,
                        "You need " + ChatColor.GOLD + "<glyph:coins_icon> " + cost
                                + ChatColor.RED + " to place this room.");
                return;
            }
        }
        int rotation = 0;
        RoomTemplate.Connector match = null;

        // Ensure corner variants maintain the correct turn direction. When
        // placing a corner we want the second exit to appear either to the
        // left or right of the entrance depending on which template was
        // selected.
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
        if (match == null && !templ.getConnectors().isEmpty()) {
            match = templ.getConnectors().get(0);
            rotation = (info.facing.opposite().ordinal() - match.facing.ordinal()) & 3;
        }
        if (match == null) return;
        int[] vec = RoomTemplate.rotate(match.x - (int) Math.round(templ.getCenterX()),
                match.z - (int) Math.round(templ.getCenterZ()), rotation);
        Location center = base.clone().subtract(vec[0], match.bottomY - templ.getConnectorMinY(), vec[1]);
        String mob = (templ == manager.getCombatLeft() || templ == manager.getCombatRight() || templ == manager.getBoss()) ? s.selectedMob : null;
        DungeonManager.PasteResult result = manager.pasteRoom(s.dungeon, templ, rotation, center, mob, true);
        ChatMessageUtil.send(s.player, MessageType.INFO,
                ChatColor.GRAY + String.format("Overlap: %.1f%%", result.overlap() * 100));
        if (!result.success()) {
            ChatMessageUtil.send(s.player, MessageType.ERROR, "Room collides with existing blocks.");
            return;
        }
        if (cost > 0 && econ != null) {
            try {
                econ.deductCoins(s.player, cost);
                if (type != null) {
                    unlockTemplate(s.player.getUniqueId(), type);
                }
                ChatMessageUtil.send(s.player, MessageType.SUCCESS,
                        "Unlocked " + ChatColor.YELLOW + describeTemplate(templ)
                                + ChatColor.GREEN + " for " + ChatColor.GOLD + "<glyph:coins_icon> " + cost
                                + ChatColor.GREEN + ". Future placements are free.");
            } catch (IllegalArgumentException ex) {
                ChatMessageUtil.send(s.player, MessageType.ERROR, "Failed to deduct coins. Placement cancelled.");
                // restore replaced blocks
                for (Map.Entry<Location, BlockData> entry : result.replaced().entrySet()) {
                    Location loc = entry.getKey();
                    BlockData data = entry.getValue();
                    if (loc.getWorld() != null) {
                        loc.getBlock().setBlockData(data, false);
                    }
                }
                // remove the room instance and any exit holograms created
                for (RoomTemplate.Marker m : templ.getExitMarkers()) {
                    int[] exitOffset = RoomTemplate.rotate(m.x - (int) Math.round(templ.getCenterX()),
                            m.z - (int) Math.round(templ.getCenterZ()), rotation);
                    Location loc = center.clone().add(exitOffset[0], m.y - templ.getConnectorMinY(), exitOffset[1]);
                    for (var ent : loc.getWorld().getNearbyEntities(loc, 1.5, 2.5, 1.5)) {
                        if (ent.getScoreboardTags().contains("dungeon_exit")) {
                            ent.remove();
                        }
                    }
                }
                s.dungeon.removeRoom(result.instance());
                return;
            }
        } else if (baseCost <= 0 && type == TemplateType.EXIT) {
            ChatMessageUtil.send(s.player, MessageType.SUCCESS, "Placed the exit room for free.");
        } else if (baseCost > 0 && unlocked) {
            ChatMessageUtil.send(s.player, MessageType.SUCCESS,
                    "Placed your unlocked " + ChatColor.YELLOW + describeTemplate(templ)
                            + ChatColor.GREEN + ".");
        }
        removeConnector(s, info);
        List<ConnectorInfo> added = spawnConnectors(s, center, templ, rotation, info);
        s.history.addLast(new History(info, added, result.instance(), result.replaced()));
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

    private Inventory createRoomSelect(Session session) {
        Inventory inv = GuiBuilder.create(27, ChatColor.DARK_GREEN + "Select Room")
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();

        ItemStack basic = pricedItem(session, manager.getDeadEnd(), Material.YELLOW_WOOL, ChatColor.YELLOW + "Basic Room",
                TooltipUtil.clickInstructions("to view variants", null));
        ItemStack hall = pricedItem(session, manager.getHallway(), Material.BROWN_WOOL, ChatColor.YELLOW + "Hallway");
        ItemStack treasureLeft = pricedHead(session, manager.getTreasureLeft(),
                HeadUtil.createCustomHead(TREASURE_HEAD, ChatColor.GOLD + "Treasure Room Left", null));
        ItemStack treasureTRight = pricedHead(session, manager.getTreasureTRight(),
                HeadUtil.createCustomHead(TREASURE_HEAD, ChatColor.GOLD + "Treasure Room T-Section Right", null));
        ItemStack decorStone = pricedHead(session, manager.getDecorStone(),
                HeadUtil.createCustomHead(STONE_DECOR_HEAD, ChatColor.GRAY + "Decor Stone Room", null));
        ItemStack decorChest = pricedHead(session, manager.getDecorChest(),
                HeadUtil.createCustomHead(CHEST_DECOR_HEAD, ChatColor.YELLOW + "Decor Chest Room", null));
        ItemStack boss = pricedHead(session, manager.getBoss(),
                HeadUtil.createCustomHead(BOSS_HEAD, ChatColor.DARK_GRAY + "Boss Room", null));
        ItemStack combat = HeadUtil.createCustomHead(COMBAT_HEAD, ChatColor.RED + "Combat Room", null);
        ItemStack exitRoom = pricedHead(session, manager.getExit(),
                HeadUtil.createCustomHead(EXIT_HEAD, ChatColor.DARK_PURPLE + "Exit Room", null));
        ItemStack library = pricedHead(session, manager.getLibrary(),
                HeadUtil.createCustomHead(LIBRARY_HEAD, ChatColor.GOLD + "Library", null));
        combat = addCostLore(session, combat, manager.getCombatLeft(),
                TooltipUtil.clickInstructions("to place", "to edit"));

        inv.setItem(10, basic);
        inv.setItem(11, hall);
        inv.setItem(12, treasureLeft);
        inv.setItem(13, treasureTRight);
        inv.setItem(14, decorStone);
        inv.setItem(15, decorChest);
        inv.setItem(16, boss);
        inv.setItem(17, combat);
        inv.setItem(18, exitRoom);
        inv.setItem(26, library);
        return inv;
    }

    private Inventory createVariantSelect(Session session) {
        Inventory inv = GuiBuilder.create(27, ChatColor.DARK_GREEN + "Basic Room Variants")
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();

        inv.setItem(10, pricedItem(session, manager.getDeadEnd(), Material.RED_WOOL, ChatColor.RED + "Basic Room Dead End"));
        inv.setItem(12, pricedItem(session, manager.getStraight(), Material.ORANGE_WOOL, ChatColor.GOLD + "Basic Room Straight"));
        inv.setItem(14, pricedItem(session, manager.getCornerLeft(), Material.GREEN_WOOL, ChatColor.GREEN + "Basic Room Corner Left"));
        inv.setItem(16, pricedItem(session, manager.getCornerRight(), Material.LIME_WOOL, ChatColor.GREEN + "Basic Room Corner Right"));
        inv.setItem(20, pricedItem(session, manager.getTJunctionLeft(), Material.BLUE_WOOL, ChatColor.BLUE + "Basic Room T-Section Left"));
        inv.setItem(22, pricedItem(session, manager.getTJunctionRight(), Material.CYAN_WOOL, ChatColor.BLUE + "Basic Room T-Section Right"));
        inv.setItem(24, pricedItem(session, manager.getCrossroad(), Material.PURPLE_WOOL, ChatColor.LIGHT_PURPLE + "Basic Room Crossroad"));

        inv.setItem(18, GuiUtil.getNexoItem("arrow_left", ChatColor.YELLOW + "Back"));
        return inv;
    }

    private Inventory createCombatVariantSelect(Session session) {
        Inventory inv = GuiBuilder.create(27, ChatColor.DARK_GREEN + "Combat Variants")
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();

        inv.setItem(11, pricedItem(session, manager.getCombatLeft(), Material.GRAY_WOOL, ChatColor.GRAY + "Combat Left"));
        inv.setItem(15, pricedItem(session, manager.getCombatRight(), Material.LIGHT_GRAY_WOOL, ChatColor.GRAY + "Combat Right"));

        inv.setItem(18, GuiUtil.getNexoItem("arrow_left", ChatColor.YELLOW + "Back"));
        return inv;
    }

    private Inventory createMobSelect(Session session, Set<String> mobs, String title, RoomTemplate template) {
        java.util.UUID playerId = session.player.getUniqueId();
        List<String> sorted = new ArrayList<>();
        if (mobs != null) {
            sorted.addAll(mobs);
        }
        sorted.sort(Comparator.comparing(key -> {
            String name = MobNameUtil.getDisplayName(key);
            return ChatColor.stripColor(name == null ? key : name);
        }, String.CASE_INSENSITIVE_ORDER));

        int rows = Math.max(1, (int) Math.ceil(sorted.size() / 9.0));
        int size = rows * 9;
        Inventory inv = GuiBuilder.create(size, ChatColor.DARK_GREEN + title)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();

        if (sorted.isEmpty()) {
            ItemStack barrier = new ItemStack(Material.BARRIER);
            ItemMeta meta = barrier.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.RED + "No discovered mobs");
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Unlock entries in your Codex to use them here.");
                lore.addAll(TooltipUtil.bulletList(
                        "Defeat the mob at least once to record it.",
                        "Return after unlocking new entries."
                ));
                meta.setLore(lore);
                meta.setLocalizedName("");
            }
            barrier.setItemMeta(meta);
            inv.setItem(size / 2, barrier);
        } else {
            var codex = manager.getPlugin().getCodexManager();
            int idx = 0;
            for (String key : sorted) {
                ItemStack is = new ItemStack(Material.SKELETON_SKULL);
                ItemMeta im = is.getItemMeta();
                if (im != null) {
                    String display = MobNameUtil.getDisplayName(key);
                    im.setDisplayName(ChatColor.WHITE + (display == null ? key : display));
                    im.setLocalizedName(key);
                    List<String> lore = new ArrayList<>();
                    String costLine = formatCostLine(session, template);
                    if (costLine != null && !costLine.isEmpty()) {
                        lore.add(costLine);
                    }
                    List<String> progress = CodexGuiUtil.mobProgressLore(codex, playerId, key);
                    if (!progress.isEmpty()) {
                        lore.addAll(progress);
                    }
                    if (!lore.isEmpty()) {
                        lore.add("");
                    }
                    lore.addAll(TooltipUtil.clickInstructions("to assign", null));
                    im.setLore(lore);
                }
                is.setItemMeta(im);
                inv.setItem(idx++, is);
            }
        }

        inv.setItem(size - 1, GuiUtil.getNexoItem("arrow_left", ChatColor.YELLOW + "Back"));
        return inv;
    }

    private Inventory createBossSelect(Session session) {
        return createMobSelect(session, manager.getAvailableBosses(session.player.getUniqueId()), "Select Boss", manager.getBoss());
    }

    private Inventory createMobSelect(Session session, RoomTemplate template) {
        return createMobSelect(session, manager.getAvailableMobs(session.player.getUniqueId()), "Select Mob", template);
    }

    private ItemStack pricedItem(Session session, RoomTemplate template, Material mat, String name) {
        return pricedItem(session, template, mat, name, null);
    }

    private ItemStack pricedItem(Session session, RoomTemplate template, Material mat, String name, List<String> instructions) {
        ItemStack is = new ItemStack(mat);
        ItemMeta im = is.getItemMeta();
        if (im != null) im.setDisplayName(name);
        is.setItemMeta(im);
        return addCostLore(session, is, template, instructions);
    }

    private ItemStack pricedHead(Session session, RoomTemplate template, ItemStack head) {
        return addCostLore(session, head, template, null);
    }

    private ItemStack addCostLore(Session session, ItemStack item, RoomTemplate template, List<String> instructions) {
        if (item == null || template == null) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        TemplateType type = manager.identifyTemplate(template);
        boolean unlocked = session != null && type != null && isTemplateUnlocked(session.player.getUniqueId(), type);
        int baseCost = getBaseRoomCost(template);
        List<String> lore = new ArrayList<>();
        lore.add(formatCostLine(session, template));
        List<String> finalInstructions = instructions;
        if ((finalInstructions == null || finalInstructions.isEmpty()) && baseCost > 0) {
            String left = unlocked ? "to place" : "to unlock & place";
            finalInstructions = TooltipUtil.clickInstructions(left, null);
        } else if ((finalInstructions == null || finalInstructions.isEmpty()) && baseCost <= 0) {
            finalInstructions = TooltipUtil.clickInstructions("to place", null);
        }
        if (finalInstructions != null && !finalInstructions.isEmpty()) {
            lore.addAll(finalInstructions);
        }
        List<String> existing = meta.getLore();
        if (existing != null && !existing.isEmpty()) {
            if (!lore.isEmpty()) {
                lore.add("");
            }
            lore.addAll(existing);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String formatCostLine(Session session, RoomTemplate template) {
        int baseCost = getBaseRoomCost(template);
        if (baseCost <= 0) {
            return ChatColor.GRAY + "Cost: " + ChatColor.GREEN + "Free";
        }
        TemplateType type = manager.identifyTemplate(template);
        if (session != null && type != null && isTemplateUnlocked(session.player.getUniqueId(), type)) {
            return ChatColor.GRAY + "Cost: " + ChatColor.GREEN + "Unlocked";
        }
        return ChatColor.GRAY + "Unlock Cost: " + ChatColor.GOLD + "<glyph:coins_icon> " + baseCost;
    }

    private int getBaseRoomCost(RoomTemplate template) {
        if (template == null) return 0;
        TemplateType type = manager.identifyTemplate(template);
        return ROOM_COSTS.getOrDefault(type, 0);
    }

    private boolean isTemplateUnlocked(UUID playerId, TemplateType type) {
        if (type == null) return false;
        EnumSet<TemplateType> unlocked = unlockedRooms.get(playerId);
        return unlocked != null && unlocked.contains(type);
    }

    private void unlockTemplate(UUID playerId, TemplateType type) {
        if (type == null || ROOM_COSTS.getOrDefault(type, 0) <= 0) return;
        EnumSet<TemplateType> unlocked = unlockedRooms.computeIfAbsent(playerId, id -> EnumSet.noneOf(TemplateType.class));
        if (unlocked.add(type)) {
            saveSessionData();
        }
    }

    private String describeTemplate(RoomTemplate template) {
        TemplateType type = manager.identifyTemplate(template);
        return switch (type) {
            case NONE -> "Room";
            case ENTRANCE -> "Entrance";
            case DEAD_END -> "Basic Room";
            case STRAIGHT -> "Basic Room";
            case CORNER_LEFT -> "Basic Room Corner";
            case CORNER_RIGHT -> "Basic Room Corner";
            case TJUNCTION, TJUNCTION_LEFT, TJUNCTION_RIGHT -> "Basic Room T-Junction";
            case CROSSROAD -> "Basic Room Crossroad";
            case HALLWAY -> "Hallway";
            case TREASURE_LEFT, TREASURE_T_RIGHT -> "Treasure Room";
            case DECOR_STONE, DECOR_CHEST -> "Decor Room";
            case COMBAT_LEFT, COMBAT_RIGHT -> "Combat Room";
            case BOSS -> "Boss Room";
            case LIBRARY -> "Library";
            case EXIT -> "Exit Room";
            default -> {
                String raw = type.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
                yield raw.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + raw.substring(1);
            }
        };
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
        final Location returnLocation;
        final boolean tempWorld;
        final Deque<History> history = new ArrayDeque<>();
        final Map<Integer, ConnectorInfo> connectors = new HashMap<>();
        boolean placingEntrance = true;
        ConnectorInfo pending;
        boolean awaitingName = false;
        String selectedMob = null;
        RoomTemplate selectedTemplate = null;
        Session(Player player, World world, Location back, boolean tempWorld) {
            this.player = player;
            this.dungeon = new Dungeon(world, player.getName() + "_builder");
            this.returnLocation = back;
            this.tempWorld = tempWorld;
        }

        void resetPlayer() {
            Location origin = new Location(dungeon.getWorld(), 0, 0, 0);
            player.teleport(origin);
            player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
            player.setAllowFlight(true);
            player.setFlying(true);
        }
        void undo() {
            History h = history.pollLast();
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
            // remove exit holograms placed by this room
            for (RoomTemplate.Marker m : h.instance.template.getExitMarkers()) {
                int[] vec = RoomTemplate.rotate(m.x - (int) Math.round(h.instance.template.getCenterX()),
                        m.z - (int) Math.round(h.instance.template.getCenterZ()), h.instance.rotation);
                Location loc = h.instance.center.clone().add(vec[0], m.y - h.instance.template.getConnectorMinY(), vec[1]);
                for (var ent : loc.getWorld().getNearbyEntities(loc, 1.5, 2.5, 1.5)) {
                    if (ent.getScoreboardTags().contains("dungeon_exit")) {
                        ent.remove();
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
            if (player.isOnline() && returnLocation != null) {
                player.teleport(returnLocation);
            }
            storedReturns.remove(player.getUniqueId());
            if (player.isOnline()) {
                restoreStoredInventory(player);
                restoreFlightState(player);
            }
            saveSessionData();
            if (tempWorld) {
                org.bukkit.Bukkit.getScheduler().runTaskLater(manager.getPlugin(),
                        () -> manager.getPlugin().getWorldManager().deleteWorld(dungeon.getWorld().getName()), 1L);
            }
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
            java.util.Set<java.awt.Point> used = new java.util.HashSet<>();
            for (int i = 0; i < dungeon.getRooms().size(); i++) {
                Dungeon.RoomInstance r = dungeon.getRooms().get(i);
                int diffX = r.center.getBlockX() - originX;
                int diffZ = r.center.getBlockZ() - originZ;
                int dx = Math.round((float) diffX / step);
                int dz = Math.round((float) diffZ / step);
                java.awt.Point key = new java.awt.Point(dx, dz);
                while (used.contains(key)) {
                    dx++;
                    key = new java.awt.Point(dx, dz);
                }
                used.add(key);
                RoomTemplate t = r.template;
                RoomType type = i == 0 ? RoomType.ENTRANCE :
                        (t == manager.getBoss() ? RoomType.BOSS :
                                (t == manager.getCombatLeft() || t == manager.getCombatRight()
                                        ? RoomType.COMBAT :
                                        (t == manager.getLibrary() ? RoomType.LIBRARY :
                                                (t == manager.getExit() ? RoomType.EXIT :
                                                        (t == manager.getTJunctionLeft() ? RoomType.TJUNCTION_LEFT :
                                                                (t == manager.getTJunctionRight() ? RoomType.TJUNCTION_RIGHT : RoomType.HALLWAY))))));
                int lx = offX + dx;
                int lz = offZ + dz;
                layout.set(lx, lz, type);
                layout.setTemplate(lx, lz, manager.identifyTemplate(t));
                layout.setRotation(lx, lz, r.rotation);
                if (type == RoomType.COMBAT || type == RoomType.BOSS) {
                    layout.setMob(lx, lz, r.mob);
                    int power = me.nakilex.levelplugin.mob.utils.CombatPowerUtil.estimateCombatPower(r.mob);
                    int threat = me.nakilex.levelplugin.mob.utils.ThreatUtil.levelForPower(power);
                    layout.setThreat(lx, lz, threat);
                }
                layout.setOffset(lx, lz, diffX, diffZ);
            }
            layout.setStep(step);
            return layout;
        }
    }

    private void storeInventory(Player player) {
        UUID id = player.getUniqueId();
        StoredInventory stored = new StoredInventory(
                cloneItems(player.getInventory().getContents()),
                cloneItems(player.getInventory().getArmorContents()),
                cloneItem(player.getInventory().getItemInOffHand())
        );
        storedInventories.put(id, stored);
        saveSessionData();
    }

    private void storeFlightState(Player player) {
        UUID id = player.getUniqueId();
        storedFlight.put(id, player.getAllowFlight());
        storedFlyingState.put(id, player.isFlying());
    }

    private void restoreStoredInventory(Player player) {
        StoredInventory stored = storedInventories.remove(player.getUniqueId());
        if (stored == null) return;
        player.getInventory().setContents(cloneItems(stored.contents));
        player.getInventory().setArmorContents(cloneItems(stored.armor));
        player.getInventory().setItemInOffHand(cloneItem(stored.offhand));
        saveSessionData();
    }

    private void restoreFlightState(Player player) {
        UUID id = player.getUniqueId();
        boolean allow = storedFlight.getOrDefault(id, false);
        boolean flying = storedFlyingState.getOrDefault(id, false) && allow;
        player.setAllowFlight(allow);
        player.setFlying(flying);
        storedFlight.remove(id);
        storedFlyingState.remove(id);
    }

    private ItemStack[] cloneItems(ItemStack[] items) {
        if (items == null) return new ItemStack[0];
        ItemStack[] copy = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            ItemStack item = items[i];
            copy[i] = item == null ? null : item.clone();
        }
        return copy;
    }

    private ItemStack cloneItem(ItemStack item) {
        return item == null ? null : item.clone();
    }

    private static class StoredInventory {
        final ItemStack[] contents;
        final ItemStack[] armor;
        final ItemStack offhand;

        StoredInventory(ItemStack[] contents, ItemStack[] armor, ItemStack offhand) {
            this.contents = contents == null ? new ItemStack[0] : contents;
            this.armor = armor == null ? new ItemStack[0] : armor;
            this.offhand = offhand;
        }
    }
}
