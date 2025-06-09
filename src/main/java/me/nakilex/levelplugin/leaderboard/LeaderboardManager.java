package me.nakilex.levelplugin.leaderboard;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class LeaderboardManager {
    private final Main plugin;
    private final LevelManager levelManager;
    private final EconomyManager economyManager;
    private final DuelStatsManager duelStats;

    private FileConfiguration config;
    private File configFile;

    private final Map<String, LeaderboardEntry> boards = new HashMap<>();

    public LeaderboardManager(Main plugin, LevelManager levelManager, EconomyManager economyManager, DuelStatsManager duelStats) {
        this.plugin = plugin;
        this.levelManager = levelManager;
        this.economyManager = economyManager;
        this.duelStats = duelStats;
        loadConfig();
        loadBoards();
        startTask();
    }

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "leaderboards.yml");
        if (!configFile.exists()) {
            plugin.saveResource("leaderboards.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void loadBoards() {
        ConfigurationSection sec = config.getConfigurationSection("leaderboards");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection cs = sec.getConfigurationSection(id);
            if (cs == null) continue;
            String worldName = cs.getString("world", "world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;
            String locStr = cs.getString("location", "0,0,0");
            String[] parts = locStr.split(",");
            double x=0,y=0,z=0;
            if (parts.length==3) {
                try {
                    x=Double.parseDouble(parts[0]);
                    y=Double.parseDouble(parts[1]);
                    z=Double.parseDouble(parts[2]);
                } catch (NumberFormatException ignored) {}
            }
            LeaderboardType type;
            try {
                type = LeaderboardType.valueOf(cs.getString("type", "LEVEL").toUpperCase());
            } catch (IllegalArgumentException ex) {
                continue;
            }
            Location loc = new Location(world, x, y, z);
            boards.put(id, new LeaderboardEntry(id, world, loc, type));
        }
    }

    private void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                updateAll();
            }
        }.runTaskTimer(plugin, 20L, 20L * 60L); // every minute
    }

    private void spawnArmorStand(Location loc, String text, LeaderboardEntry entry) {
        ArmorStand stand = loc.getWorld().spawn(loc, ArmorStand.class);
        stand.addScoreboardTag("leaderboard_hologram");
        stand.setVisible(false);
        stand.setMarker(true);
        stand.setGravity(false);
        stand.setCustomName(text);
        stand.setCustomNameVisible(true);
        stand.setSilent(true);
        stand.setSmall(true);
        entry.holograms.add(stand);
    }

    private List<String> buildLines(LeaderboardEntry entry) {
        List<String> lines = new ArrayList<>();
        lines.add("§eTop " + entry.type.getDisplay());
        Map<UUID, Integer> values = new HashMap<>();
        for (UUID uuid : StatsManager.getInstance().getAllPlayerUUIDs()) {
            switch (entry.type) {
                case LEVEL -> values.put(uuid, levelManager.getLevel(uuid));
                case BALANCE -> values.put(uuid, economyManager.getBalance(uuid));
                case DUELS -> values.put(uuid, duelStats.getWins(uuid));
            }
        }
        List<Map.Entry<UUID,Integer>> sorted = values.entrySet().stream()
                .sorted((a,b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(5)
                .collect(Collectors.toList());
        int rank = 1;
        for (Map.Entry<UUID,Integer> e : sorted) {
            String name = Optional.ofNullable(Bukkit.getOfflinePlayer(e.getKey()).getName()).orElse("Unknown");
            lines.add("§6" + rank + ". §f" + name + " §7- §a" + e.getValue());
            rank++;
        }
        if (sorted.isEmpty()) {
            lines.add("§7No data");
        }
        return lines;
    }

    public void updateAll() {
        for (LeaderboardEntry entry : boards.values()) {
            // remove old stands
            for (ArmorStand stand : entry.holograms) {
                if (!stand.isDead()) stand.remove();
            }
            entry.holograms.clear();
            // spawn new
            List<String> lines = buildLines(entry);
            Location base = entry.location.clone();
            double y = 0;
            for (String line : lines) {
                Location loc = base.clone().add(0.5, y, 0.5);
                spawnArmorStand(loc, line, entry);
                y -= 0.25;
            }
        }
    }

    public DuelStatsManager getDuelStats() {
        return duelStats;
    }

    public void removeAll() {
        for (LeaderboardEntry entry : boards.values()) {
            for (ArmorStand stand : entry.holograms) {
                if (!stand.isDead()) stand.remove();
            }
            entry.holograms.clear();
        }
    }
}
