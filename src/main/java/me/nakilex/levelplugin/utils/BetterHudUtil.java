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

    /**
     * Toggles a specific BetterHud HUD for the target player.
     *
     * @param player target player
     * @param hudId BetterHud HUD id
     * @param enable true to add the HUD, false to remove
     */
    public static void setHud(Player player, String hudId, boolean enable) {
        if (player == null || hudId == null || hudId.isBlank()) {
            return;
        }
        String action = enable ? "add" : "remove";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "betterhud hud " + action + " " + player.getName() + " " + hudId);
    }

    public static void addHud(Player player) {
        setHud(player, true);
    }

    public static void removeHud(Player player) {
        setHud(player, false);
    }
}
