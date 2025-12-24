package me.nakilex.levelplugin.fishing.core.condition;

import me.nakilex.levelplugin.fishing.api.FishingContext;
import me.nakilex.levelplugin.fishing.api.condition.Condition;
import me.nakilex.levelplugin.fishing.core.FishingArgs;

import java.util.Map;

public class TimeRangeCondition implements Condition {
    @Override
    public boolean test(FishingContext ctx, Map<String, Object> args) {
        int min = FishingArgs.getInt(args, "min", 0);
        int max = FishingArgs.getInt(args, "max", 24000);
        int time = (int) (ctx.getWorldTime() % 24000);
        if (min <= max) {
            return time >= min && time <= max;
        }
        return time >= min || time <= max;
    }
}
