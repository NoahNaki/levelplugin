package me.nakilex.levelplugin.fishing.core.condition;

import me.nakilex.levelplugin.fishing.api.FishingContext;
import me.nakilex.levelplugin.fishing.api.condition.Condition;
import me.nakilex.levelplugin.fishing.core.FishingArgs;

import java.util.Map;

public class WeatherCondition implements Condition {
    @Override
    public boolean test(FishingContext ctx, Map<String, Object> args) {
        String value = FishingArgs.getString(args, "value");
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "CLEAR" -> !ctx.isRaining();
            case "RAIN" -> ctx.isRaining();
            case "STORM", "THUNDER" -> ctx.isThundering();
            default -> false;
        };
    }
}
