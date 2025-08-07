package me.nakilex.levelplugin.utils;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Utility holder for Adventure component helpers.
 *
 * <p>Provides shared instances of serializers and other
 * Adventure-related helpers so they can be reused across the plugin
 * without recreating the same objects.</p>
 */
public final class ComponentUtil {

    /**
     * Shared {@link LegacyComponentSerializer} using section colour codes.
     */
    public static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private ComponentUtil() {
        // Utility class
    }
}

