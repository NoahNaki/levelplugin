package me.nakilex.levelplugin.utils;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

/** Utility for spawning decorative fireworks. */
public final class FireworkUtil {
    private FireworkUtil() {}

    /**
     * Launch a small firework at the given location.
     */
    public static void launchFirework(Location location) {
        Firework firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = firework.getFireworkMeta();
        FireworkEffect effect = FireworkEffect.builder()
                .withColor(Color.GREEN)
                .withFade(Color.BLUE)
                .with(FireworkEffect.Type.BALL)
                .withFlicker()
                .withTrail()
                .build();
        meta.addEffect(effect);
        meta.setPower(1);
        firework.setFireworkMeta(meta);
        firework.setSilent(true);
    }
}
