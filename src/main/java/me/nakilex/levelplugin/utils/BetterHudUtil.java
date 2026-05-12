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
        setHud(player, "all", enable);
    }

    /**
     * Toggles one named BetterHud HUD for a player.
     *
     * @param player target player
     * @param hudName BetterHud HUD id, or {@code all} for every HUD
     * @param enable true to add the HUD, false to remove it
     */
    public static void setHud(Player player, String hudName, boolean enable) {
        if (player == null) {
            return;
        }
        String action = enable ? "add" : "remove";
        String hud = hudName == null || hudName.isBlank() ? "all" : hudName.trim();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "hud hud " + action + " " + player.getName() + " " + hud);
    }

    public static void addHud(Player player) {
        setHud(player, true);
    }

    public static void removeHud(Player player) {
        setHud(player, false);
    }

    public static void addHud(Player player, String hudName) {
        setHud(player, hudName, true);
    }

    public static void removeHud(Player player, String hudName) {
        setHud(player, hudName, false);
    }
}
