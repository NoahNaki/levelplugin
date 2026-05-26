package me.nakilex.levelplugin.advancement.persistence;

import me.nakilex.levelplugin.advancement.model.AdvancementKey;

import java.util.UUID;

public interface AdvancementDatabase {
    TeamProgression loadTeam(UUID teamId);
    int updateProgression(AdvancementKey key, TeamProgression team, int newProgress);
}
