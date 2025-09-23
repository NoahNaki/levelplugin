package me.nakilex.levelplugin.arena.rating;

import me.nakilex.levelplugin.player.config.PlayerConfig;
import org.bukkit.ChatColor;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains player-versus-player arena ratings with a dynamic ELO-like
 * calculation. Ratings are persisted in {@code player_data.yml} so they
 * survive restarts and take volatility into account when computing matchmaking
 * windows.
 */
public class ArenaRatingManager {
    private static final int DEFAULT_RATING = 1200;
    private static final double DEFAULT_DEVIATION = 350.0;
    private static final int MIN_RATING = 0;
    private static final int MAX_RATING = 4000;

    private final PlayerConfig playerConfig;
    private final Map<UUID, RatingProfile> profiles = new ConcurrentHashMap<>();

    public ArenaRatingManager(PlayerConfig playerConfig) {
        this.playerConfig = playerConfig;
    }

    /** Snapshot the player's current rating data. */
    public RatingSnapshot getSnapshot(UUID playerId) {
        RatingProfile profile = profiles.computeIfAbsent(playerId, this::loadProfile);
        return profile.toSnapshot();
    }

    /** Current rating value for the player. */
    public int getRating(UUID playerId) {
        return getSnapshot(playerId).rating();
    }

    /**
     * Apply the rating change for a completed match. The returned update contains
     * both the before/after values for each participant so callers can surface
     * rich feedback messages.
     */
    public synchronized RatingUpdate recordMatch(UUID winnerId, UUID loserId) {
        RatingProfile winner = profiles.computeIfAbsent(winnerId, this::loadProfile);
        RatingProfile loser = profiles.computeIfAbsent(loserId, this::loadProfile);

        RatingSnapshot winnerBefore = winner.toSnapshot();
        RatingSnapshot loserBefore = loser.toSnapshot();

        double expectedWinner = expectedScore(winner.rating, loser.rating);
        double expectedLoser = expectedScore(loser.rating, winner.rating);

        double winnerK = computeKFactor(winner);
        double loserK = computeKFactor(loser);

        winner.rating = clampRating(winner.rating + (int) Math.round(winnerK * (1.0 - expectedWinner)));
        loser.rating = clampRating(loser.rating + (int) Math.round(loserK * (0.0 - expectedLoser)));

        winner.matches++;
        loser.matches++;

        adjustDeviation(winner, Math.abs(winner.rating - winnerBefore.rating()));
        adjustDeviation(loser, Math.abs(loser.rating - loserBefore.rating()));

        RatingSnapshot winnerAfter = winner.toSnapshot();
        RatingSnapshot loserAfter = loser.toSnapshot();

        persist(winnerId, winner);
        persist(loserId, loser);

        return new RatingUpdate(winnerBefore, winnerAfter, loserBefore, loserAfter);
    }

    /** Tier describing the player's skill bracket. */
    public RatingTier getTier(int rating) {
        RatingTier best = RatingTier.BRONZE;
        for (RatingTier tier : RatingTier.valuesDesc) {
            if (rating >= tier.threshold) {
                best = tier;
                break;
            }
        }
        return best;
    }

    /** Color-coded tier label suitable for chat or GUI lore. */
    public String formatTier(int rating) {
        RatingTier tier = getTier(rating);
        return tier.color + tier.displayName;
    }

    /** Compute the matchmaking tolerance (rating window) for the player. */
    public int computeMatchWindow(UUID playerId, Duration waitTime) {
        RatingProfile profile = profiles.computeIfAbsent(playerId, this::loadProfile);
        return profile.toSnapshot().matchWindow(waitTime);
    }

    private RatingProfile loadProfile(UUID playerId) {
        if (playerConfig == null) {
            return new RatingProfile(DEFAULT_RATING, DEFAULT_DEVIATION, 0);
        }
        String base = "players." + playerId + ".arena.";
        int rating = playerConfig.getConfig().getInt(base + "elo", DEFAULT_RATING);
        double deviation = playerConfig.getConfig().getDouble(base + "deviation", DEFAULT_DEVIATION);
        int matches = playerConfig.getConfig().getInt(base + "matches", 0);
        return new RatingProfile(rating, deviation, matches);
    }

    private void persist(UUID playerId, RatingProfile profile) {
        if (playerConfig == null) return;
        String base = "players." + playerId + ".arena.";
        playerConfig.getConfig().set(base + "elo", profile.rating);
        playerConfig.getConfig().set(base + "deviation", profile.deviation);
        playerConfig.getConfig().set(base + "matches", profile.matches);
        playerConfig.saveConfigFile();
    }

    private static double expectedScore(int rating, int opponent) {
        return 1.0 / (1.0 + Math.pow(10.0, (opponent - rating) / 400.0));
    }

    private static int clampRating(int rating) {
        if (rating < MIN_RATING) return MIN_RATING;
        if (rating > MAX_RATING) return MAX_RATING;
        return rating;
    }

    private static double computeKFactor(RatingProfile profile) {
        double base = profile.matches < 15 ? 36.0 : 28.0;
        base += Math.min(20.0, profile.deviation / 12.0);
        if (profile.rating >= 1800) {
            base -= Math.min(10.0, (profile.rating - 1800) / 60.0);
        }
        return Math.max(16.0, Math.min(48.0, base));
    }

    private static void adjustDeviation(RatingProfile profile, int ratingChange) {
        double dampen = profile.matches > 25 ? 0.82 : 0.9;
        double adjustment = Math.min(80.0, ratingChange * 0.6 + (profile.matches < 10 ? 25.0 : 10.0));
        profile.deviation = Math.max(35.0, Math.min(350.0, profile.deviation * dampen + adjustment));
    }

    private static class RatingProfile {
        private int rating;
        private double deviation;
        private int matches;

        RatingProfile(int rating, double deviation, int matches) {
            this.rating = rating;
            this.deviation = deviation;
            this.matches = matches;
        }

        RatingSnapshot toSnapshot() {
            return new RatingSnapshot(rating, deviation, matches);
        }
    }

    /** Immutable snapshot returned to external callers. */
    public record RatingSnapshot(int rating, double deviation, int matches) {
        public int matchWindow(Duration waitTime) {
            long seconds = Math.max(0L, waitTime.toSeconds());
            int base = matches < 10 ? 180 : 120;
            base += (int) Math.round(deviation * 0.45);
            base += (int) Math.min(200, (seconds / 20) * 28);
            return Math.max(80, Math.min(600, base));
        }
    }

    /** Result describing the before/after ratings for both players. */
    public record RatingUpdate(RatingSnapshot winnerBefore,
                               RatingSnapshot winnerAfter,
                               RatingSnapshot loserBefore,
                               RatingSnapshot loserAfter) {
        public int winnerDelta() {
            return winnerAfter.rating() - winnerBefore.rating();
        }

        public int loserDelta() {
            return loserAfter.rating() - loserBefore.rating();
        }
    }

    /**
     * Basic skill brackets used for GUI presentation. The array is stored in
     * descending order so lookup is efficient.
     */
    public enum RatingTier {
        MYTHIC(2400, ChatColor.DARK_PURPLE, "Mythic"),
        CHAMPION(2100, ChatColor.DARK_RED, "Champion"),
        DIAMOND(1850, ChatColor.AQUA, "Diamond"),
        PLATINUM(1600, ChatColor.BLUE, "Platinum"),
        GOLD(1400, ChatColor.GOLD, "Gold"),
        SILVER(1200, ChatColor.WHITE, "Silver"),
        BRONZE(0, ChatColor.GRAY, "Bronze");

        private static final RatingTier[] valuesDesc;

        static {
            EnumSet<RatingTier> set = EnumSet.allOf(RatingTier.class);
            valuesDesc = set.stream()
                    .sorted((a, b) -> Integer.compare(b.threshold, a.threshold))
                    .toArray(RatingTier[]::new);
        }

        private final int threshold;
        private final ChatColor color;
        private final String displayName;

        RatingTier(int threshold, ChatColor color, String displayName) {
            this.threshold = threshold;
            this.color = color;
            this.displayName = displayName;
        }

        public ChatColor color() {
            return color;
        }

        public String displayName() {
            return displayName;
        }
    }
}
