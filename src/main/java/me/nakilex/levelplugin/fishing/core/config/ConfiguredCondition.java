package me.nakilex.levelplugin.fishing.core.config;

import java.util.Collections;
import java.util.Map;

public record ConfiguredCondition(String type, Map<String, Object> args) {
    public ConfiguredCondition {
        args = args == null ? Collections.emptyMap() : Collections.unmodifiableMap(args);
    }
}
