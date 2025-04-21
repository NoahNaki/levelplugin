package me.nakilex.levelplugin.mob.listeners;

import me.nakilex.levelplugin.utils.HologramUtil;
import me.nakilex.levelplugin.mob.managers.DmgNumberToggleManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class DamageIndicatorListener implements Listener {

    private final DmgNumberToggleManager toggleManager;

    public DamageIndicatorListener(DmgNumberToggleManager toggleManager) {
        this.toggleManager = toggleManager;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Only care when a player damages a living entity
        if (!(event.getDamager() instanceof Player)) return;
        Entity target = event.getEntity();
        if (!(target instanceof LivingEntity)) return;

        Player damager = (Player) event.getDamager();

        // Check if player has damage numbers enabled
        if (!toggleManager.isEnabled(damager)) return;

        // Get the final damage dealt
        double damage = event.getFinalDamage();

        // Format the text (e.g. "-7.5")
        String text = ChatColor.RED + String.format("-%.1f", damage);

        // Spawn the holographic damage number at the target's eye location
        LivingEntity mob = (LivingEntity) target;
        HologramUtil.spawnDamageHologram(mob.getEyeLocation(), text);
    }
}
