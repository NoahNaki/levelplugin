package me.nakilex.levelplugin.cursormenu.model;

import java.util.ArrayList;
import java.util.List;

public class MenuButton {
    private final String id;
    private final String text;
    private final double x;
    private final double y;
    private final double z;
    private final double scale;
    private final List<String> commands;
    private final int commandDelay;
    private final String permission;

    private final double tiltX;
    private final double tiltY;
    private final double tiltZ;

    private final String conditionVariable;
    private final String conditionOperator;
    private final String conditionValue;

    private final boolean nextMenuEnabled;
    private final String nextMenuKey;

    private final boolean stopMenuEnabled;
    private final boolean teleportEnabled;
    private final boolean teleportBackOriginal;
    private final String teleportWorld;
    private final Double teleportX;
    private final Double teleportY;
    private final Double teleportZ;

    private final List<String> randomCommands;
    private final List<Double> randomChances;

    private final boolean closeMenu;

    public MenuButton(String id,
                      String text,
                      double x,
                      double y,
                      double z,
                      double scale,
                      List<String> commands,
                      int commandDelay,
                      String permission,
                      double tiltX,
                      double tiltY,
                      double tiltZ,
                      String conditionVariable,
                      String conditionOperator,
                      String conditionValue,
                      boolean nextMenuEnabled,
                      String nextMenuKey,
                      boolean stopMenuEnabled,
                      boolean teleportEnabled,
                      boolean teleportBackOriginal,
                      String teleportWorld,
                      Double teleportX,
                      Double teleportY,
                      Double teleportZ,
                      List<String> randomCommands,
                      List<Double> randomChances,
                      boolean closeMenu) {
        this.id = id;
        this.text = text;
        this.x = x;
        this.y = y;
        this.z = z;
        this.scale = scale;
        this.commands = commands == null ? new ArrayList<>() : new ArrayList<>(commands);
        this.commandDelay = commandDelay;
        this.permission = permission;
        this.tiltX = tiltX;
        this.tiltY = tiltY;
        this.tiltZ = tiltZ;
        this.conditionVariable = conditionVariable;
        this.conditionOperator = conditionOperator;
        this.conditionValue = conditionValue;
        this.nextMenuEnabled = nextMenuEnabled;
        this.nextMenuKey = nextMenuKey;
        this.stopMenuEnabled = stopMenuEnabled;
        this.teleportEnabled = teleportEnabled;
        this.teleportBackOriginal = teleportBackOriginal;
        this.teleportWorld = teleportWorld;
        this.teleportX = teleportX;
        this.teleportY = teleportY;
        this.teleportZ = teleportZ;
        this.randomCommands = randomCommands == null ? new ArrayList<>() : new ArrayList<>(randomCommands);
        this.randomChances = randomChances == null ? new ArrayList<>() : new ArrayList<>(randomChances);
        this.closeMenu = closeMenu;
    }

    public String id() { return id; }
    public String text() { return text; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public double scale() { return scale; }
    public List<String> commands() { return commands; }
    public int commandDelay() { return commandDelay; }
    public String permission() { return permission; }
    public double tiltX() { return tiltX; }
    public double tiltY() { return tiltY; }
    public double tiltZ() { return tiltZ; }
    public String conditionVariable() { return conditionVariable; }
    public String conditionOperator() { return conditionOperator; }
    public String conditionValue() { return conditionValue; }
    public boolean nextMenuEnabled() { return nextMenuEnabled; }
    public String nextMenuKey() { return nextMenuKey; }
    public boolean stopMenuEnabled() { return stopMenuEnabled; }
    public boolean teleportEnabled() { return teleportEnabled; }
    public boolean teleportBackOriginal() { return teleportBackOriginal; }
    public String teleportWorld() { return teleportWorld; }
    public Double teleportX() { return teleportX; }
    public Double teleportY() { return teleportY; }
    public Double teleportZ() { return teleportZ; }
    public List<String> randomCommands() { return randomCommands; }
    public List<Double> randomChances() { return randomChances; }
    public boolean closeMenu() { return closeMenu; }
}
