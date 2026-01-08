package me.nakilex.levelplugin.mob.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Utility methods to convert between MythicMob internal IDs and human-friendly names. */
public final class MobNameUtil {
    private static final String MYTHIC_PLUGIN = "MythicMobs";
    private static final String MYTHIC_BUKKIT_CLASS = "io.lumine.mythic.bukkit.MythicBukkit";

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
        Optional<Object> opt = resolveMythicMobHandle(mobId);
        if (opt.isPresent()) {
            String result = extractDisplayName(opt.get());
            if (result != null && !result.isEmpty()) {
                return result;
            }
        }
        return toPrettyName(mobId);
    }

    /**
     * Attempt to locate a MythicMob definition regardless of how the ID is cased or formatted.
     * Mythic mob IDs are traditionally upper-case and use underscores, while our configuration
     * files often use lower-case keys copied straight from Mythic. This helper keeps the lookup
     * tolerant so we can still resolve the correct display name even when the cases differ.
     */
    public static Optional<String> resolveMythicInternalName(String mobId) {
        return resolveMythicMobHandle(mobId)
                .map(MobNameUtil::extractInternalName)
                .filter(name -> name != null && !name.isBlank());
    }

    private static Optional<Object> resolveMythicMobHandle(String mobId) {
        if (mobId == null || mobId.isEmpty()) {
            return Optional.empty();
        }

        if (!Bukkit.getPluginManager().isPluginEnabled(MYTHIC_PLUGIN)) {
            return Optional.empty();
        }

        Object manager;
        try {
            Class<?> mythicClass = Class.forName(MYTHIC_BUKKIT_CLASS);
            Object mythic = mythicClass.getMethod("inst").invoke(null);
            manager = mythic.getClass().getMethod("getMobManager").invoke(mythic);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return Optional.empty();
        }
        if (manager == null) {
            return Optional.empty();
        }

        String normalized = mobId.replace(' ', '_');
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(mobId);
        candidates.add(normalized);
        candidates.add(mobId.toUpperCase(Locale.ROOT));
        candidates.add(normalized.toUpperCase(Locale.ROOT));
        candidates.add(mobId.toLowerCase(Locale.ROOT));
        candidates.add(normalized.toLowerCase(Locale.ROOT));

        List<String> tokens = tokenize(mobId);
        if (!tokens.isEmpty()) {
            String joined = String.join("_", tokens);
            candidates.add(joined);
            candidates.add(joined.toUpperCase(Locale.ROOT));
            candidates.add(joined.toLowerCase(Locale.ROOT));

            if (tokens.size() > 1) {
                List<String> sorted = new ArrayList<>(tokens);
                sorted.sort(String.CASE_INSENSITIVE_ORDER);
                String sortedJoin = String.join("_", sorted);
                candidates.add(sortedJoin);
                candidates.add(sortedJoin.toUpperCase(Locale.ROOT));
                candidates.add(sortedJoin.toLowerCase(Locale.ROOT));

                List<String> reversed = new ArrayList<>(tokens);
                Collections.reverse(reversed);
                String reversedJoin = String.join("_", reversed);
                candidates.add(reversedJoin);
                candidates.add(reversedJoin.toUpperCase(Locale.ROOT));
                candidates.add(reversedJoin.toLowerCase(Locale.ROOT));
            }
        }

        for (String candidate : candidates) {
            if (candidate == null || candidate.isEmpty()) {
                continue;
            }
            try {
                Object result = manager.getClass()
                        .getMethod("getMythicMob", String.class)
                        .invoke(manager, candidate);
                if (result instanceof Optional<?> optional && optional.isPresent()) {
                    return Optional.of(optional.get());
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
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

        String base = resolveMythicInternalName(mobId).orElse(null);
        if (base == null || base.isBlank()) {
            String plain = getPlainDisplayName(mobId);
            base = (plain == null || plain.isBlank()) ? mobId : plain;
        }
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

    private static String extractDisplayName(Object mob) {
        try {
            Object placeholder = mob.getClass().getMethod("getDisplayName").invoke(mob);
            if (placeholder == null) {
                return null;
            }
            Object value = placeholder.getClass().getMethod("get").invoke(placeholder);
            return value != null ? value.toString() : null;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    private static String extractInternalName(Object mob) {
        try {
            Object name = mob.getClass().getMethod("getInternalName").invoke(mob);
            return name != null ? name.toString() : null;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
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
