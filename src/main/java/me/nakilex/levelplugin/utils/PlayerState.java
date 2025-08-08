package me.nakilex.levelplugin.utils;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Snapshot of a player's basic state that can be restored later.
 * Extracted from the cutscene system so other features like cursor menus
 * can temporarily modify a player's mode and flight abilities without
 * duplicating capture/restore logic.
 */
public class PlayerState {
    private final GameMode mode;
    private final boolean allowFlight;
    private final boolean flying;
    private Location endLocation;

    private PlayerState(GameMode mode, boolean allowFlight, boolean flying) {
        this.mode = mode;
        this.allowFlight = allowFlight;
        this.flying = flying;
    }

    /** Capture the player's current state. */
    public static PlayerState capture(Player player) {
        return new PlayerState(player.getGameMode(), player.getAllowFlight(), player.isFlying());
    }

    /** Restore the captured state to the player. */
    public void restore(Player player) {
        player.setGameMode(mode);
        player.setAllowFlight(allowFlight);
        player.setFlying(flying);
    }

    public Location getEndLocation() {
        return endLocation;
    }

    public void setEndLocation(Location endLocation) {
        this.endLocation = endLocation;
    }
}

