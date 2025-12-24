package me.nakilex.levelplugin.fishing.core.registry;

import me.nakilex.levelplugin.fishing.api.condition.Condition;

import java.util.HashMap;
import java.util.Map;

public class ConditionRegistry {
    private final Map<String, Condition> conditions = new HashMap<>();

    public void register(String key, Condition condition) {
        if (key == null || condition == null) {
            return;
        }
        conditions.put(key.toLowerCase(), condition);
    }

    public Condition get(String key) {
        if (key == null) {
            return null;
        }
        return conditions.get(key.toLowerCase());
    }
}
