package me.nakilex.levelplugin.mob.custom;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ModelEngineUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

/**
 * Triggers contextual ModelEngine animations for custom mob attacks.
 */
public class CustomMobAnimationListener implements Listener {
    private static final double WALK_THRESHOLD = 0.0035;

    private final Main plugin;
    private final CustomMobManager customMobManager;

    public CustomMobAnimationListener(Main plugin, CustomMobManager customMobManager) {
        this.plugin = plugin;
        this.customMobManager = customMobManager;
        startMovementTicker();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCustomMobDamage(EntityDamageByEntityEvent event) {
        if (customMobManager == null) {
            return;
        }
        LivingEntity attacker = resolveLivingDamager(event.getDamager());
        if (attacker == null) {
            return;
        }
        if (customMobManager.getInstance(attacker).isEmpty()) {
            return;
        }
        ModelEngineUtil.playBestAttackAnimation(attacker);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (customMobManager == null) {
            return;
        }
        if (!(event.getEntity().getShooter() instanceof LivingEntity shooter)) {
            return;
        }
        if (customMobManager.getInstance(shooter).isEmpty()) {
            return;
        }
        ModelEngineUtil.playBestShootAnimation(shooter);
    }

    private void startMovementTicker() {
        if (plugin == null || customMobManager == null) {
            return;
        }
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (CustomMobInstance instance : customMobManager.getActiveMobs().values()) {
                LivingEntity entity = instance.entity();
                if (entity == null || entity.isDead()) {
                    continue;
                }
                double horizontalSpeed = entity.getVelocity().clone().setY(0).lengthSquared();
                if (horizontalSpeed > WALK_THRESHOLD) {
                    ModelEngineUtil.playBestAnimation(entity, java.util.List.of("walk", "run", "move"), true);
                } else {
                    ModelEngineUtil.playBestAnimation(entity, java.util.List.of("idle", "stand", "loop"), true);
                }
            }
        }, 10L, 10L);
    }

    private LivingEntity resolveLivingDamager(Entity damager) {
        if (damager instanceof LivingEntity living) {
            return living;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity livingShooter) {
            return livingShooter;
        }
        return null;
    }
}
