package me.nakilex.levelplugin.mob.utils;

import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.MythicMob;

import java.util.Optional;

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

    /**
     * Look up the configured display name for a MythicMob ID. Falls back to a prettified
     * version of the ID if the mob or its display name cannot be found.
     *
     * @param mobId MythicMob internal ID
     * @return the configured display name or a prettified ID
     */
    public static String getDisplayName(String mobId) {
        if (mobId == null || mobId.isEmpty()) {
            return mobId;
        }
        Optional<MythicMob> opt = MythicBukkit.inst().getMobManager().getMythicMob(mobId);
        if (opt.isPresent()) {
            PlaceholderString name = opt.get().getDisplayName();
            if (name != null) {
                String result = name.get();
                if (result != null && !result.isEmpty()) {
                    return result;
                }
            }
        }
        return toPrettyName(mobId);
    }
}

