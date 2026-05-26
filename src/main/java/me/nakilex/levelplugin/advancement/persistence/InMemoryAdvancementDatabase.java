package me.nakilex.levelplugin.advancement.persistence;

import me.nakilex.levelplugin.advancement.model.AdvancementKey;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAdvancementDatabase implements AdvancementDatabase {
    private final Map<UUID, TeamProgression> teams = new ConcurrentHashMap<>();

    @Override
    public TeamProgression loadTeam(UUID teamId) {
        return teams.computeIfAbsent(teamId, TeamProgression::new);
    }

    @Override
    public int updateProgression(AdvancementKey key, TeamProgression team, int newProgress) {
        return team.put(key, Math.max(0, newProgress));
    }
}
