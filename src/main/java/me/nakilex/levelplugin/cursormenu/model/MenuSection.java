package me.nakilex.levelplugin.cursormenu.model;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class MenuSection {
    private final String key;
    private final Location camera;
    private final double distance;

    private final String permission;
    private final boolean autoCommandsEnabled;
    private final List<String> autoCommands;
    private final List<Integer> autoCommandDelays;

    private final List<MenuButton> buttons;

    public MenuSection(String key,
                       Location camera,
                       double distance,
                       String permission,
                       boolean autoCommandsEnabled,
                       List<String> autoCommands,
                       List<Integer> autoCommandDelays,
                       List<MenuButton> buttons) {
        this.key = key;
        this.camera = camera;
        this.distance = distance;
        this.permission = permission;
        this.autoCommandsEnabled = autoCommandsEnabled;
        this.autoCommands = autoCommands == null ? new ArrayList<>() : new ArrayList<>(autoCommands);
        this.autoCommandDelays = autoCommandDelays == null ? new ArrayList<>() : new ArrayList<>(autoCommandDelays);
        this.buttons = buttons == null ? new ArrayList<>() : new ArrayList<>(buttons);
    }

    public String key() { return key; }
    public Location camera() { return camera; }
    public double distance() { return distance; }
    public String permission() { return permission; }
    public boolean autoCommandsEnabled() { return autoCommandsEnabled; }
    public List<String> autoCommands() { return autoCommands; }
    public List<Integer> autoCommandDelays() { return autoCommandDelays; }
    public List<MenuButton> buttons() { return buttons; }
}
