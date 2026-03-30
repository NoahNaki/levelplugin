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
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

/**
 * Triggers contextual ModelEngine animations for custom mob attacks.
 */
public class CustomMobAnimationListener implements Listener {
    private static final double WALK_THRESHOLD = 0.0035;
    private static final long ATTACK_ACTION_HOLD_MS = 450L;
    private static final long SHOOT_ACTION_HOLD_MS = 550L;

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
        if (isScriptDriven(attacker)) {
            return;
        }
        ModelEngineUtil.triggerActionState(attacker, java.util.List.of("attack", "slash", "swing", "hit", "shoot", "cast"), ATTACK_ACTION_HOLD_MS);
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
        if (isScriptDriven(shooter)) {
            return;
        }
        ModelEngineUtil.triggerActionState(shooter, java.util.List.of("shoot", "arrow", "bow", "cast", "attack"), SHOOT_ACTION_HOLD_MS);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCustomMobDeath(EntityDeathEvent event) {
        if (customMobManager == null) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (customMobManager.getInstance(entity).isEmpty()) {
            return;
        }
        ModelEngineUtil.clearAnimationState(entity);
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
                ModelEngineUtil.setMovingState(entity, horizontalSpeed > WALK_THRESHOLD);
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

    private boolean isScriptDriven(LivingEntity entity) {
        if (entity == null || customMobManager == null) {
            return false;
        }
        return customMobManager.getInstance(entity)
                .map(instance -> instance.definition().spells().stream()
                        .anyMatch(spell -> spell != null
                                && spell.scriptKey() != null
                                && !spell.scriptKey().isBlank()))
                .orElse(false);
    }
}
