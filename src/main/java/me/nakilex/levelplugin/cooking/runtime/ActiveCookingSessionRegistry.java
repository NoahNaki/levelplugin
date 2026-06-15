package me.nakilex.levelplugin.cooking.runtime;

import me.nakilex.levelplugin.cooking.util.CookingLocationKey;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Runtime-only active cooking session locks, separated from placed workstation tracking. */
public class ActiveCookingSessionRegistry {
    private final Map<UUID, ActiveCookingSession> byPlayer = new ConcurrentHashMap<>();
    private final Map<CookingLocationKey, ActiveCookingSession> byWorkstation = new ConcurrentHashMap<>();

    public Optional<ActiveCookingSession> getByPlayer(UUID playerId) {
        return Optional.ofNullable(playerId == null ? null : byPlayer.get(playerId));
    }

    public Optional<ActiveCookingSession> getByWorkstation(CookingLocationKey workstationKey) {
        return Optional.ofNullable(workstationKey == null ? null : byWorkstation.get(workstationKey));
    }

    public CreateResult create(UUID playerId, CookingLocationKey workstationKey, String recipeId) {
        if (playerId == null || workstationKey == null || recipeId == null || recipeId.isBlank()) {
            return CreateResult.INVALID;
        }
        if (byPlayer.containsKey(playerId)) {
            return CreateResult.PLAYER_BUSY;
        }
        if (byWorkstation.containsKey(workstationKey)) {
            return CreateResult.WORKSTATION_BUSY;
        }
        ActiveCookingSession session = new ActiveCookingSession(playerId, workstationKey, recipeId);
        byPlayer.put(playerId, session);
        ActiveCookingSession previous = byWorkstation.putIfAbsent(workstationKey, session);
        if (previous != null) {
            byPlayer.remove(playerId, session);
            return CreateResult.WORKSTATION_BUSY;
        }
        return CreateResult.CREATED;
    }

    public Optional<ActiveCookingSession> removeByPlayer(UUID playerId) {
        if (playerId == null) {
            return Optional.empty();
        }
        ActiveCookingSession removed = byPlayer.remove(playerId);
        if (removed != null) {
            byWorkstation.remove(removed.workstationKey(), removed);
        }
        return Optional.ofNullable(removed);
    }

    public Optional<ActiveCookingSession> removeByWorkstation(CookingLocationKey workstationKey) {
        if (workstationKey == null) {
            return Optional.empty();
        }
        ActiveCookingSession removed = byWorkstation.remove(workstationKey);
        if (removed != null) {
            byPlayer.remove(removed.playerId(), removed);
        }
        return Optional.ofNullable(removed);
    }

    public Collection<ActiveCookingSession> all() {
        return List.copyOf(byWorkstation.values());
    }

    public void clear() {
        byPlayer.clear();
        byWorkstation.clear();
    }

    public enum CreateResult {
        CREATED,
        PLAYER_BUSY,
        WORKSTATION_BUSY,
        INVALID
    }
}
