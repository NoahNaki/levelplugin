package me.nakilex.levelplugin.cursormenu.menu;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Defines a menu section with camera information and ordered layouts.
 */
public class Section {
    private final Location camera;
    private final float yaw;
    private final float pitch;
    private final String permission;
    private final List<MenuLayout> layouts;

    public Section(Location camera, float yaw, float pitch, String permission, List<MenuLayout> layouts) {
        this.camera = camera;
        this.yaw = yaw;
        this.pitch = pitch;
        this.permission = permission;
        this.layouts = layouts == null ? new ArrayList<>() : new ArrayList<>(layouts);
    }

    public Location getCamera() { return camera; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public String getPermission() { return permission; }
    public List<MenuLayout> getLayouts() { return Collections.unmodifiableList(layouts); }
}
