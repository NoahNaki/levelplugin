package me.nakilex.levelplugin.animatedlb;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class PlayerStatsLeaderboardDataProvider implements LeaderboardDataProvider {
    private final Main plugin;

    public PlayerStatsLeaderboardDataProvider(Main plugin) { this.plugin = plugin; }

    @Override
    public List<LeaderboardEntry> getEntries(BoardType type, int limit) {
        return switch (type) {
            case STRONGHOLD_STAGE -> getStronghold(limit);
            case POWER -> getPower(limit);
        };
    }

    private List<LeaderboardEntry> getStronghold(int limit) {
        List<LeaderboardEntry> out = new ArrayList<>();
        for (UUID id : getKnownPlayers()) {
            var progress = plugin.getStrongholdRunManager().getHighestStageProgress(id);
            out.add(new LeaderboardEntry(getPlayerName(id), progress.stage(), progress.wave()));
        }
        out.sort(Comparator.comparingDouble(LeaderboardEntry::primaryValue).reversed()
                .thenComparing(Comparator.comparingDouble(LeaderboardEntry::secondaryValue).reversed()));
        return out.subList(0, Math.min(limit, out.size()));
    }

    private List<LeaderboardEntry> getPower(int limit) {
        List<LeaderboardEntry> out = new ArrayList<>();
        FileConfiguration cfg = plugin.getPlayerConfig().getConfig();
        for (UUID id : getKnownPlayers()) {
            int level = cfg.getInt("players." + id + ".level", 1);
            OfflinePlayer off = Bukkit.getOfflinePlayer(id);
            int gear = off.isOnline() ? ItemUtil.calculateTotalGearScore(off.getPlayer()) : 0;
            out.add(new LeaderboardEntry(getPlayerName(id), gear, level));
        }
        out.sort(Comparator.comparingDouble(LeaderboardEntry::primaryValue).reversed()
                .thenComparing(Comparator.comparingDouble(LeaderboardEntry::secondaryValue).reversed()));
        return out.subList(0, Math.min(limit, out.size()));
    }

    private Set<UUID> getKnownPlayers() {
        Set<UUID> ids = new HashSet<>();
        FileConfiguration cfg = plugin.getPlayerConfig().getConfig();
        if (cfg.isConfigurationSection("players")) {
            for (String key : cfg.getConfigurationSection("players").getKeys(false)) {
                try { ids.add(UUID.fromString(key)); } catch (IllegalArgumentException ignored) {}
            }
        }
        return ids;
    }

    private String getPlayerName(UUID id) {
        String n = Bukkit.getOfflinePlayer(id).getName();
        return n == null ? "NONE" : n;
    }
}
