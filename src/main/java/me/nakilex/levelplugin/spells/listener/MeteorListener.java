package me.nakilex.levelplugin.spells.listener;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Handles post-spawn interactions for Meteor effects.
 */
public class MeteorListener implements Listener {

    @EventHandler
    public void onMeteorPlace(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock fb)) return;
        if (!fb.hasMetadata("Meteor")) return;
        event.setCancelled(true);
        fb.remove();
    }

    @EventHandler
    public void onFireballDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Fireball fb)) return;
        if (!fb.hasMetadata("Meteor")) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(fb.getShooter() instanceof Player shooter)) return;
        if (!DuelManager.getInstance().areInDuel(shooter.getUniqueId(), victim.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
