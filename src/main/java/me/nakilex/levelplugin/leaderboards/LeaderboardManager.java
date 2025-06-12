package me.nakilex.levelplugin.leaderboards;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads leaderboard definitions from leaderboards.yml and displays them
 * using holograms.
 */
public class LeaderboardManager {
    private final Main plugin;
    private final EconomyManager economy;
    private final PlayerConfig playerConfig;
    private final DuelStatsManager duelStats;

    private File file;
    private FileConfiguration config;
    private final Map<String, Leaderboard> boards = new HashMap<>();

    public LeaderboardManager(Main plugin, EconomyManager eco, PlayerConfig pCfg, DuelStatsManager duelStats) {
        this.plugin = plugin;
        this.economy = eco;
        this.playerConfig = pCfg;
        this.duelStats = duelStats;
        load();
        plugin.getLogger().info("Loaded " + boards.size() + " leaderboard(s)");
        updateAll();
    }

    private void load() {
        file = new File(plugin.getDataFolder(), "leaderboards.yml");
        if (!file.exists()) {
            plugin.saveResource("leaderboards.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        boards.clear();
        ConfigurationSection sec = config.getConfigurationSection("leaderboards");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            String base = id + ".";
            String worldName = sec.getString(id + ".world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;
            double x = sec.getDouble(id + ".x");
            double y = sec.getDouble(id + ".y");
            double z = sec.getDouble(id + ".z");
            String typeStr = sec.getString(id + ".type", "LEVEL").toUpperCase();
            LeaderboardType type;
            try {
                type = LeaderboardType.valueOf(typeStr);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Unknown leaderboard type " + typeStr);
                continue;
            }
            Location loc = new Location(world, x, y, z);
            plugin.getLogger().info("Registering leaderboard '" + id + "' at "
                    + worldName + " " + x + "," + y + "," + z + " type=" + type);
            boards.put(id, new Leaderboard(id, loc, type));
        }
    }

    public Collection<Leaderboard> getBoards() {
        return boards.values();
    }

    public void updateAll() {
        plugin.getLogger().info("Updating " + boards.size() + " leaderboards");
        for (Leaderboard lb : boards.values()) {
            List<String> lines = buildLines(lb.getType());
            plugin.getLogger().fine("Spawning leaderboard " + lb.getId());
            lb.spawn(lines);
        }
    }

    private List<String> buildLines(LeaderboardType type) {
        List<String> lines = new ArrayList<>();
        switch (type) {
            case LEVEL -> lines.add("§eTop Levels");
            case DUELS -> lines.add("§eTop Duel Wins");
            case BALANCE -> lines.add("§eRichest Players");
        }
        List<Map.Entry<UUID,Integer>> top = getTop(type, 10);
        int rank = 1;
        for (Map.Entry<UUID,Integer> e : top) {
            OfflinePlayer off = Bukkit.getOfflinePlayer(e.getKey());
            String name = off.getName() != null ? off.getName() : e.getKey().toString();
            lines.add("§7" + rank + ". §f" + name + " - " + e.getValue());
            rank++;
        }
        return lines;
    }

    private List<Map.Entry<UUID,Integer>> getTop(LeaderboardType type, int limit) {
        Map<UUID,Integer> map = new HashMap<>();
        switch (type) {
            case LEVEL -> {
                FileConfiguration pcfg = playerConfig.getConfig();
                if (pcfg.isConfigurationSection("players")) {
                    for (String uuidStr : pcfg.getConfigurationSection("players").getKeys(false)) {
                        UUID id = UUID.fromString(uuidStr);
                        int lvl = pcfg.getInt("players." + uuidStr + ".level", 1);
                        map.put(id, lvl);
                    }
                }
            }
            case BALANCE -> {
                FileConfiguration bcfg = economy.getBalanceConfig();
                if (bcfg.isConfigurationSection("balances")) {
                    for (String uuidStr : bcfg.getConfigurationSection("balances").getKeys(false)) {
                        UUID id = UUID.fromString(uuidStr);
                        int bal = bcfg.getInt("balances." + uuidStr, 0);
                        map.put(id, bal);
                    }
                }
            }
            case DUELS -> {
                for (String uuidStr : duelStats.getAll().keySet()) {
                    UUID id = UUID.fromString(uuidStr);
                    int w = duelStats.getWins(id);
                    map.put(id, w);
                }
            }
        }
        return map.entrySet().stream()
            .sorted((a,b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save leaderboards.yml: " + e.getMessage());
        }
    }

    /** Reload the leaderboard configuration from disk. */
    public void reload() {
        load();
        plugin.getLogger().info("Reloaded " + boards.size() + " leaderboard(s)");
        updateAll();
    }
}
