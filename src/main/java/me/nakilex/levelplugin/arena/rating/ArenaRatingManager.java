package me.nakilex.levelplugin.arena.rating;

import me.nakilex.levelplugin.player.config.PlayerConfig;
import org.bukkit.ChatColor;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    public enum RatingCategory {
        DUEL(null),
        TEAM_2V2("team_2v2");

        private final String pathSegment;

        RatingCategory(String pathSegment) {
            this.pathSegment = pathSegment;
        }

        String pathSegment() {
            return pathSegment;
        }
    }

    private final PlayerConfig playerConfig;
    private final Map<RatingCategory, Map<UUID, RatingProfile>> profiles = new EnumMap<>(RatingCategory.class);

    public ArenaRatingManager(PlayerConfig playerConfig) {
        this.playerConfig = playerConfig;
        for (RatingCategory category : RatingCategory.values()) {
            profiles.put(category, new ConcurrentHashMap<>());
        }
    }

    /** Snapshot the player's current rating data for the default duel ladder. */
    public RatingSnapshot getSnapshot(UUID playerId) {
        return getSnapshot(playerId, RatingCategory.DUEL);
    }

    /** Snapshot the player's rating data for the specified ladder. */
    public RatingSnapshot getSnapshot(UUID playerId, RatingCategory category) {
        RatingProfile profile = profileMap(category).computeIfAbsent(playerId, id -> loadProfile(id, category));
        return profile.toSnapshot();
    }

    /** Current rating value for the player on the default duel ladder. */
    public int getRating(UUID playerId) {
        return getRating(playerId, RatingCategory.DUEL);
    }

    /** Current rating value for the player on the specified ladder. */
    public int getRating(UUID playerId, RatingCategory category) {
        return getSnapshot(playerId, category).rating();
    }

    /** Apply the rating change for a 1v1 match on the default ladder. */
    public synchronized RatingUpdate recordMatch(UUID winnerId, UUID loserId) {
        return recordMatch(winnerId, loserId, RatingCategory.DUEL);
    }

    /** Apply the rating change for a 1v1 match on the specified ladder. */
    public synchronized RatingUpdate recordMatch(UUID winnerId, UUID loserId, RatingCategory category) {
        MultiRatingUpdate update = recordMatchInternal(List.of(winnerId), List.of(loserId), category);
        RatingChange winnerChange = update.change(winnerId);
        RatingChange loserChange = update.change(loserId);
        return new RatingUpdate(winnerChange.before(), winnerChange.after(), loserChange.before(), loserChange.after());
    }

    /** Apply the rating change for a multi-player match on the specified ladder. */
    public synchronized MultiRatingUpdate recordMatch(Collection<UUID> winners,
                                                      Collection<UUID> losers,
                                                      RatingCategory category) {
        return recordMatchInternal(winners, losers, category);
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

    /** Compute the matchmaking tolerance (rating window) for the player on the default ladder. */
    public int computeMatchWindow(UUID playerId, Duration waitTime) {
        return computeMatchWindow(playerId, waitTime, RatingCategory.DUEL);
    }

    /** Compute the matchmaking tolerance (rating window) for the player on the specified ladder. */
    public int computeMatchWindow(UUID playerId, Duration waitTime, RatingCategory category) {
        RatingProfile profile = profileMap(category).computeIfAbsent(playerId, id -> loadProfile(id, category));
        return profile.toSnapshot().matchWindow(waitTime);
    }

    /** Message describing a tier promotion/demotion, if one occurred. */
    public Optional<String> buildTierChangeMessage(int before, int after) {
        RatingTier beforeTier = getTier(before);
        RatingTier afterTier = getTier(after);
        if (beforeTier == afterTier) {
            return Optional.empty();
        }
        String message = ChatColor.GRAY + "Your arena tier is now " + afterTier.color + afterTier.displayName + ChatColor.GRAY + "!";
        return Optional.of(message);
    }

    private MultiRatingUpdate recordMatchInternal(Collection<UUID> winners,
                                                  Collection<UUID> losers,
                                                  RatingCategory category) {
        Map<UUID, RatingProfile> profileMap = profileMap(category);
        Map<UUID, RatingProfile> winnerProfiles = new HashMap<>();
        Map<UUID, RatingProfile> loserProfiles = new HashMap<>();

        for (UUID id : winners) {
            winnerProfiles.put(id, profileMap.computeIfAbsent(id, key -> loadProfile(key, category)));
        }
        for (UUID id : losers) {
            loserProfiles.put(id, profileMap.computeIfAbsent(id, key -> loadProfile(key, category)));
        }

        if (winnerProfiles.isEmpty() || loserProfiles.isEmpty()) {
            return new MultiRatingUpdate(Map.of());
        }

        double losersAverage = loserProfiles.values().stream()
                .mapToInt(profile -> profile.rating)
                .average()
                .orElse(DEFAULT_RATING);
        double winnersAverage = winnerProfiles.values().stream()
                .mapToInt(profile -> profile.rating)
                .average()
                .orElse(DEFAULT_RATING);

        Map<UUID, RatingChange> changes = new HashMap<>();
        boolean dirty = false;

        for (Map.Entry<UUID, RatingProfile> entry : winnerProfiles.entrySet()) {
            UUID playerId = entry.getKey();
            RatingProfile profile = entry.getValue();
            RatingSnapshot before = profile.toSnapshot();
            double expected = expectedScore(profile.rating, losersAverage);
            double k = computeKFactor(profile);
            int delta = (int) Math.round(k * (1.0 - expected));
            profile.rating = clampRating(profile.rating + delta);
            profile.matches++;
            adjustDeviation(profile, Math.abs(profile.rating - before.rating()));
            RatingSnapshot after = profile.toSnapshot();
            changes.put(playerId, new RatingChange(before, after));
            dirty |= persist(playerId, profile, category);
        }

        for (Map.Entry<UUID, RatingProfile> entry : loserProfiles.entrySet()) {
            UUID playerId = entry.getKey();
            RatingProfile profile = entry.getValue();
            RatingSnapshot before = profile.toSnapshot();
            double expected = expectedScore(profile.rating, winnersAverage);
            double k = computeKFactor(profile);
            int delta = (int) Math.round(k * (0.0 - expected));
            profile.rating = clampRating(profile.rating + delta);
            profile.rating = Math.max(profile.rating, minimumRatingFloor(profile));
            profile.matches++;
            adjustDeviation(profile, Math.abs(profile.rating - before.rating()));
            RatingSnapshot after = profile.toSnapshot();
            changes.put(playerId, new RatingChange(before, after));
            dirty |= persist(playerId, profile, category);
        }

        if (dirty && playerConfig != null) {
            playerConfig.saveConfigFile();
        }

        return new MultiRatingUpdate(Collections.unmodifiableMap(new HashMap<>(changes)));
    }

    private Map<UUID, RatingProfile> profileMap(RatingCategory category) {
        return profiles.computeIfAbsent(category, key -> new ConcurrentHashMap<>());
    }

    private RatingProfile loadProfile(UUID playerId, RatingCategory category) {
        if (playerConfig == null) {
            return new RatingProfile(DEFAULT_RATING, DEFAULT_DEVIATION, 0);
        }
        String base = pathPrefix(playerId, category);
        int rating = playerConfig.getConfig().getInt(base + "elo", DEFAULT_RATING);
        double deviation = playerConfig.getConfig().getDouble(base + "deviation", DEFAULT_DEVIATION);
        int matches = playerConfig.getConfig().getInt(base + "matches", 0);
        return new RatingProfile(rating, deviation, matches);
    }

    private boolean persist(UUID playerId, RatingProfile profile, RatingCategory category) {
        if (playerConfig == null) {
            return false;
        }
        String base = pathPrefix(playerId, category);
        playerConfig.getConfig().set(base + "elo", profile.rating);
        playerConfig.getConfig().set(base + "deviation", profile.deviation);
        playerConfig.getConfig().set(base + "matches", profile.matches);
        return true;
    }

    private String pathPrefix(UUID playerId, RatingCategory category) {
        String base = "players." + playerId + ".arena.";
        if (category.pathSegment() == null || category.pathSegment().isEmpty()) {
            return base;
        }
        return base + category.pathSegment() + ".";
    }

    private static double expectedScore(double rating, double opponent) {
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

    private static int minimumRatingFloor(RatingProfile profile) {
        if (profile == null) {
            return MIN_RATING;
        }
        if (profile.matches < 10) {
            return 900;
        }
        if (profile.matches < 20) {
            return 700;
        }
        return MIN_RATING;
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

    /** Before/after pair for a single player's rating adjustment. */
    public record RatingChange(RatingSnapshot before, RatingSnapshot after) {
        public int delta() {
            return after.rating() - before.rating();
        }
    }

    /** Result describing the before/after ratings for all participants. */
    public record MultiRatingUpdate(Map<UUID, RatingChange> changes) {
        public RatingChange change(UUID playerId) {
            return changes.get(playerId);
        }
    }

    /** Result describing the before/after ratings for both players in a duel. */
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
