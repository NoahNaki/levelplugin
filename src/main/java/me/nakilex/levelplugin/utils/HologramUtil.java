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
     * Spawn a short lived ArmorStand showing the given damage text. The
     * previous pooled implementation proved unreliable on some servers, so we
     * now simply spawn a temporary stand every time.
     */
    public static void spawnDamageHologram(Player viewer, Location at, String text) {
        // Ensure the pool is initialised in case other parts of the plugin rely
        // on it, but delegate to the simple one-off spawn for reliability.
        if (!initialized) {
            initPool(at.getWorld());
        }

        spawnOneOff(viewer, at, text);
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
