package me.nakilex.levelplugin.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Convenience methods for interacting with the BetterHud plugin.
 */
public final class BetterHudUtil {
    private BetterHudUtil() {}

    /**
     * Toggles the player's HUD via BetterHud's commands.
     *
     * @param player target player
     * @param enable true to add the HUD, false to remove
     */
    public static void setHud(Player player, boolean enable) {
        String action = enable ? "add" : "remove";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "hud hud " + action + " " + player.getName() + " all");
    }

    public static void addHud(Player player) {
        setHud(player, true);
    }

    public static void removeHud(Player player) {
        setHud(player, false);
    }
}
