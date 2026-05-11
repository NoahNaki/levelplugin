package me.nakilex.levelplugin.spells.input;

import me.nakilex.levelplugin.settings.data.PlayerSettings;
import me.nakilex.levelplugin.settings.managers.SettingsManager;
import me.nakilex.levelplugin.utils.BetterHudUtil;
import org.bukkit.entity.Player;

/**
 * Synchronizes the BetterHud mouse-click combo HUD with the player's spell input setting.
 */
public final class SpellInputHudManager {
    private static final String MOUSE_CLICK_HUD_ID = "test_hud";

    private SpellInputHudManager() {
    }

    public static void sync(Player player, SettingsManager settingsManager) {
        if (player == null || settingsManager == null) {
            return;
        }
        PlayerSettings settings = settingsManager.getSettings(player);
        sync(player, settings == null ? null : settings.getSpellInputMode());
    }

    public static void sync(Player player, SpellInputMode mode) {
        if (player == null || mode == null) {
            return;
        }
        BetterHudUtil.setHud(player, MOUSE_CLICK_HUD_ID, mode == SpellInputMode.MOUSE_COMBO);
    }

    public static void remove(Player player) {
        BetterHudUtil.setHud(player, MOUSE_CLICK_HUD_ID, false);
    }
}
