package me.nakilex.levelplugin.fakeblock;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.quests.managers.QuestManager;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Handles quest gated fake blocks. When a player hasn't completed the
 * associated quest the gate appears closed and movement through the area is
 * prevented.
 */
public class QuestGateManager implements Listener {

    private final Main plugin;
    private final QuestManager questManager;
    private final FakeBlockManager blockManager;
    private final List<QuestGate> gates = new ArrayList<>();

    public QuestGateManager(Main plugin, QuestManager questManager, FakeBlockManager blockManager) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.blockManager = blockManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadFromConfig();
    }

    public void addGate(QuestGate gate) {
        gates.add(gate);
    }

    private void loadFromConfig() {
        File file = new File(plugin.getDataFolder(), "gates.yml");
        if (!file.exists()) {
            plugin.saveResource("gates.yml", false);
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        if (!cfg.isConfigurationSection("gates")) return;
        for (String key : cfg.getConfigurationSection("gates").getKeys(false)) {
            String base = "gates." + key + ".";
            String quest = cfg.getString(base + "quest");
            String worldName = cfg.getString(base + "world");
            World world = plugin.getServer().getWorld(worldName);
            if (quest == null || world == null) continue;

            Location p1 = readLocation(world, cfg, base + "pos1");
            Location p2 = readLocation(world, cfg, base + "pos2");
            if (p1 == null || p2 == null) continue;
            Material mat = Material.matchMaterial(cfg.getString(base + "block", "BARRIER"));
            if (mat == null) mat = Material.BARRIER;
            BlockData data = mat.createBlockData();
            addGate(new QuestGate(quest, p1, p2, data));
        }
    }

    private Location readLocation(World world, FileConfiguration cfg, String path) {
        if (!cfg.isConfigurationSection(path)) return null;
        int x = cfg.getInt(path + ".x");
        int y = cfg.getInt(path + ".y");
        int z = cfg.getInt(path + ".z");
        return new Location(world, x, y, z);
    }

    /** Update all gates for a specific player */
    public void updatePlayer(Player player) {
        for (QuestGate gate : gates) {
            if (questManager.hasCompleted(player.getUniqueId(), gate.getQuestId())) {
                for (var loc : gate.getBlocks()) {
                    blockManager.hideFakeBlock(player, loc);
                }
            } else {
                for (var loc : gate.getBlocks()) {
                    blockManager.showFakeBlock(player, loc, gate.getClosedData());
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
        for (QuestGate gate : gates) {
            if (!questManager.hasCompleted(player.getUniqueId(), gate.getQuestId())) {
                if (!gate.isInside(event.getFrom()) && gate.isInside(event.getTo())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
}
