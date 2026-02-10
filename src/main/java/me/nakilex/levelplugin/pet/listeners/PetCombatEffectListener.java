package me.nakilex.levelplugin.pet.listeners;

import me.nakilex.levelplugin.pet.PetEffectType;
import me.nakilex.levelplugin.pet.PetManager;
import me.nakilex.levelplugin.utils.PotionEffectUtil;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;

public class PetCombatEffectListener implements Listener {
    private final PetManager petManager;

    public PetCombatEffectListener(PetManager petManager) {
        this.petManager = petManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLifeSteal(EntityDamageByEntityEvent event) {
        Player attacker = resolveAttacker(event);
        if (attacker == null) {
            return;
        }
        double lifeSteal = petManager.getActiveEffectValue(attacker.getUniqueId(), PetEffectType.LIFE_STEAL);
        if (lifeSteal <= 0.0) {
            return;
        }
        double healAmount = event.getFinalDamage() * lifeSteal;
        if (healAmount <= 0.0) {
            return;
        }
        var maxHealthAttr = attacker.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr == null) {
            return;
        }
        double maxHealth = maxHealthAttr.getValue();
        double newHealth = Math.min(maxHealth, attacker.getHealth() + healAmount);
        attacker.setHealth(Math.max(0.0, newHealth));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLastStand(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (petManager.isLastStandImmune(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        double lastStandValue = petManager.getActiveEffectValue(player.getUniqueId(), PetEffectType.LAST_STAND);
        if (lastStandValue <= 0.0) {
            return;
        }
        double healthAfter = player.getHealth() - event.getFinalDamage();
        if (healthAfter > 0.0) {
            return;
        }
        if (!petManager.tryActivateLastStand(player)) {
            return;
        }
        event.setCancelled(true);
        player.setHealth(Math.max(1.0, player.getHealth()));
        PotionEffectUtil.applyHiddenEffect(player, PotionEffectType.SPEED, 100, 0);
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }
}
