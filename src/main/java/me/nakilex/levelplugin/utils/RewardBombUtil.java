package me.nakilex.levelplugin.utils;

import java.util.function.Supplier;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/** Utility to spew rewards fountain-style for a short duration. */
public final class RewardBombUtil {
    private RewardBombUtil() {
    }

    private static final int DROP_INTERVAL_TICKS = 12;

    /**
     * Spawns a short fountain of rewards from the target location.
     *
     * @param plugin   plugin scheduler host
     * @param origin   center location to emit from
     * @param reward   supplier for each reward item drop
     * @param duration duration in ticks
     */
    public static void startRewardBomb(JavaPlugin plugin, Location origin, Supplier<ItemStack> reward, int duration) {
        startRewardBomb(plugin, origin, reward, duration, null);
    }

    /**
     * Reward bomb that exposes the requested level to the reward supplier.
     */
    public static void startRewardBomb(JavaPlugin plugin, Location origin, int level, java.util.function.Function<Integer, ItemStack> reward, int duration, Player owner) {
        if (reward == null) {
            startRewardBomb(plugin, origin, (Supplier<ItemStack>) null, duration, owner);
            return;
        }
        startRewardBomb(plugin, origin, () -> reward.apply(level), duration, owner);
    }

    /**
     * Client-biased reward bomb that optionally sets an owner for each drop so every player
     * sees their own fountain of loot.
     */
    public static void startRewardBomb(JavaPlugin plugin, Location origin, Supplier<ItemStack> reward, int duration, Player owner) {
        if (plugin == null || origin == null || reward == null || duration <= 0) return;
        new BukkitRunnable() {
            int lived = 0;

            @Override
            public void run() {
                if (!origin.isWorldLoaded()) {
                    cancel();
                    return;
                }
                if (lived >= duration) {
                    cancel();
                    return;
                }
                lived += DROP_INTERVAL_TICKS;
                ItemStack item = reward.get();
                if (item != null) {
                    Vector vel = new Vector(
                            randomRange(-0.35, 0.35),
                            0.5 + randomRange(0.1, 0.25),
                            randomRange(-0.35, 0.35));
                    Item drop = origin.getWorld().dropItem(origin.clone().add(0.5, 1, 0.5), item);
                    if (owner != null) {
                        drop.setOwner(owner.getUniqueId());
                        for (Player viewer : origin.getWorld().getPlayers()) {
                            if (!viewer.getUniqueId().equals(owner.getUniqueId())) {
                                viewer.hideEntity(plugin, drop);
                            }
                        }
                    }
                    drop.setVelocity(vel);
                }
                origin.getWorld().spawnParticle(Particle.ENCHANT, origin.clone().add(0.5, 1.1, 0.5), 18, 0.4, 0.4, 0.4, 0.1);
                origin.getWorld().playSound(origin, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.1f);
            }
        }.runTaskTimer(plugin, 0L, DROP_INTERVAL_TICKS);
    }

    private static double randomRange(double min, double max) {
        return min + Math.random() * (max - min);
    }
}
