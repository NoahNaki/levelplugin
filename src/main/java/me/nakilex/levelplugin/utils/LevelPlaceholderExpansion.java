package me.nakilex.levelplugin.utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.settings.managers.SettingsManager;
import me.nakilex.levelplugin.spells.input.SpellInputDisplayManager;
import me.nakilex.levelplugin.spells.input.SpellInputMode;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI expansion for spell combo HUD placeholders.
 * Identifier: level
 */
public class LevelPlaceholderExpansion extends PlaceholderExpansion {
    private final Main plugin;
    private final SettingsManager settingsManager;
    private final SpellInputDisplayManager displayManager = SpellInputDisplayManager.getInstance();

    public LevelPlaceholderExpansion(Main plugin) {
        this.plugin = plugin;
        this.settingsManager = plugin.getSettingsManager();
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
        if (!isMouseComboEnabled(player)) {
            return "";
        }
        String key = params.toLowerCase();
        return switch (key) {
            case "spell_combo_active" -> displayManager.isComboActive(player) ? "1" : "0";
            case "spell_combo_glyphs" -> displayManager.getComboGlyphs(player);
            case "spell_combo_slot1" -> displayManager.getComboSlot(player, 0);
            case "spell_combo_slot2" -> displayManager.getComboSlot(player, 1);
            case "spell_combo_slot3" -> displayManager.getComboSlot(player, 2);
            default -> null;
        };
    }

    private boolean isMouseComboEnabled(Player player) {
        if (settingsManager == null) {
            return false;
        }
        return settingsManager.getSettings(player).getSpellInputMode() == SpellInputMode.MOUSE_COMBO;
    }
}
