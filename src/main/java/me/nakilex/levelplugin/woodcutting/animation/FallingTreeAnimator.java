package me.nakilex.levelplugin.woodcutting.animation;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.woodcutting.WoodcuttingConfig;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class FallingTreeAnimator {
    private final Main plugin;
    private final WoodcuttingConfig config;
    private final FallDirectionResolver fallDirectionResolver;
    private final Set<DisplayTree> activeTrees = new HashSet<>();
    private final Set<BukkitTask> activeTasks = new HashSet<>();

    public FallingTreeAnimator(Main plugin, WoodcuttingConfig config, FallDirectionResolver fallDirectionResolver) {
        this.plugin = plugin;
        this.config = config;
        this.fallDirectionResolver = fallDirectionResolver;
    }

    public void animate(Player player, DisplayTree tree, Runnable onComplete) {
        activeTrees.add(tree);
        Vector direction = fallDirectionResolver.resolve(player, tree.result());
        Location pivot = tree.result().root().getLocation().add(0.5, 0, 0.5);
        BukkitRunnable runnable = new BukkitRunnable() {
            double angle = 0.0D;
            boolean collided = false;
            @Override public void run() {
                angle += config.fallSpeed();
                double clamped = Math.min(Math.PI / 2.0D, angle);
                for (DisplayTree.DisplayBlock displayBlock : tree.blocks()) {
                    Vector rotated = rotate(displayBlock.relativeOffset(), direction, clamped);
                    Location next = pivot.clone().add(rotated);
                    displayBlock.display().teleport(next);
                }
                if (config.collisionEnabled() && !collided && Math.toDegrees(clamped) >= config.collisionMinAngleDegrees()) {
                    damageNearbyEntities(player, pivot, direction, tree);
                    collided = true;
                }
                if (clamped >= Math.PI / 2.0D) {
                    if (config.lyingDelayTicks() <= 0L) finish(tree, onComplete, this);
                    else plugin.getServer().getScheduler().runTaskLater(plugin, () -> finish(tree, onComplete, this), config.lyingDelayTicks());
                    cancel();
                }
            }
        };
        BukkitTask task = runnable.runTaskTimer(plugin, 0L, config.ticksPerFrame());
        activeTasks.add(task);
    }

    public void shutdown() {
        for (BukkitTask task : Set.copyOf(activeTasks)) task.cancel();
        activeTasks.clear();
        for (DisplayTree tree : Set.copyOf(activeTrees)) tree.removeDisplays();
        activeTrees.clear();
    }

    private void finish(DisplayTree tree, Runnable onComplete, BukkitRunnable runnable) {
        tree.removeDisplays();
        activeTrees.remove(tree);
        activeTasks.removeIf(BukkitTask::isCancelled);
        onComplete.run();
    }

    private Vector rotate(Vector offset, Vector direction, double angle) {
        double vertical = offset.getY();
        Vector horizontal = direction.clone().multiply(vertical * Math.sin(angle));
        double nextY = vertical * Math.cos(angle) - (0.20D * Math.sin(angle));
        return new Vector(offset.getX(), nextY, offset.getZ()).add(horizontal);
    }

    private void damageNearbyEntities(Player player, Location pivot, Vector direction, DisplayTree tree) {
        double radius = Math.max(3.0D, tree.result().logs().size() / 8.0D);
        Location center = pivot.clone().add(direction.clone().multiply(radius / 2.0D));
        for (LivingEntity entity : center.getWorld().getNearbyLivingEntities(center, radius)) {
            if (entity.equals(player)) {
                if (config.playerDamage() > 0) entity.damage(config.playerDamage(), player);
            } else if (config.entityDamage() > 0) entity.damage(config.entityDamage(), player);
        }
        if (config.collisionParticles()) center.getWorld().spawnParticle(Particle.BLOCK, center, 24, 1.0D, 0.4D, 1.0D, tree.result().root().getBlockData());
    }
}
