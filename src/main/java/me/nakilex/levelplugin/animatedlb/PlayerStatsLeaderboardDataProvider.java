package me.nakilex.levelplugin.animatedlb;

import dev.drawethree.xprison.api.XPrisonAPI;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.tools.ToolDiscipline;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.attributes.lifeskill.LifeSkillProgression;
import me.nakilex.levelplugin.player.attributes.lifeskill.LifeSkillRegistry;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class PlayerStatsLeaderboardDataProvider implements LeaderboardDataProvider {
    private final Main plugin;
    private final Map<ToolDiscipline, LifeSkillProgression> lifeSkills;

    public PlayerStatsLeaderboardDataProvider(Main plugin) {
        this.plugin = plugin;
        this.lifeSkills = LifeSkillRegistry.progressions(plugin);
    }

    @Override
    public List<LeaderboardEntry> getEntries(BoardType type, int limit) {
        return switch (type) {
            case STRONGHOLD_STAGE -> getStronghold(limit);
            case POWER -> getPower(limit);
            case MINING -> getPickaxeLevelTop(limit);
            case XPRISON_RANK -> getXPrisonTop(limit, "ranks", api -> api.getRanksApi().getTopByRank(limit));
            case XPRISON_PRESTIGE -> getXPrisonTop(limit, "prestiges", api -> api.getPrestigesApi().getTopByPrestige(limit));
            case XPRISON_REBIRTH -> getXPrisonTop(limit, "rebirths", api -> api.getRebirthApi().getTopByRebirth(limit));
            case XPRISON_TOKENS -> getXPrisonTop(limit, "token balances", api -> api.getCurrencyApi().getTopByBalance("tokens", limit));
            case FARMING -> getLifeSkill(ToolDiscipline.FARMING, limit);
            case FISHING -> getLifeSkill(ToolDiscipline.FISHING, limit);
        };
    }

    private List<LeaderboardEntry> getStronghold(int limit) {
        List<LeaderboardEntry> out = new ArrayList<>();
        for (UUID id : getKnownPlayers()) {
            var progress = plugin.getStrongholdRunManager().getHighestStageProgress(id);
            out.add(new LeaderboardEntry(id, getPlayerName(id), progress.stage(), progress.wave()));
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
            out.add(new LeaderboardEntry(id, getPlayerName(id), gear, level));
        }
        out.sort(Comparator.comparingDouble(LeaderboardEntry::primaryValue).reversed()
                .thenComparing(Comparator.comparingDouble(LeaderboardEntry::secondaryValue).reversed()));
        return out.subList(0, Math.min(limit, out.size()));
    }

    /**
     * X-Prison owns mining progression now, but its pickaxe level lives on the item's NBT, not per-account -
     * {@code getTopByPickaxeLevel} only scans currently online players' held pickaxe. So for offline players we
     * fall back to the snapshot {@link me.nakilex.levelplugin.xprison.XPrisonPickaxeLevelTracker} persists on
     * join, level-up and quit, and for online players we query X-Prison live for the freshest number.
     */
    private List<LeaderboardEntry> getPickaxeLevelTop(int limit) {
        Map<UUID, Integer> levels = new HashMap<>();
        boolean xPrisonEnabled = xPrisonEnabled();
        for (UUID id : getKnownPlayers()) {
            levels.put(id, plugin.getPlayerConfig().getXPrisonPickaxeLevel(id));
        }

        // X-Prison's own top lookup scans the pickaxes of online players. Merge it into the
        // persisted snapshots so a newly installed tracker works immediately for everyone online.
        if (xPrisonEnabled) {
            try {
                Map<UUID, Integer> live = XPrisonAPI.getInstance().getPickaxeLevelsApi()
                        .getTopByPickaxeLevel(Math.max(limit, Bukkit.getOnlinePlayers().size()));
                if (live != null) {
                    live.forEach((id, level) -> {
                        levels.put(id, level);
                        plugin.getPlayerConfig().setXPrisonPickaxeLevel(id, level);
                    });
                }
            } catch (RuntimeException | LinkageError ex) {
                plugin.getLogger().warning("Could not read X-Prison pickaxe levels for the leaderboard: " + ex.getMessage());
            }
        }

        List<LeaderboardEntry> out = new ArrayList<>();
        levels.forEach((id, level) -> out.add(new LeaderboardEntry(id, getPlayerName(id), level, 0)));
        out.sort(Comparator.comparingDouble(LeaderboardEntry::primaryValue).reversed());
        return out.subList(0, Math.min(limit, out.size()));
    }

    private boolean xPrisonEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("X-Prison");
    }

    /**
     * Rank, prestige, rebirth and currency balances are all stored server-side by X-Prison (unlike pickaxe
     * level, which lives on the item's NBT), so their top-N lookups already cover offline players with no
     * bridge/snapshot needed - just call straight through.
     */
    private List<LeaderboardEntry> getXPrisonTop(int limit, String label,
            java.util.function.Function<XPrisonAPI, Map<UUID, ? extends Number>> query) {
        List<LeaderboardEntry> out = new ArrayList<>();
        if (!xPrisonEnabled()) return out;
        Map<UUID, ? extends Number> top;
        try {
            top = query.apply(XPrisonAPI.getInstance());
        } catch (RuntimeException | LinkageError ex) {
            plugin.getLogger().warning("Could not read X-Prison " + label + " for the leaderboard: " + ex.getMessage());
            return out;
        }
        if (top == null) return out;
        for (Map.Entry<UUID, ? extends Number> entry : top.entrySet()) {
            out.add(new LeaderboardEntry(entry.getKey(), getPlayerName(entry.getKey()), entry.getValue().doubleValue(), 0));
        }
        out.sort(Comparator.comparingDouble(LeaderboardEntry::primaryValue).reversed());
        return out.subList(0, Math.min(limit, out.size()));
    }

    private List<LeaderboardEntry> getLifeSkill(ToolDiscipline discipline, int limit) {
        List<LeaderboardEntry> out = new ArrayList<>();
        LifeSkillProgression progression = lifeSkills.get(discipline);
        if (progression == null) return out;

        String key = LifeSkillRegistry.key(discipline);
        for (UUID id : getKnownPlayers()) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(id);
            int level;
            long totalXp;
            if (offlinePlayer.isOnline()) {
                level = progression.getLevel(id);
                totalXp = progression.getTotalXP(id);
            } else {
                int slot = plugin.getPlayerConfig().getLastActiveSlot(id);
                FileConfiguration cfg = plugin.getPlayerConfig().getConfig();
                String path = "players." + id + ".profiles." + slot + ".lifeskills." + key;
                level = cfg.getInt(path + ".level", 1);
                totalXp = progression.getTotalXP(level, cfg.getInt(path + ".xp", 0));
            }
            out.add(new LeaderboardEntry(id, getPlayerName(id), totalXp, level));
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
