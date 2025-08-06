package me.nakilex.levelplugin.screenmenu;

/**
 * Represents a single line of text in a screen menu along with its
 * relative position and optional command.
 */
public record ScreenMenuEntry(String text, double x, double y, String command) {}
