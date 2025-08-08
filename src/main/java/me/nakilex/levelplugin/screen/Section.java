package me.nakilex.levelplugin.screen;

import org.bukkit.Location;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents one screen section. A section holds a camera location and
 * ordered {@link MenuLayout} entries that will be projected for a player.
 */
public class Section {

    private final Location cameraLocation;
    private final String permission;
    private final List<MenuLayout> layouts = new ArrayList<>();

    public Section(Location cameraLocation, String permission) {
        this.cameraLocation = cameraLocation;
        this.permission = permission;
    }

    public Location getCameraLocation() {
        return cameraLocation.clone();
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

