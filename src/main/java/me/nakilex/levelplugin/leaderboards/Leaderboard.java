package me.nakilex.levelplugin.leaderboards;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single holographic leaderboard display.
 */
public class Leaderboard {
    private final String id;
    private final Location location;
    private final LeaderboardType type;
    private final List<TextDisplay> lines = new ArrayList<>();

    public Leaderboard(String id, Location location, LeaderboardType type) {
        this.id = id;
        this.location = location;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public LeaderboardType getType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    /** Remove existing hologram entities. */
    public void despawn() {
        for (TextDisplay td : lines) {
            if (td != null && !td.isDead()) {
                td.remove();
            }
        }
        lines.clear();
    }

    /** Spawn hologram lines with the given text. */
    public void spawn(List<String> textLines) {
        despawn();
        Location base = location.clone();
        double offset = 0.0;
        for (String text : textLines) {
            Location lineLoc = base.clone().add(0, offset, 0);
            TextDisplay disp = (TextDisplay) base.getWorld().spawnEntity(lineLoc, EntityType.TEXT_DISPLAY);
            disp.setBillboard(Display.Billboard.CENTER);
            disp.setShadowRadius(0f);
            disp.setShadowStrength(0f);
            disp.setText(text);
            lines.add(disp);
            offset -= 0.25; // stack downward
        }
    }
}
