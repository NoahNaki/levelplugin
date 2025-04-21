package me.nakilex.levelplugin.mob.listeners;

import me.nakilex.levelplugin.mob.managers.DmgNumberToggleManager;
import me.nakilex.levelplugin.utils.HologramUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffectType;

public class DamageIndicatorListener implements Listener {

    private final DmgNumberToggleManager toggleManager;

    public DamageIndicatorListener(DmgNumberToggleManager toggleManager) {
        this.toggleManager = toggleManager;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Only handle Player → LivingEntity
        if (!(event.getDamager() instanceof Player)) return;
        Entity target = event.getEntity();
        if (!(target instanceof LivingEntity)) return;

        Player damager = (Player) event.getDamager();
        if (!toggleManager.isEnabled(damager)) return;

        double damage = event.getFinalDamage();

        // —— Manual crit detection ——
        boolean falling     = damager.getFallDistance() > 0.0F;
        boolean notGround   = !damager.isOnGround();
        boolean notClimb    = !damager.isClimbing();                         // no ladder/vine
        boolean notLiquid   = !damager.getLocation().getBlock().isLiquid();  // not in water/lava
        boolean notBlind    = !damager.hasPotionEffect(PotionEffectType.BLINDNESS);
        boolean notRiding   = damager.getVehicle() == null;
        boolean notSprint   = !damager.isSprinting();
        boolean charged     = ((float) ((Player) damager).getAttackCooldown()) > 0.84F;

        boolean isCrit = falling && notGround && notClimb
            && notLiquid && notBlind && notRiding
            && notSprint && charged
            && damage > 0.0;

        // Color code
        ChatColor color = isCrit ? ChatColor.GOLD : ChatColor.RED;
        String text     = color + String.format(isCrit ? "-%.1f✦" : "-%.1f", damage);

        // Spawn the floating text
        LivingEntity mob = (LivingEntity) target;
        HologramUtil.spawnDamageHologram(mob.getEyeLocation(), text);
    }
}
