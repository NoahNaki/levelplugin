package me.nakilex.playerspoofer;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks genuine server users so automatic spoofing never reuses a name that has actually
 * connected here. Async pre-login reservations close the race between a real login and the
 * population controller selecting the same display name.
 */
final class RealPlayerRegistry {
    private final PlayerSpooferPlugin plugin;
    private final Set<String> historicalNames = ConcurrentHashMap.newKeySet();
    private final Set<UUID> historicalUuids = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, Long> preLoginReservations = new ConcurrentHashMap<>();
    private boolean enabled;
    private boolean excludeHistory;
    private long reservationMillis;

    RealPlayerRegistry(PlayerSpooferPlugin plugin) {
        this.plugin = plugin;
    }

    void initialize() {
        enabled = plugin.getConfig().getBoolean("collision-protection.enabled", true);
        excludeHistory = plugin.getConfig().getBoolean("collision-protection.exclude-real-player-history", true);
        reservationMillis = Math.max(1L, plugin.getConfig().getLong("collision-protection.prelogin-reservation-minutes", 1440L)) * 60_000L;

        for (String name : plugin.getConfig().getStringList("real-player-history.names")) {
            if (name != null && !name.isBlank()) historicalNames.add(key(name));
        }
        for (String raw : plugin.getConfig().getStringList("real-player-history.uuids")) {
            try {
                historicalUuids.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (plugin.getConfig().getBoolean("collision-protection.import-existing-server-history", true)) {
            int beforeNames = historicalNames.size();
            int beforeUuids = historicalUuids.size();
            try {
                for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
                    if (!offline.hasPlayedBefore()) continue;
                    if (offline.getName() != null && !offline.getName().isBlank()) historicalNames.add(key(offline.getName()));
                    historicalUuids.add(offline.getUniqueId());
                }
            } catch (Throwable throwable) {
                plugin.getLogger().warning("Could not import Paper offline-player history: " + throwable.getMessage());
            }
            if (historicalNames.size() != beforeNames || historicalUuids.size() != beforeUuids) save();
        }

        plugin.getLogger().info("Real-player collision guard loaded " + historicalNames.size()
                + " historical name(s) and " + historicalUuids.size() + " UUID(s).");
    }

    /** Safe to call from AsyncPlayerPreLoginEvent. Does not touch Bukkit/config APIs. */
    void reservePreLogin(String name, UUID uuid) {
        if (!enabled || name == null || name.isBlank()) return;
        preLoginReservations.put(key(name), System.currentTimeMillis() + reservationMillis);
        // UUID is deliberately not made permanent until PlayerJoinEvent confirms the join.
    }

    void recordSuccessfulJoin(Player player) {
        if (player == null) return;
        historicalNames.add(key(player.getName()));
        historicalUuids.add(player.getUniqueId());
        preLoginReservations.remove(key(player.getName()));
        save();
    }

    boolean isExcludedName(String name) {
        if (!enabled || name == null) return false;
        cleanupExpiredReservations();
        String key = key(name);
        if (excludeHistory && historicalNames.contains(key)) return true;
        Long until = preLoginReservations.get(key);
        return until != null && until > System.currentTimeMillis();
    }

    boolean isHistoricalName(String name) {
        return name != null && historicalNames.contains(key(name));
    }

    boolean isHistoricalUuid(UUID uuid) {
        return uuid != null && historicalUuids.contains(uuid);
    }

    int historyNameCount() { return historicalNames.size(); }
    int historyUuidCount() { return historicalUuids.size(); }

    int activeReservationCount() {
        cleanupExpiredReservations();
        return preLoginReservations.size();
    }

    private void cleanupExpiredReservations() {
        long now = System.currentTimeMillis();
        preLoginReservations.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private void save() {
        // Successful joins/imports happen on the primary thread.
        List<String> names = new ArrayList<>(historicalNames);
        names.sort(String.CASE_INSENSITIVE_ORDER);
        List<String> uuids = historicalUuids.stream().map(UUID::toString).sorted().toList();
        plugin.getConfig().set("real-player-history.names", names);
        plugin.getConfig().set("real-player-history.uuids", uuids);
        plugin.saveConfig();
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
