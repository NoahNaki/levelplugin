package me.nakilex.levelplugin.mob.utils;

/** Utility methods to convert between MythicMob internal IDs and human-friendly names. */
public final class MobNameUtil {
    private MobNameUtil() {}

    /**
     * Convert an internal mob ID like "KING_SLIME" into a pretty name like "King Slime".
     *
     * @param rawName MythicMob internal name
     * @return formatted display name
     */
    public static String toPrettyName(String rawName) {
        if (rawName == null || rawName.isEmpty()) return rawName;
        String[] parts = rawName.split("_");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].toLowerCase();
            if (!part.isEmpty()) {
                parts[i] = Character.toUpperCase(part.charAt(0)) + part.substring(1);
            }
        }
        return String.join(" ", parts);
    }

}
