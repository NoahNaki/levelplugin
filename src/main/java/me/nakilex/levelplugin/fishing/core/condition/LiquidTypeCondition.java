package me.nakilex.levelplugin.fishing.core.condition;

import me.nakilex.levelplugin.fishing.api.FishingContext;
import me.nakilex.levelplugin.fishing.api.FishingMechanism;
import me.nakilex.levelplugin.fishing.api.condition.Condition;
import me.nakilex.levelplugin.fishing.core.FishingArgs;

import java.util.Map;

public class LiquidTypeCondition implements Condition {
    @Override
    public boolean test(FishingContext ctx, Map<String, Object> args) {
        String value = FishingArgs.getString(args, "value");
        if (value == null) {
            return false;
        }
        try {
            FishingMechanism mechanism = FishingMechanism.valueOf(value.trim().toUpperCase());
            return ctx.getMechanism() == mechanism;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
