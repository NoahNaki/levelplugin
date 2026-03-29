package me.nakilex.levelplugin.mob.custom;

import me.nakilex.levelplugin.utils.ModelEngineUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Triggers contextual ModelEngine animations for custom mob attacks.
 */
public class CustomMobAnimationListener implements Listener {
    private final CustomMobManager customMobManager;

    public CustomMobAnimationListener(CustomMobManager customMobManager) {
        this.customMobManager = customMobManager;
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
