package me.nakilex.levelplugin.player.woodcutting.drop;

public enum DropMode {
    LOCAL,
    ORIGIN,
    INVENTORY,
    TURN_INTO_BLOCKS;

    public static DropMode fromString(String raw) {
        if (raw == null) return LOCAL;
        try {
            return DropMode.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return LOCAL;
        }
    }
}
