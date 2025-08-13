package me.nakilex.levelplugin.utils;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple multi-line hologram using TextDisplay entities.
 */
public class MultiLineHologram {
    private final Location location;
    private final List<TextDisplay> lines = new ArrayList<>();

    public MultiLineHologram(Location location) {
        this.location = location;
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

    /** Update lines without respawning if count matches, otherwise respawn. */
    public void setLines(List<String> textLines) {
        if (lines.size() != textLines.size()) {
            spawn(textLines);
            return;
        }
        for (int i = 0; i < lines.size(); i++) {
            lines.get(i).setText(textLines.get(i));
        }
    }
}
