package me.nakilex.levelplugin.utils;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;

/**
 * Simple helper to render a single TextDisplay above a living entity and keep
 * it updated as the entity moves. Useful for showing nametags or health bars
 * when the underlying entity is invisible (e.g. ModelEngine models).
 */
public class EntityTextDisplay {

    private final LivingEntity target;
    private final double yOffset;
    private TextDisplay display;

    /**
     * @param target   entity to follow
     * @param yOffset  extra vertical offset above the entity's height
     */
    public EntityTextDisplay(LivingEntity target, double yOffset) {
        this.target = target;
        this.yOffset = yOffset;
    }

    /**
     * Update the display's text and position. Spawns the TextDisplay if needed.
     */
    public void update(String text) {
        Location loc = target.getLocation().add(0, target.getHeight() + yOffset, 0);
        if (display == null || display.isDead()) {
            display = (TextDisplay) target.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
            display.setBillboard(Display.Billboard.CENTER);
            display.setShadowRadius(0f);
            display.setShadowStrength(0f);
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        } else {
            display.teleport(loc);
        }
        if (!text.equals(display.getText())) {
            display.setText(text);
        }
    }

    /** Remove the TextDisplay if present. */
    public void remove() {
        if (display != null && !display.isDead()) {
            display.remove();
        }
    }
}
