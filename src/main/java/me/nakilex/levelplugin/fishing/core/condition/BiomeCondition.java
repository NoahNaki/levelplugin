package me.nakilex.levelplugin.fishing.core.condition;

import me.nakilex.levelplugin.fishing.api.FishingContext;
import me.nakilex.levelplugin.fishing.api.condition.Condition;
import me.nakilex.levelplugin.fishing.core.FishingArgs;

import java.util.List;
import java.util.Map;

public class BiomeCondition implements Condition {
    @Override
    public boolean test(FishingContext ctx, Map<String, Object> args) {
        List<String> biomes = FishingArgs.getStringList(args, "value");
        if (biomes.isEmpty()) {
            return false;
        }
        String biomeName = ctx.getBiome().name();
        return biomes.stream().anyMatch(value -> value.equalsIgnoreCase(biomeName));
    }
}
