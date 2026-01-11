package me.nakilex.levelplugin.hud.placeholders;

import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class HudPlaceholderRegistry {
    private final Map<String, Function<Player, String>> placeholders = new HashMap<>();

    public HudPlaceholderRegistry() {
    }

    public void register(String key, Function<Player, String> resolver) {
        if (key == null || resolver == null) {
            return;
        }
        placeholders.put(key.toLowerCase(), resolver);
    }

    public Map<String, Function<Player, String>> getPlaceholders() {
        return Collections.unmodifiableMap(placeholders);
    }
}
