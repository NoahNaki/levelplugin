package me.nakilex.levelplugin.fishing.api.action;

import me.nakilex.levelplugin.fishing.api.FishingContext;

import java.util.Map;

public interface Action {
    void execute(FishingContext ctx, Map<String, Object> args);
}
