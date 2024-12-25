package me.nakilex.levelplugin.storage.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class StoragePermissions {

    // Check if a player has permission to create storage
    public static boolean canCreateStorage(Player player) {
        return player.hasPermission("levelplugin.storage.create");
    }

    // Check if a player has permission to open storage
    public static boolean canOpenStorage(Player player) {
        return player.hasPermission("levelplugin.storage.open");
    }

    // Check if a player has admin permissions for storage
    public static boolean isAdmin(Player player) {
        return player.hasPermission("levelplugin.storage.admin");
    }

    // Send a no-permission message to the player
    public static void sendNoPermissionMessage(Player player) {
        player.sendMessage("You do not have permission to perform this action.");
    }
}
