package me.nakilex.levelplugin.cursormenu;

import org.bukkit.Location;

import java.util.List;

/**
 * Represents one menu section/camera location. Sections are made up of ordered
 * {@link MenuLayout} entries which define the clickable areas displayed to the
 * player. A section may require a permission before a player can access it.
 */
public class Section {

    private final Location cameraLocation;
    private final String permission;
    private final List<MenuLayout> layouts;

    public Section(Location cameraLocation, String permission, List<MenuLayout> layouts) {
        this.cameraLocation = cameraLocation;
        this.permission = permission;
        this.layouts = List.copyOf(layouts);
    }

    public Location getCameraLocation() {
        return cameraLocation;
    }

    public String getPermission() {
        return permission;
    }

    public List<MenuLayout> getLayouts() {
        return layouts;
    }
}
