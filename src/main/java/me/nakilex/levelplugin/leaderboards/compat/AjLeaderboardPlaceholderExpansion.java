package me.nakilex.levelplugin.leaderboards.compat;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.nakilex.levelplugin.Main;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compatibility expansion for the ajLeaderboards placeholders used by the prison setup.
 */
public final class AjLeaderboardPlaceholderExpansion extends PlaceholderExpansion {
    private static final String PERIODS = "alltime|hourly|daily|weekly|monthly|yearly";
    private static final Pattern LIST = Pattern.compile(
            "^lb_(.+)_([1-9][0-9]*)_(" + PERIODS + ")_(name|value|value_formatted|prefix|suffix|displayname)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAYER = Pattern.compile(
            "^(position|value|value_formatted)_(.+)_(" + PERIODS + ")$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SIZE = Pattern.compile("^size_(.+)_(" + PERIODS + ")$", Pattern.CASE_INSENSITIVE);

    private final Main plugin;
    private final LeaderboardSystem system;

    public AjLeaderboardPlaceholderExpansion(Main plugin, LeaderboardSystem system) {
        this.plugin = plugin;
        this.system = system;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ajlb";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        return resolve(player == null ? null : player.getUniqueId(), params);
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        return resolve(player == null ? null : player.getUniqueId(), params);
    }

    private String resolve(UUID playerId, String params) {
        Matcher list = LIST.matcher(params);
        if (list.matches()) {
            String board = list.group(1).toLowerCase(Locale.ROOT);
            int requestedRank = Integer.parseInt(list.group(2));
            LeaderboardPeriod period = LeaderboardPeriod.parse(list.group(3));
            List<LeaderboardSystem.RankedScore> scores = system.getTop(board, period, requestedRank);
            if (scores.size() < requestedRank) {
                return list.group(4).equalsIgnoreCase("name") || list.group(4).equalsIgnoreCase("displayname")
                        ? system.noDataName() : system.noDataValue();
            }
            LeaderboardSystem.ScoreRecord score = scores.get(requestedRank - 1).score();
            return switch (list.group(4).toLowerCase(Locale.ROOT)) {
                case "name" -> score.name();
                case "displayname" -> score.displayName();
                case "prefix" -> score.prefix();
                case "suffix" -> score.suffix();
                case "value_formatted" -> system.formatValue(board, score.value(period), true);
                default -> system.formatValue(board, score.value(period), false);
            };
        }

        Matcher own = PLAYER.matcher(params);
        if (own.matches()) {
            if (playerId == null) return system.noDataValue();
            String field = own.group(1).toLowerCase(Locale.ROOT);
            String board = own.group(2).toLowerCase(Locale.ROOT);
            LeaderboardPeriod period = LeaderboardPeriod.parse(own.group(3));
            LeaderboardSystem.RankedScore ranked = system.getRank(board, period, playerId);
            if (ranked == null) return system.noDataValue();
            if (field.equals("position")) return Integer.toString(ranked.rank());
            return system.formatValue(board, ranked.score().value(period), field.equals("value_formatted"));
        }

        Matcher size = SIZE.matcher(params);
        if (size.matches()) {
            return Integer.toString(system.getTop(size.group(1), LeaderboardPeriod.parse(size.group(2)), Integer.MAX_VALUE).size());
        }
        return null;
    }
}
