package me.nakilex.levelplugin.fishing.core.loot;

import me.nakilex.levelplugin.fishing.core.config.ConfiguredAction;
import me.nakilex.levelplugin.fishing.core.config.ConfiguredCondition;

import java.util.List;

public record LootEntry(String id,
                        double weight,
                        Double minSize,
                        Double maxSize,
                        List<ConfiguredCondition> conditions,
                        List<ConfiguredAction> actions) {
}
