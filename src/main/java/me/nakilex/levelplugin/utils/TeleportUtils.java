package me.nakilex.levelplugin.utils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Utility methods for teleporting players with a visual effect. */
public final class TeleportUtils {
    private TeleportUtils() {}

    /**
     * Teleport the player instantly while spawning particles at the origin and
     * destination along with the enderman teleport sound.
     */
    public static void teleportWithEffect(Player player, Location dest) {
        Location origin = player.getLocation();
        origin.getWorld().spawnParticle(Particle.DRAGON_BREATH, origin, 100, 0.5, 1, 0.5, 0);
        player.teleport(dest);
        dest.getWorld().spawnParticle(Particle.DRAGON_BREATH, dest, 100, 0.5, 1, 0.5, 0);
        dest.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
    }
}
