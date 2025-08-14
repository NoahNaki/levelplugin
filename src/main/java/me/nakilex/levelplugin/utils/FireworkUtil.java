package me.nakilex.levelplugin.utils;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.concurrent.ThreadLocalRandom;

/** Utility for spawning decorative fireworks. */
public final class FireworkUtil {
    private FireworkUtil() {}

    /**
     * Launch a decorative firework with randomized color, type, and size.
     */
    public static void launchFirework(Location location) {
        Firework firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = firework.getFireworkMeta();

        ThreadLocalRandom rand = ThreadLocalRandom.current();
        Color primary = Color.fromRGB(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
        Color fade = Color.fromRGB(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
        FireworkEffect.Type type = FireworkEffect.Type.values()[rand.nextInt(FireworkEffect.Type.values().length)];

        FireworkEffect effect = FireworkEffect.builder()
                .withColor(primary)
                .withFade(fade)
                .with(type)
                .flicker(rand.nextBoolean())
                .trail(rand.nextBoolean())
                .build();
        meta.addEffect(effect);
        meta.setPower(rand.nextInt(3) + 1); // 1-3 power levels
        firework.setFireworkMeta(meta);
        firework.setSilent(true);
    }

    /**
     * Launch multiple decorative fireworks around a center point with random
     * offsets. Radius controls how far from the center fireworks may spawn,
     * minY ensures they appear above a minimum height, and maxYOffset adds a
     * random vertical variation.
     */
    public static void launchRandomFireworkBurst(Location center, double radius,
                                                 int count, double minY,
                                                 double maxYOffset) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            double dist = radius * Math.sqrt(rand.nextDouble());
            double angle = rand.nextDouble() * 2 * Math.PI;
            double x = center.getX() + dist * Math.cos(angle);
            double z = center.getZ() + dist * Math.sin(angle);
            double y = Math.max(minY, center.getY()) + rand.nextDouble() * maxYOffset;
            launchFirework(new Location(center.getWorld(), x, y, z));
        }
    }
}
