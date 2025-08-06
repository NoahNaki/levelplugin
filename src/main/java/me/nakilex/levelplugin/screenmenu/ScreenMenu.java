package me.nakilex.levelplugin.screenmenu;

import java.util.List;

/**
 * Simple screen menu definition loaded from configuration.
 */
public class ScreenMenu {
    private final double distance;
    private final List<ScreenMenuEntry> entries;

    public ScreenMenu(double distance, List<ScreenMenuEntry> entries) {
        this.distance = distance;
        this.entries = entries;
    }

    public double getDistance() {
        return distance;
    }

    public List<ScreenMenuEntry> getEntries() {
        return entries;
    }
}
