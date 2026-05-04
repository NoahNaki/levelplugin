package me.nakilex.levelplugin.utils;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import me.nakilex.levelplugin.utils.MultiLineHologram;

/**
 * Simple helper to render a single TextDisplay above a living entity and keep
 * it updated as the entity moves. Useful for showing nametags or health bars
 * when the underlying entity is invisible (e.g. ModelEngine models).
 */
public class EntityTextDisplay {

    public static final String DISPLAY_TAG = "lp_entity_display";

    private final JavaPlugin plugin;
    private final Entity target;
    private final double yOffset;
    private TextDisplay display;
    private BukkitTask followTask;

    /**
     * @param plugin  owning plugin for scheduling follow task
     * @param target  entity to follow
     * @param yOffset extra vertical offset above the entity's height
     */
    public EntityTextDisplay(JavaPlugin plugin, Entity target, double yOffset) {
        this.plugin = plugin;
        this.target = target;
        this.yOffset = yOffset;
    }

    /**
     * Update the display's text. Spawns and starts a follow task if needed.
     */
    public void update(String text) {
        if (display == null || display.isDead()) {
            Location loc = target.getLocation().add(0, target.getHeight() + yOffset, 0);
            display = (TextDisplay) target.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
            display.setBillboard(Display.Billboard.CENTER);
            display.setViewRange(20.0f);
            display.setShadowRadius(0f);
            display.setShadowStrength(0f);
            display.setBackgroundColor(Color.fromARGB(180, 0, 0, 0));
            display.setDefaultBackground(false);
            display.setTeleportDuration(1); // interpolate for smoothness
            display.addScoreboardTag(DISPLAY_TAG);
            startFollowTask();
        }
        if (!text.equals(display.getText())) {
            display.setText(text);
        }
    }

    /** Begin repeating task to keep the display positioned every tick. */
    private void startFollowTask() {
        if (followTask != null) return;
        followTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (display == null || display.isDead() || target.isDead()) {
                remove();
                return;
            }
            Location loc = target.getLocation().add(0, target.getHeight() + yOffset, 0);
            display.teleport(loc);
        }, 1L, 1L);
    }

    /** Remove the TextDisplay and stop follow task if present. */
    public void remove() {
        if (followTask != null) {
            followTask.cancel();
            followTask = null;
        }
        if (display != null && !display.isDead()) {
            display.remove();
        }
        display = null;
    }

    public Entity displayEntity() {
        return display;
    }

    public void setDisplayMetadata(String key, Object value) {
        if (display == null || display.isDead() || key == null || key.isBlank()) {
            return;
        }
        display.setMetadata(key, new FixedMetadataValue(plugin, value));
    }

    /** Remove any lingering displays spawned by this helper. */
    public static void removeAllDisplays() {
        MultiLineHologram.removeAll(DISPLAY_TAG);
    }
}
