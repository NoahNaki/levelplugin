package me.nakilex.levelplugin.pet.listeners;

import me.nakilex.levelplugin.pet.PetEffectType;
import me.nakilex.levelplugin.pet.PetManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

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
        var maxHealthAttr = attacker.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr == null) {
            return;
        }
        double maxHealth = maxHealthAttr.getValue();
        double newHealth = Math.min(maxHealth, attacker.getHealth() + healAmount);
        attacker.setHealth(Math.max(0.0, newHealth));
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
