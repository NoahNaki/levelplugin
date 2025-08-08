package me.nakilex.levelplugin.screen.menu;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a menu section containing the camera location and layouts.
 * The class is intentionally lightweight and reusable outside of this
 * plugin to encourage generic screen menu implementations.
 */
public class Section {
    private final String key;
    private final Location camera;
    private final float yaw;
    private final float pitch;
    private final String permission;
    private final List<MenuLayout> layouts = new ArrayList<>();

    public Section(String key, Location camera, float yaw, float pitch, String permission) {
        this.key = key;
        this.camera = camera;
        this.yaw = yaw;
        this.pitch = pitch;
        this.permission = permission;
    }

    public String getKey() {
        return key;
    }

    public Location getCamera() {
        return camera.clone();
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public String getPermission() {
        return permission;
    }

    public void addLayout(MenuLayout layout) {
        layouts.add(layout);
    }

    public List<MenuLayout> getLayouts() {
        return Collections.unmodifiableList(layouts);
    }

    /**
     * Returns the first layout the player has permission for or null.
     */
    public MenuLayout getFirstAllowed(Player player) {
        for (MenuLayout layout : layouts) {
            if (layout.hasPermission(player)) {
                return layout;
            }
        }
        return null;
    }
}
