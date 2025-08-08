package me.nakilex.levelplugin.cursormenu;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Represents a clickable layout option for a section menu. Layouts are
 * typically defined in a configuration file with a list of commands to be run
 * when the player selects the option.
 */
public class MenuLayout {

    private final double x;
    private final double y;
    private final double z;
    private final List<String> commands;
    private final boolean stop;
    private final Location teleportLocation;
    private final String permission;

    public MenuLayout(double x, double y, double z, List<String> commands,
                      boolean stop, Location teleportLocation, String permission) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.commands = List.copyOf(commands);
        this.stop = stop;
        this.teleportLocation = teleportLocation;
        this.permission = permission;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public List<String> getCommands() { return commands; }
    public boolean shouldStop() { return stop; }
    public Location getTeleportLocation() { return teleportLocation; }
    public String getPermission() { return permission; }

    /**
     * Execute the configured commands for this layout.
     *
     * @param player player executing the commands
     */
    public void runCommand(Player player) {
        for (String command : commands) {
            String parsed = command.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
    }

    /**
     * Teleport the player if a teleport location has been set.
     *
     * @param player target player
     */
    public void teleport(Player player) {
        if (teleportLocation != null) {
            player.teleport(teleportLocation);
        }
    }

    /**
     * Run any stop commands when the menu is closed. This default
     * implementation simply delegates to {@link #runCommand(Player)}
     * but can be overridden in subclasses for specific behaviour.
     */
    public void runStopCommand(Player player) {
        runCommand(player);
    }

    /**
     * Check if a player has permission to click this option.
     */
    public boolean hasPermission(Player player) {
        return permission == null || permission.isEmpty() || player.hasPermission(permission);
    }
}
