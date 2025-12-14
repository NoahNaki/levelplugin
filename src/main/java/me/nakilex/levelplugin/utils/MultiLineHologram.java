package me.nakilex.levelplugin.utils;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Interaction;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple multi-line hologram using lightweight TextDisplay entities.
 */
public class MultiLineHologram {
    private final Location location;
    private final List<TextDisplay> lines = new ArrayList<>();
    private final String tag;

    public MultiLineHologram(Location location) {
        this(location, null);
    }

    public MultiLineHologram(Location location, String tag) {
        this.location = location;
        this.tag = tag;
    }

    public Location getLocation() {
        return location;
    }

    public List<TextDisplay> getDisplays() {
        return Collections.unmodifiableList(lines);
    }

    /** Remove existing hologram entities. */
    public void despawn() {
        for (TextDisplay as : lines) {
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
            TextDisplay display = (TextDisplay) lineLoc.getWorld().spawnEntity(lineLoc, EntityType.TEXT_DISPLAY);
            display.setBillboard(Display.Billboard.CENTER);
            display.setShadowRadius(0f);
            display.setShadowStrength(0f);
            display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            display.setText(text);
            if (tag != null && !tag.isEmpty()) {
                display.addScoreboardTag(tag);
            }
            lines.add(display);
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
            lines.get(i).setText(textLines.get(i));
        }
    }

    /**
     * Remove any armor stands with the given tag within a radius of the
     * location.
     */
    public static void removeAll(Location base, double radius, String tag) {
        if (base == null || base.getWorld() == null) return;
        for (Entity e : base.getWorld().getNearbyEntities(base, radius, radius, radius)) {
            if ((e instanceof TextDisplay || e instanceof ArmorStand) && (tag == null || e.getScoreboardTags().contains(tag))) {
                e.remove();
            }
        }
    }
    /**
     * Remove all entities with scoreboard tags starting with the given prefix across all worlds.
     */
    public static void removeAll(String tagPrefix) {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity e : world.getEntities()) {
                if (e instanceof Display || e instanceof ArmorStand || e instanceof Interaction) {
                    for (String t : e.getScoreboardTags()) {
                        if (t.startsWith(tagPrefix)) {
                            e.remove();
                            break;
                        }
                    }
                }
            }
        }
    }
}
