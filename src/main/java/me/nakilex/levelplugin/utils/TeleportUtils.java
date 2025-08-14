package me.nakilex.levelplugin.utils;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Utility methods for teleporting players with a visual effect. */
public final class TeleportUtils {
    private TeleportUtils() {}

    /**
     * Teleport the player after a brief delay while spawning particles at the
     * origin and destination along with the enderman teleport sound.
     */
    public static void teleportWithEffect(Player player, Location dest) {
        teleportWithEffect(player, dest, 15L);
    }

    /**
     * Teleport the player with particles/sound after the given delay.
     */
    public static void teleportWithEffect(Player player, Location dest, long delayTicks) {
        Location origin = player.getLocation();
        origin.getWorld().spawnParticle(Particle.DRAGON_BREATH, origin, 100, 0.6, 1.2, 0.6, 0);
        origin.getWorld().spawnParticle(Particle.PORTAL, origin, 60, 0.6, 1.2, 0.6, 0.2);
        origin.getWorld().spawnParticle(Particle.END_ROD, origin, 80, 0.6, 1.2, 0.6, 0.1);
        origin.getWorld().playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            player.teleport(dest);
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, false));
            dest.getWorld().spawnParticle(Particle.DRAGON_BREATH, dest, 140, 0.6, 1.2, 0.6, 0);
            dest.getWorld().spawnParticle(Particle.END_ROD, dest, 80, 0.6, 1.2, 0.6, 0.1);
            dest.getWorld().spawnParticle(Particle.PORTAL, dest, 80, 0.6, 1.2, 0.6, 0.2);
            dest.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () ->
                    dest.getWorld().spawnParticle(Particle.SPELL_WITCH, dest, 60, 0.6, 1.2, 0.6, 0.1),
                10L);
        }, delayTicks);
    }
}
