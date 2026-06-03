package me.nakilex.levelplugin.player.woodcutting.animation;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.woodcutting.WoodcuttingConfig;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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
        AtomicBoolean completed = new AtomicBoolean(false);
        Vector direction = fallDirectionResolver.resolve(player, tree.result()).normalize();
        Vector3f axis = new Vector3f((float) -direction.getZ(), 0.0f, (float) direction.getX()).normalize();
        Location pivot = tree.pivot();
        int treeHeight = tree.result().treeHeight();
        plugin.getLogger().info("[Woodcutting] Animation pivot=" + format(pivot)
                + " displays=" + tree.blocks().size()
                + " direction=" + String.format("%.3f,%.3f", direction.getX(), direction.getZ())
                + " axis=" + String.format("%.3f,%.3f,%.3f", axis.x, axis.y, axis.z)
                + " height=" + treeHeight);

        BukkitRunnable runnable = new BukkitRunnable() {
            double angle = Math.min(Math.PI / 2.0D, config.initialAngleRadians());
            double angularVelocity = 0.0D;
            boolean collided = false;

            @Override public void run() {
                try {
                    double acceleration = config.gravity() / Math.max(1.0D, treeHeight) * Math.sin(angle);
                    angularVelocity += acceleration * config.animationDeltaTime();
                    angle += angularVelocity * config.animationDeltaTime();
                    double clamped = Math.min(Math.PI / 2.0D, angle);
                    applyRotation(tree, axis, clamped);
                    if (config.collisionEnabled() && !collided && Math.toDegrees(clamped) >= config.collisionMinAngleDegrees()) {
                        damageNearbyEntities(player, pivot, direction, tree);
                        collided = true;
                    }
                    if (clamped >= Math.PI / 2.0D) {
                        cancel();
                        if (config.lyingDelayTicks() <= 0L) {
                            completeOnce(tree, onComplete, completed);
                        } else {
                            BukkitTask delayed = plugin.getServer().getScheduler().runTaskLater(plugin,
                                    () -> completeOnce(tree, onComplete, completed), config.lyingDelayTicks());
                            activeTasks.add(delayed);
                        }
                    }
                } catch (Throwable throwable) {
                    plugin.getLogger().log(java.util.logging.Level.WARNING, "[Woodcutting] Animation failed; cleaning up displays", throwable);
                    completeOnce(tree, onComplete, completed);
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

    private void applyRotation(DisplayTree tree, Vector3f axis, double angle) {
        Quaternionf rotation = new Quaternionf().fromAxisAngleRad(axis, (float) angle);
        for (DisplayTree.DisplayBlock displayBlock : tree.blocks()) {
            Vector3f originalOffset = displayBlock.originalOffsetFromPivot();
            Vector3f rotatedOffset = new Vector3f(originalOffset);
            rotation.transform(rotatedOffset);
            Vector3f delta = rotatedOffset.sub(originalOffset, new Vector3f());

            Transformation transformation = displayBlock.display().getTransformation();
            transformation.getLeftRotation().set(rotation);
            transformation.getTranslation().set(delta);
            displayBlock.display().setTransformation(transformation);
            displayBlock.display().setInterpolationDelay(0);
            displayBlock.display().setInterpolationDuration((int) config.ticksPerFrame());
        }
    }

    private void completeOnce(DisplayTree tree, Runnable onComplete, AtomicBoolean completed) {
        if (!completed.compareAndSet(false, true)) return;
        tree.removeDisplays();
        activeTrees.remove(tree);
        activeTasks.removeIf(BukkitTask::isCancelled);
        onComplete.run();
    }

    private void damageNearbyEntities(Player player, Location pivot, Vector direction, DisplayTree tree) {
        double radius = Math.max(3.0D, tree.result().logs().size() / 8.0D);
        Location center = pivot.clone().add(direction.clone().multiply(radius / 2.0D));
        for (LivingEntity entity : center.getWorld().getNearbyLivingEntities(center, radius)) {
            if (entity.equals(player)) {
                if (config.playerDamage() > 0) entity.damage(config.playerDamage(), player);
            } else if (config.entityDamage() > 0) entity.damage(config.entityDamage(), player);
        }
        if (config.collisionParticles()) {
            BlockData particleData = tree.blocks().isEmpty() ? tree.result().root().getBlockData() : tree.blocks().getFirst().data();
            center.getWorld().spawnParticle(Particle.BLOCK, center, 24, 1.0D, 0.4D, 1.0D, particleData);
        }
    }

    private String format(Location location) {
        return String.format("%.2f,%.2f,%.2f", location.getX(), location.getY(), location.getZ());
    }
}
