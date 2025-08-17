package me.nakilex.levelplugin.mob.utils;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;

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

    /**
     * Look up the configured display name and strip any color codes.
     * Falls back to a prettified ID if necessary.
     *
     * @param mobId MythicMob internal ID
     * @return plain display name without color codes
     */
    public static String getPlainDisplayName(String mobId) {
        return ChatColor.stripColor(getDisplayName(mobId));
    }

    /**
     * Check whether a custom mob name contains a numeric health component such as
     * "35/50". Color codes are stripped before checking.
     *
     * @param customName the mob's custom name
     * @return {@code true} if the name includes a number slash number pattern
     */
    public static boolean hasNumericHealth(String customName) {
        if (customName == null) {
            return false;
        }
        String stripped = ChatColor.stripColor(customName);
        return stripped.matches(".*\\d+\\s*/\\s*\\d+.*");
    }

    /**
     * Check whether the given entity has a *visible* custom name containing
     * a numeric health component such as "35/50".
     *
     * @param entity the entity to inspect
     * @return {@code true} if the custom name is visible and contains numeric health
     */
    public static boolean hasNumericHealth(LivingEntity entity) {
        return entity != null
                && entity.isCustomNameVisible()
                && hasNumericHealth(entity.getCustomName());
    }

    /**
     * Build a formatted display name containing level, base mob name, and its
     * current and maximum health.
     *
     * @param level      mob level to show in the prefix
     * @param nameColor  color of the mob's base name
     * @param prettyType human-friendly mob name
     * @param currentHP  mob's current health
     * @param maxHP      mob's maximum health
     * @return formatted display name including numeric health
     */
    public static String buildHealthName(int level,
                                         ChatColor nameColor,
                                         String prettyType,
                                         double currentHP,
                                         double maxHP) {
        return ChatColor.GRAY + "[Lv " + level + "] "
                + nameColor + prettyType + " "
                + ChatColor.RED + (int) currentHP + "/" + (int) maxHP + " \u2764";
    }
}

