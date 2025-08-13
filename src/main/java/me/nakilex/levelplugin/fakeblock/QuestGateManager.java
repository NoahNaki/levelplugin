package me.nakilex.levelplugin.fakeblock;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.fakeblock.GateAnimation;
import me.nakilex.levelplugin.utils.SchematicUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.Location;

import java.io.File;
import java.util.*;

/**
 * Handles quest gated fake blocks. When a player hasn't completed the
 * associated quest the gate appears closed and movement through the area is
 * prevented.
 */
public class QuestGateManager implements Listener {

    private final Main plugin;
    private final FakeBlockManager blockManager;
    private final Map<String, QuestGate> gates = new HashMap<>();
    /** Folder storing schematics for custom gate states. */
    private final File schemFolder;
    /** Enables verbose logging for gate state changes. */
    private boolean debug = false;
    private File file;
    private FileConfiguration config;

    public QuestGateManager(Main plugin, FakeBlockManager blockManager) {
        this.plugin = plugin;
        this.blockManager = blockManager;
        this.schemFolder = new File(plugin.getDataFolder(), "gate_schematics");
        if (!schemFolder.exists()) schemFolder.mkdirs();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadFromConfig();
    }

    public File getSchematicFolder() { return schemFolder; }

    public void addGate(QuestGate gate) {
        gates.put(gate.getId().toLowerCase(), gate);
    }

    /** Returns the set of registered gate ids. */
    public Set<String> getGateIds() {
        return new HashSet<>(gates.keySet());
    }

    /** Remove a gate and persist changes. */
    public boolean removeGate(String id) {
        if (gates.remove(id.toLowerCase()) != null) {
            saveConfig();
            updateAll();
            return true;
        }
        return false;
    }

    /** Create and register a new gate and persist it to disk. */
    public void createGate(QuestGate gate) {
        addGate(gate);
        logDebug("createGate " + gate.getId() + " blocks=" + gate.getBlocks().size());
        saveConfig();
        updateAll();
    }

    /** Toggle debug logging. */
    public boolean toggleDebug() {
        debug = !debug;
        plugin.getLogger().info("[GateDebug] mode " + (debug ? "enabled" : "disabled"));
        return debug;
    }

    public boolean isDebug() {
        return debug;
    }

    private void logDebug(String msg) {
        if (debug) {
            plugin.getLogger().info("[GateDebug] " + msg);
        }
    }

    private void loadFromConfig() {
        file = new File(plugin.getDataFolder(), "gates.yml");
        if (!file.exists()) {
            plugin.saveResource("gates.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        if (!config.isConfigurationSection("gates")) return;
        for (String key : config.getConfigurationSection("gates").getKeys(false)) {
            String base = "gates." + key + ".";
            String worldName = config.getString(base + "world");
            World world = plugin.getServer().getWorld(worldName);
            if (world == null) continue;

            Location p1 = readLocation(world, config, base + "pos1");
            Location p2 = readLocation(world, config, base + "pos2");
            if (p1 == null || p2 == null) continue;
            String closedSchem = config.getString(base + "closedSchematic");
            String openSchem = config.getString(base + "openSchematic");
            boolean closed = config.getBoolean(base + "closed", true);
            GateAnimation anim = GateAnimation.fromString(config.getString(base + "animation"));
            long ticks = config.getLong(base + "duration", 40L);
            Map<Location, BlockData> closedMap = null;
            Map<Location, BlockData> openMap = null;
            if (closedSchem != null) {
                File schem = new File(schemFolder, closedSchem);
                var rel = SchematicUtil.loadSchematic(schem, plugin.getLogger());
                closedMap = SchematicUtil.toLocationMap(rel, world,
                        Math.min(p1.getBlockX(), p2.getBlockX()),
                        Math.min(p1.getBlockY(), p2.getBlockY()),
                        Math.min(p1.getBlockZ(), p2.getBlockZ()));
            }
            if (openSchem != null) {
                File schem = new File(schemFolder, openSchem);
                var rel = SchematicUtil.loadSchematic(schem, plugin.getLogger());
                openMap = SchematicUtil.toLocationMap(rel, world,
                        Math.min(p1.getBlockX(), p2.getBlockX()),
                        Math.min(p1.getBlockY(), p2.getBlockY()),
                        Math.min(p1.getBlockZ(), p2.getBlockZ()));
            }
            Material mat = Material.matchMaterial(config.getString(base + "block", "BARRIER"));
            if (mat == null) mat = Material.BARRIER;
            BlockData data = mat.createBlockData();
            QuestGate gate = QuestGate.create(key.toLowerCase(), p1, p2, data, closedMap, openMap, closed, anim, ticks);
            addGate(gate);
            applyDefaultState(gate);
        }
    }

    /** Replace every block inside the gate region with the given material. */
    private void fillRegion(QuestGate gate, Material material) {
        for (Location loc : gate.getBlocks()) {
            loc.getBlock().setType(material, false);
        }
    }

    /** Apply the gate's default state to the actual world blocks. */
    private void applyDefaultState(QuestGate gate) {
        if (gate.isDefaultClosed()) {
            clearGateBlocks(gate);
        } else if (gate.hasOpenCustomBlocks()) {
            // Place the open-state blocks so the region isn't empty on restart
            for (var entry : gate.getOpenDataMap().entrySet()) {
                entry.getKey().getBlock().setBlockData(entry.getValue(), false);
            }
        }
    }

    /** Ensure the real world stays passable by removing solid gate blocks. */
    private void clearGateBlocks(QuestGate gate) {
        fillRegion(gate, Material.AIR);
        logDebug("cleared region for " + gate.getId());
    }

    private void saveConfig() {
        config.set("gates", null);
        for (QuestGate gate : gates.values()) {
            String base = "gates." + gate.getId() + ".";
            Location p1 = gate.getPos1();
            Location p2 = gate.getPos2();
            config.set(base + "world", p1.getWorld().getName());
            config.set(base + "pos1.x", p1.getBlockX());
            config.set(base + "pos1.y", p1.getBlockY());
            config.set(base + "pos1.z", p1.getBlockZ());
            config.set(base + "pos2.x", p2.getBlockX());
            config.set(base + "pos2.y", p2.getBlockY());
            config.set(base + "pos2.z", p2.getBlockZ());
            if (gate.hasCustomBlocks()) {
                config.set(base + "closedSchematic", gate.getId() + "_closed.schem");
                config.set(base + "block", null);
            } else {
                config.set(base + "closedSchematic", null);
                config.set(base + "block", gate.getClosedData().getMaterial().name());
            }
            if (gate.hasOpenCustomBlocks()) {
                config.set(base + "openSchematic", gate.getId() + "_open.schem");
            } else {
                config.set(base + "openSchematic", null);
            }
            config.set(base + "closed", gate.isDefaultClosed());
            config.set(base + "animation", gate.getAnimation().name());
            config.set(base + "duration", gate.getAnimationTicks());
        }
        try { config.save(file); } catch (Exception e) { e.printStackTrace(); }
    }

    private Location readLocation(World world, FileConfiguration cfg, String path) {
        if (!cfg.isConfigurationSection(path)) return null;
        int x = cfg.getInt(path + ".x");
        int y = cfg.getInt(path + ".y");
        int z = cfg.getInt(path + ".z");
        return new Location(world, x, y, z);
    }

    /** Toggle the closed state of a gate for a specific player. */
    public boolean toggleGate(Player player, String id) {
        QuestGate gate = gates.get(id.toLowerCase());
        if (gate == null) return false;
        boolean closed = gate.isClosed(player.getUniqueId());
        gate.setClosed(player.getUniqueId(), !closed);
        animateGate(player, gate, !closed);
        logDebug(player.getName() + " toggled " + id + " to " + (!closed ? "open" : "closed"));
        return true;
    }

    /** Explicitly open a gate for a player. */
    public boolean openGate(Player player, String id) {
        return setGateState(player, id, false);
    }

    /** Explicitly close a gate for a player. */
    public boolean closeGate(Player player, String id) {
        return setGateState(player, id, true);
    }

    private boolean setGateState(Player player, String id, boolean closed) {
        QuestGate gate = gates.get(id.toLowerCase());
        if (gate == null) return false;
        gate.setClosed(player.getUniqueId(), closed);
        animateGate(player, gate, closed);
        logDebug(player.getName() + " set " + id + " to " + (closed ? "closed" : "open"));
        return true;
    }

    /** Access a gate by id or null if not found. */
    public QuestGate getGate(String id) {
        if (id == null) return null;
        return gates.get(id.toLowerCase());
    }

    private void animateGate(Player player, QuestGate gate, boolean closed) {
        java.util.List<java.util.List<org.bukkit.Location>> groups = new java.util.ArrayList<>();

        switch (gate.getAnimation()) {
            case GATE, WATERFALL -> {
                java.util.Map<Integer, java.util.List<org.bukkit.Location>> map = new java.util.HashMap<>();
                for (var loc : gate.getBlocks()) {
                    map.computeIfAbsent(loc.getBlockY(), k -> new java.util.ArrayList<>()).add(loc);
                }
                java.util.List<Integer> ys = new java.util.ArrayList<>(map.keySet());
                ys.sort(Integer::compare);
                if (gate.getAnimation() == GateAnimation.WATERFALL) java.util.Collections.reverse(ys);
                for (int y : ys) groups.add(map.get(y));
            }
            case ELEVATOR -> {
                java.util.Map<String, java.util.List<org.bukkit.Location>> colMap = new java.util.HashMap<>();
                for (var loc : gate.getBlocks()) {
                    String key = loc.getBlockX()+","+loc.getBlockZ();
                    colMap.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(loc);
                }
                double cx = (gate.getMinX() + gate.getMaxX()) / 2.0;
                double cz = (gate.getMinZ() + gate.getMaxZ()) / 2.0;
                java.util.Map<Double, java.util.List<java.util.List<org.bukkit.Location>>> distMap = new java.util.TreeMap<>();
                for (var entry : colMap.entrySet()) {
                    var p = entry.getValue().get(0);
                    double dx = (p.getBlockX() + 0.5) - cx;
                    double dz = (p.getBlockZ() + 0.5) - cz;
                    double dist = Math.sqrt(dx*dx + dz*dz);
                    dist = Math.round(dist * 1000.0) / 1000.0;
                    distMap.computeIfAbsent(dist, d -> new java.util.ArrayList<>()).add(entry.getValue());
                }
                for (var list : distMap.values()) {
                    java.util.List<org.bukkit.Location> group = new java.util.ArrayList<>();
                    for (var col : list) group.addAll(col);
                    groups.add(group);
                }
            }
            default -> groups.add(gate.getBlocks());
        }

        if (closed && gate.getAnimation() != GateAnimation.INSTANT) java.util.Collections.reverse(groups);

        java.util.Iterator<java.util.List<org.bukkit.Location>> it = groups.iterator();

        org.bukkit.scheduler.BukkitRunnable task = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!it.hasNext()) { cancel(); return; }
                var locs = it.next();
                if (closed) {
                    for (var loc : locs) blockManager.showFakeBlock(player, loc, gate.getClosedData(loc));
                } else {
                    if (gate.hasOpenCustomBlocks()) {
                        for (var loc : locs) blockManager.showFakeBlock(player, loc, gate.getOpenData(loc));
                    } else {
                        for (var loc : locs) blockManager.hideFakeBlock(player, loc);
                    }
                }
            }
        };

        if (gate.getAnimation() == GateAnimation.INSTANT) {
            task.run();
        } else {
            long interval = 1L;
            if (!groups.isEmpty()) {
                interval = Math.max(1L, gate.getAnimationTicks() / groups.size());
            }
            task.runTaskTimer(plugin, 0L, interval);
        }
    }

    /** Update all players currently online. */
    public void updateAll() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            updatePlayer(p);
        }
    }

    /** Update all gates for a specific player */
    public void updatePlayer(Player player) {
        for (QuestGate gate : gates.values()) {
            if (gate.isClosed(player.getUniqueId())) {
                for (var loc : gate.getBlocks()) {
                    blockManager.showFakeBlock(player, loc, gate.getClosedData(loc));
                }
            } else {
                if (gate.hasOpenCustomBlocks()) {
                    for (var loc : gate.getBlocks()) {
                        blockManager.showFakeBlock(player, loc, gate.getOpenData(loc));
                    }
                } else {
                    for (var loc : gate.getBlocks()) {
                        blockManager.hideFakeBlock(player, loc);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        QuestGate gate = gates.get("office_elevator");
        if (gate != null) {
            gate.setClosed(player.getUniqueId(), true);
            logDebug(player.getName() + " join -> set office_elevator closed");
        }
        // Delay updating until the player's chunks have loaded to ensure
        // the fake blocks are visible on join.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> updatePlayer(player), 10L);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        for (QuestGate gate : gates.values()) {
            if (gate.isClosed(player.getUniqueId())) {
                if (!gate.isInside(event.getFrom()) && gate.isInside(event.getTo())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Player player = event.getPlayer();
        Location loc = event.getClickedBlock().getLocation();
        for (QuestGate gate : gates.values()) {
            if (gate.isClosed(player.getUniqueId()) && gate.isInside(loc)) {
                event.setCancelled(true);
                blockManager.showFakeBlock(player, loc, gate.getClosedData(loc));
                // Resend the fake block shortly after to avoid client-side glitches
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline() && gate.isClosed(player.getUniqueId())) {
                        blockManager.showFakeBlock(player, loc, gate.getClosedData(loc));
                    }
                }, 1L);
                break;
            }
        }
    }
}
