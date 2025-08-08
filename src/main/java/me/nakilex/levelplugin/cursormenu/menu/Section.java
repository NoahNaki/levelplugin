package me.nakilex.levelplugin.cursormenu.menu;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds camera location and menu layouts for a section.
 */
public class Section {
    private final String key;
    private final Location camera;
    private final String permission;
    private final List<MenuLayout> layouts = new ArrayList<>();

    public Section(String key, Location camera, String permission) {
        this.key = key;
        this.camera = camera;
        this.permission = permission;
    }

    public String getKey() { return key; }
    public Location getCamera() { return camera; }
    public String getPermission() { return permission; }
    public List<MenuLayout> getLayouts() { return Collections.unmodifiableList(layouts); }

    public void addLayout(MenuLayout layout) {
        layouts.add(layout);
    }
}
