package me.nakilex.levelplugin.mob.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.MetadataValue;
import me.nakilex.levelplugin.mob.custom.CustomMobManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
        return toPrettyName(mobId);
    }

    /**
     * Resolve a custom mob id from metadata, if one is present.
     */
    public static Optional<String> resolveCustomMobId(LivingEntity entity) {
        if (entity == null || !entity.hasMetadata(CustomMobManager.CUSTOM_MOB_ID_META)) {
            return Optional.empty();
        }
        List<MetadataValue> values = entity.getMetadata(CustomMobManager.CUSTOM_MOB_ID_META);
        for (MetadataValue value : values) {
            if (value == null) {
                continue;
            }
            Object raw = value.value();
            if (raw instanceof String id && !id.isBlank()) {
                return Optional.of(id);
            }
        }
        return Optional.empty();
    }

    /**
     * Produce a canonical identity string for mob comparisons. The canonical form
     * ignores ordering of words, punctuation, casing and color codes so entries
     * like "KING_SLIME", "Slime King" and "King_Slime" all normalize to the
     * same value.
     *
     * @param mobId MythicMob identifier or display alias
     * @return canonical identity suitable for equality comparisons
     */
    public static String canonicalMobKey(String mobId) {
        if (mobId == null || mobId.isEmpty()) {
            return "";
        }

        String plain = getPlainDisplayName(mobId);
        String base = (plain == null || plain.isBlank()) ? mobId : plain;
        List<String> tokens = tokenize(base);
        if (tokens.isEmpty()) {
            return "";
        }
        tokens.sort(String.CASE_INSENSITIVE_ORDER);
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            if (token.isBlank()) continue;
            if (builder.length() > 0) {
                builder.append('_');
            }
            builder.append(token.toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private static List<String> tokenize(String input) {
        if (input == null) {
            return java.util.Collections.emptyList();
        }
        String cleaned = ChatColor.stripColor(input);
        cleaned = cleaned.replace('_', ' ').replace('-', ' ');
        cleaned = cleaned.replaceAll("(?<=[A-Za-z])(?=[A-Z][a-z])", " ");
        cleaned = cleaned.replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ");
        cleaned = cleaned.replaceAll("[^A-Za-z0-9\\s]+", " ");
        cleaned = cleaned.trim();
        if (cleaned.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        String[] parts = cleaned.split("\\s+");
        List<String> tokens = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (!part.isEmpty()) {
                tokens.add(part);
            }
        }
        return tokens;
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
        if (entity == null) {
            return false;
        }
        boolean visible = entity.isCustomNameVisible() && hasNumericHealth(entity.getCustomName());
        boolean viaDisplay = entity.hasMetadata("lp_numeric_hp");
        return visible || viaDisplay;
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
