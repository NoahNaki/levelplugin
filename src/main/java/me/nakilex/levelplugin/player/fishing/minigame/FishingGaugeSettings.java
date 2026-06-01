package me.nakilex.levelplugin.player.fishing.minigame;

import org.bukkit.configuration.ConfigurationSection;

/** Configurable characters or resource-pack glyph placeholders used to render fishing gauges. */
public record FishingGaugeSettings(int width, String empty, String target, String pointer) {
    public FishingGaugeSettings {
        width = Math.max(5, width);
        empty = normalize(empty, "-");
        target = normalize(target, "■");
        pointer = normalize(pointer, "|");
    }

    public static FishingGaugeSettings from(ConfigurationSection section) {
        return new FishingGaugeSettings(section == null ? 15 : section.getInt("width", 15),
                section == null ? "-" : section.getString("empty", "-"),
                section == null ? "■" : section.getString("target", "■"),
                section == null ? "|" : section.getString("pointer", "|"));
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
