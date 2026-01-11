package me.nakilex.levelplugin.utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.input.SpellInputDisplayManager;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI expansion for spell combo HUD placeholders.
 * Identifier: level
 */
public class LevelPlaceholderExpansion extends PlaceholderExpansion {
    private final Main plugin;
    private final SpellInputDisplayManager displayManager = SpellInputDisplayManager.getInstance();

    public LevelPlaceholderExpansion(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "level";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null || params == null) {
            return "";
        }
        String key = params.toLowerCase();
        return switch (key) {
            case "spell_combo_active" -> displayManager.isComboActive(player) ? "1" : "0";
            case "spell_combo_glyphs" -> "\uE001\uE002\uE001";
            default -> null;
        };
    }
}
