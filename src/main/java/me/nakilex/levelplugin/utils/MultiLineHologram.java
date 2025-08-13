package me.nakilex.levelplugin.utils;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple multi-line hologram using invisible ArmorStand entities.
 */
public class MultiLineHologram {
    private final Location location;
    private final List<ArmorStand> lines = new ArrayList<>();

    public MultiLineHologram(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    /** Remove existing hologram entities. */
    public void despawn() {
        for (ArmorStand as : lines) {
            if (as != null && !as.isDead()) {
                as.remove();
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
            ArmorStand stand = lineLoc.getWorld().spawn(lineLoc, ArmorStand.class, as -> {
                as.setVisible(false);
                as.setGravity(false);
                as.setMarker(true);
                as.setCustomNameVisible(true);
                as.setCustomName(text);
            });
            lines.add(stand);
            offset -= 0.3; // stack downward
        }
    }

    /** Update lines without respawning if count matches, otherwise respawn. */
    public void setLines(List<String> textLines) {
        if (lines.size() != textLines.size()) {
            spawn(textLines);
            return;
        }
        for (int i = 0; i < lines.size(); i++) {
            lines.get(i).setCustomName(textLines.get(i));
        }
    }
}
