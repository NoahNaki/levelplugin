package me.nakilex.levelplugin.fishing.core.game;

import me.nakilex.levelplugin.fishing.core.config.ConfiguredCondition;

import java.util.List;

public record GameDefinition(String id,
                             String type,
                             int durationTicks,
                             double windowMin,
                             double windowMax,
                             List<ConfiguredCondition> conditions) {
}
