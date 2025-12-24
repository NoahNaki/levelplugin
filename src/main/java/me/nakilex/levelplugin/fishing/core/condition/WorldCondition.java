package me.nakilex.levelplugin.fishing.core.condition;

import me.nakilex.levelplugin.fishing.api.FishingContext;
import me.nakilex.levelplugin.fishing.api.condition.Condition;
import me.nakilex.levelplugin.fishing.core.FishingArgs;

import java.util.List;
import java.util.Map;

public class WorldCondition implements Condition {
    @Override
    public boolean test(FishingContext ctx, Map<String, Object> args) {
        List<String> worlds = FishingArgs.getStringList(args, "value");
        if (worlds.isEmpty()) {
            return false;
        }
        String worldName = ctx.getWorld().getName();
        return worlds.stream().anyMatch(value -> value.equalsIgnoreCase(worldName));
    }
}
