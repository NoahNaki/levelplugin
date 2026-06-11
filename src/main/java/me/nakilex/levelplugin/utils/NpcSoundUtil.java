package me.nakilex.levelplugin.utils;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.settings.managers.SettingsManager;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Player-scoped NPC sound helpers that honor each player's /settings preference.
 */
public final class NpcSoundUtil {
    private NpcSoundUtil() {
    }

    public static boolean canHearNpcSound(Player player) {
        if (player == null) {
            return false;
        }
        Main plugin = Main.getInstance();
        if (plugin == null) {
            return true;
        }
        SettingsManager settingsManager = plugin.getSettingsManager();
        return settingsManager == null || settingsManager.getSettings(player).isNpcSoundEffectsEnabled();
    }

    public static void play(Player player, Sound sound, float volume, float pitch) {
        if (player == null || sound == null || !canHearNpcSound(player)) {
            return;
        }
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public static void play(Player player, Location location, Sound sound, float volume, float pitch) {
        if (player == null || location == null || sound == null || !canHearNpcSound(player)) {
            return;
        }
        player.playSound(location, sound, volume, pitch);
    }
}
