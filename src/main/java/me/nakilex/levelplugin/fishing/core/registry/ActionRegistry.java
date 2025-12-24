package me.nakilex.levelplugin.fishing.core.registry;

import me.nakilex.levelplugin.fishing.api.action.Action;

import java.util.HashMap;
import java.util.Map;

public class ActionRegistry {
    private final Map<String, Action> actions = new HashMap<>();

    public void register(String key, Action action) {
        if (key == null || action == null) {
            return;
        }
        actions.put(key.toLowerCase(), action);
    }

    public Action get(String key) {
        if (key == null) {
            return null;
        }
        return actions.get(key.toLowerCase());
    }
}
