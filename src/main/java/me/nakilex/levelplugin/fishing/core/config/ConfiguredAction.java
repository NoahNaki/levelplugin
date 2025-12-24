package me.nakilex.levelplugin.fishing.core.config;

import java.util.Collections;
import java.util.Map;

public record ConfiguredAction(String type, Map<String, Object> args) {
    public ConfiguredAction {
        args = args == null ? Collections.emptyMap() : Collections.unmodifiableMap(args);
    }
}
