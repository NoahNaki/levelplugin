package me.nakilex.levelplugin.fishing.core.condition;

import me.nakilex.levelplugin.fishing.api.FishingContext;
import me.nakilex.levelplugin.fishing.api.condition.Condition;
import me.nakilex.levelplugin.fishing.core.FishingArgs;

import java.util.Map;

public class YRangeCondition implements Condition {
    @Override
    public boolean test(FishingContext ctx, Map<String, Object> args) {
        int min = FishingArgs.getInt(args, "min", Integer.MIN_VALUE);
        int max = FishingArgs.getInt(args, "max", Integer.MAX_VALUE);
        int y = ctx.getLocation().getBlockY();
        return y >= min && y <= max;
    }
}
