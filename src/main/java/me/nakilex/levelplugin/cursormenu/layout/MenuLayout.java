package me.nakilex.levelplugin.cursormenu.layout;

import me.nakilex.levelplugin.cursormenu.util.ColorParser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents one clickable button within a cursor menu.
 */
public class MenuLayout {
    private final Location location;
    private final List<String> commands = new ArrayList<>();
    private final String permission;

    public MenuLayout(Location location, String permission) {
        this.location = location;
        this.permission = permission;
    }

    public Location getLocation() {
        return location;
    }

    public String getPermission() {
        return permission;
    }

    public List<String> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    public void addCommand(String command) {
        commands.add(command);
    }

    /**
     * Execute all commands associated with this layout for the given player.
     */
    public void runCommands(Player player) {
        for (String cmd : commands) {
            String parsed = ColorParser.parse(cmd).replace("%player%", player.getName());
            Bukkit.dispatchCommand(player, parsed);
        }
    }
}
