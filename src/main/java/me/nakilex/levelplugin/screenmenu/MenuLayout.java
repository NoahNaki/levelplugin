package me.nakilex.levelplugin.screenmenu;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.List;

/**
 * Represents a single clickable entry within a menu section.
 * The structure mirrors the original CustomScreenMenu plugin
 * but is trimmed to the essentials required here.
 */
public class MenuLayout {

    private final String key;
    private final String name;
    private final List<String> commands;
    private final boolean stop;
    private final double x;
    private final double y;
    private final double z;
    private final boolean teleport;
    private final boolean teleportBack;
    private final Location teleportLoc;
    private final List<String> stopCommands;
    private final String permission;

    public MenuLayout(String key,
                      String name,
                      List<String> commands,
                      boolean stop,
                      double x,
                      double y,
                      double z,
                      boolean teleport,
                      boolean teleportBack,
                      Location teleportLoc,
                      List<String> stopCommands,
                      String permission) {
        this.key = key;
        this.name = name;
        this.commands = commands;
        this.stop = stop;
        this.x = x;
        this.y = y;
        this.z = z;
        this.teleport = teleport;
        this.teleportBack = teleportBack;
        this.teleportLoc = teleportLoc;
        this.stopCommands = stopCommands;
        this.permission = permission;
    }

    public String key() { return key; }
    public String name() { return name; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }

    /** Executes the configured commands and optional teleport logic. */
    public void execute(Player player, ScreenMenuManager manager) {
        if (!permission.isEmpty() && !player.hasPermission(permission) && !player.isOp()) {
            player.sendMessage(ChatColor.RED + "You lack permission to use this.");
            return;
        }

        for (String cmd : commands) {
            cmd = cmd.replace("%player%", player.getName());
            cmd = PlaceholderAPI.setPlaceholders(player, cmd);
            if (cmd.toLowerCase().startsWith("[console]")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replaceFirst("\\[console\\]", "").trim());
            } else if (cmd.toLowerCase().startsWith("[op]")) {
                cmd = cmd.replaceFirst("\\[op\\]", "").trim();
                boolean wasOp = player.isOp();
                try {
                    player.setOp(true);
                    player.performCommand(cmd);
                } finally {
                    player.setOp(wasOp);
                }
            } else {
                if (cmd.toLowerCase().startsWith("[player]")) {
                    cmd = cmd.replaceFirst("\\[player\\]", "").trim();
                }
                player.performCommand(cmd);
            }
        }

        if (stop) {
            manager.hideMenu(player);
            if (teleport && teleportLoc != null) {
                Location target = teleportBack ? player.getWorld().getSpawnLocation() : teleportLoc;
                player.teleport(target);
            }
            if (stopCommands != null) {
                for (String cmd : stopCommands) {
                    cmd = cmd.replace("%player%", player.getName());
                    cmd = PlaceholderAPI.setPlaceholders(player, cmd);
                    if (cmd.toLowerCase().startsWith("[console]")) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replaceFirst("\\[console\\]", "").trim());
                    } else {
                        if (cmd.toLowerCase().startsWith("[player]")) {
                            cmd = cmd.replaceFirst("\\[player\\]", "").trim();
                        }
                        player.performCommand(cmd);
                    }
                }
            }
        }
    }
}
