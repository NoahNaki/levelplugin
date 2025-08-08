package me.nakilex.levelplugin.cursormenu.layout;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A section represents a camera location and a list of menu layouts that
 * should be shown from that vantage point.
 */
public class Section {
    private final String key;
    private final Location cameraLocation;
    private final String permission;
    private final List<MenuLayout> layouts = new ArrayList<>();

    public Section(String key, Location cameraLocation, String permission) {
        this.key = key;
        this.cameraLocation = cameraLocation;
        this.permission = permission;
    }

    public String getKey() {
        return key;
    }

    public Location getCameraLocation() {
        return cameraLocation;
    }

    public String getPermission() {
        return permission;
    }

    public List<MenuLayout> getLayouts() {
        return Collections.unmodifiableList(layouts);
    }

    public void addLayout(MenuLayout layout) {
        layouts.add(layout);
    }
}
