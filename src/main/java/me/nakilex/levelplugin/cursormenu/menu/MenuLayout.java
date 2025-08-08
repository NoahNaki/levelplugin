package me.nakilex.levelplugin.cursormenu.menu;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single clickable option within a menu.
 */
public class MenuLayout {
    private final String id;
    private final double x;
    private final double y;
    private final double z;
    private final List<String> commands;
    private final Location teleport;
    private final boolean stopCursor;
    private final String permission;

    public MenuLayout(String id, double x, double y, double z, List<String> commands,
                      Location teleport, boolean stopCursor, String permission) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.commands = commands == null ? new ArrayList<>() : new ArrayList<>(commands);
        this.teleport = teleport;
        this.stopCursor = stopCursor;
        this.permission = permission;
    }

    public String getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public boolean isStopCursor() { return stopCursor; }
    public Location getTeleport() { return teleport; }
    public String getPermission() { return permission; }

    /**
     * Run configured commands for the player.
     */
    public void runCommands(Player player) {
        for (String cmd : commands) {
            Bukkit.dispatchCommand(player, cmd.replace("{player}", player.getName()));
        }
    }
}
