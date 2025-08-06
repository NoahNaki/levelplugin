package me.nakilex.levelplugin.screenmenu;

import java.util.LinkedHashMap;

/**
 * A menu section groups a set of MenuLayouts and defines camera
 * placement parameters. Closely mirrors the original plugin's
 * representation but only keeps the fields used by our manager.
 */
public class Section {
    public final double distance;
    public final String world;
    public final double cameraX;
    public final double cameraY;
    public final double cameraZ;
    public final float yaw;
    public final float pitch;
    public final String permission;
    public final LinkedHashMap<String, MenuLayout> layouts = new LinkedHashMap<>();

    public Section(double distance,
                   String world,
                   double cameraX,
                   double cameraY,
                   double cameraZ,
                   float yaw,
                   float pitch,
                   String permission) {
        this.distance = distance;
        this.world = world;
        this.cameraX = cameraX;
        this.cameraY = cameraY;
        this.cameraZ = cameraZ;
        this.yaw = yaw;
        this.pitch = pitch;
        this.permission = permission;
    }

    public void add(String key, MenuLayout layout) {
        layouts.put(key, layout);
    }
}
