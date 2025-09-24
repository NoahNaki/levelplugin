package me.nakilex.levelplugin.pathfinding.npc;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Centralised factory for creating {@link PathNpc} profiles from simple
 * string identifiers. Having a single mapping avoids duplicating switch
 * statements in every feature that needs to spawn Citizens driven by our
 * mercenary combat behaviours.
 */
public final class PathNpcFactory {
    private static final List<String> KNOWN = List.of("rogue", "mage", "warrior", "archer");

    private PathNpcFactory() {}

    /**
     * Creates a new {@link PathNpc} implementation based on the provided
     * identifier. Identifiers are case-insensitive and mirror the options
     * exposed by the mercenary command.
     *
     * @param id profile identifier such as "rogue" or "mage"
     * @return optional containing a new profile instance if recognised
     */
    public static Optional<PathNpc> fromId(String id) {
        if (id == null || id.isEmpty()) {
            return Optional.empty();
        }
        return switch (id.toLowerCase(Locale.ROOT)) {
            case "rogue" -> Optional.of(new RogueMercenary());
            case "mage" -> Optional.of(new MageMercenary());
            case "warrior" -> Optional.of(new WarriorMercenary());
            case "archer" -> Optional.of(new ArcherMercenary());
            default -> Optional.empty();
        };
    }

    /**
     * @return immutable list of supported profile identifiers.
     */
    public static List<String> identifiers() {
        return KNOWN;
    }
}
