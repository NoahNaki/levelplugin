package me.nakilex.levelplugin.leaderboards;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.settings.managers.SettingsManager;
import me.nakilex.levelplugin.environment.EnvironmentManager;
import me.nakilex.levelplugin.guild.Guild;
import me.nakilex.levelplugin.guild.GuildManager;
import me.nakilex.levelplugin.guild.siege.GuildSiegeManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads leaderboard definitions from config.yml and displays them using holograms.
 */
public class LeaderboardManager {
    private final Main plugin;
    private final EconomyManager economy;
    private final PlayerConfig playerConfig;
    private final DuelStatsManager duelStats;
    private final SettingsManager settingsManager;

    private final File file;
    private FileConfiguration config;
    private final Map<String, Leaderboard> boards = new HashMap<>();
    /** Whether leaderboards are currently visible. */
    private boolean visible = false;


    public LeaderboardManager(Main plugin, EconomyManager eco, PlayerConfig pCfg, DuelStatsManager duelStats, SettingsManager settingsManager) {
        this.plugin = plugin;
        this.economy = eco;
        this.playerConfig = pCfg;
        this.duelStats = duelStats;
        this.settingsManager = settingsManager;
        this.file = new File(plugin.getDataFolder(), "config.yml");
        load();
        plugin.getLogger().info("Loaded " + boards.size() + " leaderboard(s)");
        updateAll();
    }

    private void load() {
        config = YamlConfiguration.loadConfiguration(file);
        boards.clear();
        ConfigurationSection sec = config.getConfigurationSection("leaderboards");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            String base = id + ".";
            String worldName = sec.getString(base + "world");
            World world = Bukkit.getWorld(worldName);
            if (world == null && "world2".equalsIgnoreCase(worldName)) {
                world = Bukkit.getWorld("world");
            }
            if (world == null) continue;
            double x = sec.getDouble(base + "x");
            double y = sec.getDouble(base + "y");
            double z = sec.getDouble(base + "z");
            String typeStr = sec.getString(base + "type", "LEVEL").toUpperCase();
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
        if (!checkVisibility()) {
            return;
        }
        plugin.getLogger().info("Updating " + boards.size() + " leaderboards");
        for (LeaderboardType type : LeaderboardType.values()) {
            updateTypeInternal(type);
        }
    }

    /** Update only leaderboards of a specific type. */
    public void updateType(LeaderboardType type) {
        if (!checkVisibility()) {
            return;
        }
        updateTypeInternal(type);
    }

    private void updateTypeInternal(LeaderboardType type) {
        for (Leaderboard lb : boards.values()) {
            if (lb.getType() != type) continue;
            List<String> lines = buildLines(type);
            plugin.getLogger().fine("Spawning leaderboard " + lb.getId());
            lb.spawn(lines);
        }
    }

    /** Spawn holograms for all leaderboards. */
    public void addAll() {
        updateAll();
    }

    /** Remove all hologram entities. */
    public void removeAll() {
        for (Leaderboard lb : boards.values()) {
            lb.despawn();
        }
        visible = false;
    }

    /** Determine if leaderboards should currently be displayed based on town progress. */
    private boolean checkVisibility() {
        boolean shouldDisplay = shouldDisplayLeaderboards();
        if (!shouldDisplay && visible) {
            removeAll();
        }
        visible = shouldDisplay;
        return shouldDisplay;
    }

    private boolean shouldDisplayLeaderboards() {
        GuildSiegeManager siege = GuildSiegeManager.getInstance();
        String ownerName = siege.getOwnerGuild();
        if (ownerName == null) {
            return false;
        }
        Guild g = GuildManager.getInstance().getGuild(ownerName);
        if (g == null) {
            return false;
        }
        UUID leader = g.getLeader();
        EnvironmentManager env = plugin.getEnvironmentManager();
        if (env == null) {
            plugin.getLogger().warning("Environment manager missing; hiding leaderboards");
            return false;
        }
        int stage = env.getBuildingStage(leader, "foundation");
        return stage >= 2;
    }

    private List<String> buildLines(LeaderboardType type) {
        List<String> lines = new ArrayList<>();

        String color;
        switch (type) {
            case LEVEL -> {
                lines.add("§a§lLEVEL LEADERBOARD");
                color = "§a";
            }
            case DUELS -> {
                lines.add("§c§lDUELS LEADERBOARD");
                color = "§c";
            }
            case BALANCE -> {
                lines.add("§e§lBALANCE LEADERBOARD");
                color = "§e";
            }
            default -> {
                lines.add("§eLEADERBOARD");
                color = "§e";
            }
        }

        List<Map.Entry<UUID, Integer>> top = getTop(type, 10);
        int rank = 1;
        for (Map.Entry<UUID, Integer> e : top) {
            OfflinePlayer off = Bukkit.getOfflinePlayer(e.getKey());
            String name = off.getName() != null ? off.getName() : e.getKey().toString();
            String value = color + e.getValue();
            if (type == LeaderboardType.BALANCE) {
                value += " <glyph:coins_icon>";
            } else if (type == LeaderboardType.DUELS) {
                value += " \uD83D\uDDE1"; // 🗡 symbol
            }
            lines.add(color + "#" + rank + " §7| §f" + name + ": " + value);
            rank++;
        }

        // spacer for "your rank" line
        lines.add(" ");

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
                        if (!settingsManager.getSettings(id).isBalancePublic()) continue;
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
            plugin.getLogger().severe("Failed to save config.yml: " + e.getMessage());
        }
    }

    /** Reload the leaderboard configuration from disk. */
    public void reload() {
        load();
        plugin.getLogger().info("Reloaded " + boards.size() + " leaderboard(s)");
        updateAll();
    }
}
