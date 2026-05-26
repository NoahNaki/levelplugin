package me.nakilex.levelplugin.utils;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ThreadLocalRandom;

/** Utility for spawning decorative fireworks. */
public final class FireworkUtil {
    private FireworkUtil() {}

    private static final NamespacedKey DECORATIVE_KEY = new NamespacedKey(
            JavaPlugin.getProvidingPlugin(FireworkUtil.class), "decorative_firework");

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
        markDecorative(firework);
    }

    /**
     * Spawn a small burst of fireworks around the given location.
     * Existing launchFirework() is reused for each rocket to keep
     * the creation logic in a single place.
     *
     * @param center central location of the burst
     * @param amount number of rockets to spawn
     */
    public static void burst(Location center, int amount) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        for (int i = 0; i < amount; i++) {
            double dx = rand.nextGaussian() * 0.5;
            double dz = rand.nextGaussian() * 0.5;
            Location offset = center.clone().add(dx, 0, dz);
            launchFirework(offset);
        }
    }



    /**
     * Spawn a burst of fireworks at random points inside an axis-aligned region.
     *
     * @param anchor world/location anchor used for world context
     * @param minX minimum block x (inclusive)
     * @param minY minimum block y (inclusive)
     * @param minZ minimum block z (inclusive)
     * @param maxX maximum block x (inclusive)
     * @param maxY maximum block y (inclusive)
     * @param maxZ maximum block z (inclusive)
     * @param amount number of rockets to spawn
     */
    public static void burstWithinArea(Location anchor,
                                       int minX, int minY, int minZ,
                                       int maxX, int maxY, int maxZ,
                                       int amount) {
        if (anchor == null || anchor.getWorld() == null || amount <= 0) {
            return;
        }

        int lowX = Math.min(minX, maxX);
        int lowY = Math.min(minY, maxY);
        int lowZ = Math.min(minZ, maxZ);
        int highX = Math.max(minX, maxX);
        int highY = Math.max(minY, maxY);
        int highZ = Math.max(minZ, maxZ);

        ThreadLocalRandom rand = ThreadLocalRandom.current();
        for (int i = 0; i < amount; i++) {
            int x = rand.nextInt(lowX, highX + 1);
            int y = rand.nextInt(lowY, highY + 1);
            int z = rand.nextInt(lowZ, highZ + 1);
            launchFirework(new Location(anchor.getWorld(), x + 0.5, y + 0.5, z + 0.5));
        }
    }

    public static boolean isDecorative(Firework firework) {
        if (firework == null) {
            return false;
        }
        return firework.getPersistentDataContainer().has(DECORATIVE_KEY, PersistentDataType.BYTE);
    }

    private static void markDecorative(Firework firework) {
        if (firework == null) {
            return;
        }
        firework.getPersistentDataContainer().set(DECORATIVE_KEY, PersistentDataType.BYTE, (byte) 1);
    }
}
