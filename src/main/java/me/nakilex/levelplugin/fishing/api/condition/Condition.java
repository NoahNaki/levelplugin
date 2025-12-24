package me.nakilex.levelplugin.fishing.api.condition;

import me.nakilex.levelplugin.fishing.api.FishingContext;

import java.util.Map;

public interface Condition {
    boolean test(FishingContext ctx, Map<String, Object> args);
}
