package me.nakilex.levelplugin.advancement.persistence;

import me.nakilex.levelplugin.advancement.model.AdvancementKey;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeamProgression {
    private final UUID teamId;
    private final Map<AdvancementKey, Integer> progress = new ConcurrentHashMap<>();

    public TeamProgression(UUID teamId) { this.teamId = teamId; }
    public UUID getTeamId() { return teamId; }
    public int get(AdvancementKey key) { return progress.getOrDefault(key, 0); }
    public int put(AdvancementKey key, int value) { Integer old = progress.put(key, value); return old == null ? 0 : old; }
    public Map<AdvancementKey, Integer> snapshot() { return Map.copyOf(progress); }
}
