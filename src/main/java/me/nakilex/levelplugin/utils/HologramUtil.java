package me.nakilex.levelplugin.utils;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Utility for spawning floating damage numbers with zero‑flash via an ArmorStand pool.
 */
public class HologramUtil {

    private static final int POOL_SIZE = 50;
    private static final int LIFETIME_TICKS = 10;
    private static final double RISE_PER_TICK = 0.02;
    private static final double START_Y_OFFSET = 0.5;
    private static final double POOL_Y = -50;      // far below world

    private static final Queue<ArmorStand> pool = new ConcurrentLinkedQueue<>();
    private static boolean initialized = false;

    /**
     * Call once at plugin startup (or will auto‑init on first use).
     */
    public static synchronized void initPool(World world) {
        if (initialized) return;
        for (int i = 0; i < POOL_SIZE; i++) {
            Location offscreen = new Location(world, 0, POOL_Y, 0);
            ArmorStand stand = (ArmorStand) world.spawnEntity(offscreen, EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setBasePlate(false);
            stand.setCustomNameVisible(true);
            pool.offer(stand);
        }
        initialized = true;
    }

    /**
     * Grab a stand, teleport it to above the target, show the text, animate & return it to the pool.
     */
    public static void spawnDamageHologram(Player viewer, Location at, String text) {
        if (!initialized) initPool(at.getWorld());

        Main.getInstance().getLogger().info(
            "[HologramUtil] spawnDamageHologram viewer=" + viewer.getName() +
            " text=" + org.bukkit.ChatColor.stripColor(text)
        );

        ArmorStand stand = pool.poll();
        if (stand == null) {
            Main.getInstance().getLogger().info("[HologramUtil] Pool empty -> spawnOneOff");
            // fallback to one‑off spawn if pool is exhausted
            spawnOneOff(viewer, at, text);
            return;
        }

        Main.getInstance().getLogger().info(
            "[HologramUtil] Using pooled stand, remaining=" + pool.size()
        );

        // Position & name
        stand.setCustomName(text);
        stand.teleport(at.clone().add(0, START_Y_OFFSET, 0));

        // Hide from everyone except the viewer (after one tick so the spawn
        // packet is processed first)
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            Main.getInstance().getLogger().info(
                "[HologramUtil] Updating visibility for " + viewer.getName()
            );
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.equals(viewer)) {
                    p.showEntity(Main.getInstance(), stand);
                } else {
                    p.hideEntity(Main.getInstance(), stand);
                }
            }
        }, 1L);

        // Animate & recycle
        new BukkitRunnable() {
            private int age = 0;

            @Override
            public void run() {
                if (age++ >= LIFETIME_TICKS || stand.isDead()) {
                    // hide from all players so next usage starts clean
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.hideEntity(Main.getInstance(), stand);
                    }
                    Main.getInstance().getLogger().info(
                        "[HologramUtil] Recycle stand id=" + stand.getEntityId()
                    );
                    // send it offscreen and recycle
                    stand.teleport(new Location(at.getWorld(), 0, POOL_Y, 0));
                    stand.setCustomName("");
                    pool.offer(stand);
                    cancel();
                    return;
                }
                stand.teleport(stand.getLocation().add(0, RISE_PER_TICK, 0));
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    /**
     * In the unlikely event the pool is empty, fall back to a temporary stand.
     */
    private static void spawnOneOff(Player viewer, Location loc, String text) {
        Main.getInstance().getLogger().info(
            "[HologramUtil] spawnOneOff viewer=" + viewer.getName()
        );
        Location spawnLoc = loc.clone().add(0, START_Y_OFFSET, 0);
        ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setInvisible(true);
        stand.setMarker(true);
        stand.setGravity(false);
        stand.setBasePlate(false);
        stand.setSmall(true);
        stand.setCustomNameVisible(true);
        stand.setCustomName(text);

        // Hide from everyone except viewer (after one tick to ensure spawn packet)
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.equals(viewer)) {
                    p.showEntity(Main.getInstance(), stand);
                } else {
                    p.hideEntity(Main.getInstance(), stand);
                }
            }
        }, 1L);

        // remove after LIFETIME_TICKS
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!stand.isDead()) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.hideEntity(Main.getInstance(), stand);
                    }
                    Main.getInstance().getLogger().info(
                        "[HologramUtil] remove one-off stand id=" + stand.getEntityId()
                    );
                    stand.remove();
                }
            }
        }.runTaskLater(Main.getInstance(), LIFETIME_TICKS);
    }
}
