package me.nakilex.levelplugin.horse.traits;

import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;

/**
 * Represents a special ability a horse can perform.
 * Traits are intended to be simple and reusable so new ones can be added
 * without modifying the core horse system.
 */
public interface HorseTrait {
    /** Unique identifier used for persistence. */
    String getId();

    /** Cooldown in seconds between activations. */
    int getCooldownSeconds();

    /**
     * Apply the trait effect. Implementations should handle their own visuals
     * and state changes.
     */
    void apply(Player player, AbstractHorse horse);
}
