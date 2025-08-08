package me.nakilex.levelplugin.screen;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;

import java.util.function.Consumer;

/**
 * Utility for spawning {@link Display} entities with optional configuration.
 * This generic helper avoids duplicating spawn logic for different display types
 * such as {@code TextDisplay} and {@code ItemDisplay}.
 */
public final class DisplayUtil {
    private DisplayUtil() {
    }

    /**
     * Spawn a new display entity of the given type at the provided location.
     * The supplied configurator is invoked immediately after spawn allowing
     * the caller to apply type specific properties.
     *
     * @param location    spawn location
     * @param displayType display class (e.g. TextDisplay.class)
     * @param configurator consumer to apply extra settings, may be null
     * @param <T>         display subtype
     * @return spawned display instance
     */
    public static <T extends Display> T spawn(Location location,
                                              Class<T> displayType,
                                              Consumer<T> configurator) {
        World world = location.getWorld();
        T display = world.spawn(location, displayType);
        if (configurator != null) {
            configurator.accept(display);
        }
        return display;
    }
}
