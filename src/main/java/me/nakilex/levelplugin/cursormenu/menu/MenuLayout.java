package me.nakilex.levelplugin.cursormenu.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Represents a clickable option within a menu. Coordinates and other visual
 * information are intentionally omitted in this generic implementation; only
 * command execution and permission handling are provided.
 */
public class MenuLayout {
    private final String id;
    private final List<String> commands;
    private final String permission;

    public MenuLayout(String id, List<String> commands, String permission) {
        this.id = id;
        this.commands = commands == null ? Collections.emptyList() : commands;
        this.permission = permission;
    }

    public String getId() { return id; }
    public String getPermission() { return permission; }

    public void runCommands(Player player) {
        for (String cmd : commands) {
            Bukkit.dispatchCommand(player, cmd.replace("%player%", player.getName()));
        }
    }
}
