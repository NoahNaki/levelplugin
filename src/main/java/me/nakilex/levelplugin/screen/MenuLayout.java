package me.nakilex.levelplugin.screen;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single clickable option on the cursor menu.
 */
public class MenuLayout {

    private final Vector position;
    private final List<String> commands = new ArrayList<>();
    private boolean stopCursor;
    private Location teleportLocation;

    public MenuLayout(Vector position) {
        this.position = position;
    }

    public Vector getPosition() {
        return position.clone();
    }

    public List<String> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    public MenuLayout addCommand(String command) {
        commands.add(command);
        return this;
    }

    public boolean isStopCursor() {
        return stopCursor;
    }

    public MenuLayout setStopCursor(boolean stopCursor) {
        this.stopCursor = stopCursor;
        return this;
    }

    public Location getTeleportLocation() {
        return teleportLocation;
    }

    public MenuLayout setTeleportLocation(Location teleportLocation) {
        this.teleportLocation = teleportLocation;
        return this;
    }

    /**
     * Dispatch all configured commands for the player. Supports the special
     * placeholder "<player>" which will be replaced with the player name.
     */
    public void runCommands(Player player) {
        for (String cmd : commands) {
            String parsed = cmd.replace("<player>", player.getName());
            if (parsed.startsWith("console:")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed.substring(8));
            } else if (parsed.startsWith("op:")) {
                boolean op = player.isOp();
                try {
                    player.setOp(true);
                    player.performCommand(parsed.substring(3));
                } finally {
                    player.setOp(op);
                }
            } else {
                player.performCommand(parsed);
            }
        }
    }

    /**
     * Teleport the player if a location is defined.
     */
    public void teleport(Player player) {
        if (teleportLocation != null) {
            player.teleport(teleportLocation);
        }
    }
}
