package me.nakilex.levelplugin.fakeblock;

import me.nakilex.levelplugin.Main;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
    private File file;
    private FileConfiguration config;

    public QuestGateManager(Main plugin, FakeBlockManager blockManager) {
        this.plugin = plugin;
        this.blockManager = blockManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadFromConfig();
    }

    public void addGate(QuestGate gate) {
        gates.put(gate.getId().toLowerCase(), gate);
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
        saveConfig();
        updateAll();
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
            Material mat = Material.matchMaterial(config.getString(base + "block", "BARRIER"));
            if (mat == null) mat = Material.BARRIER;
            BlockData data = mat.createBlockData();
            boolean closed = config.getBoolean(base + "closed", true);
            addGate(new QuestGate(key.toLowerCase(), p1, p2, data, closed));
        }
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
            config.set(base + "block", gate.getClosedData().getMaterial().name());
            config.set(base + "closed", gate.isDefaultClosed());
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
        gate.toggle(player.getUniqueId());
        updatePlayer(player);
        return true;
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
                    blockManager.showFakeBlock(player, loc, gate.getClosedData());
                }
            } else {
                for (var loc : gate.getBlocks()) {
                    blockManager.hideFakeBlock(player, loc);
                }
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        updatePlayer(event.getPlayer());
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
}
