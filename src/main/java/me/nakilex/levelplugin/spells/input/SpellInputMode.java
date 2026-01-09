package me.nakilex.levelplugin.spells.input;

public enum SpellInputMode {
    MOUSE_COMBO("Mouse Combo Clicks"),
    MOUSE_AND_KEYBOARD("Mouse + Keyboard");

    private final String displayName;

    SpellInputMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public SpellInputMode next() {
        return switch (this) {
            case MOUSE_COMBO -> MOUSE_AND_KEYBOARD;
            case MOUSE_AND_KEYBOARD -> MOUSE_COMBO;
        };
    }
}
