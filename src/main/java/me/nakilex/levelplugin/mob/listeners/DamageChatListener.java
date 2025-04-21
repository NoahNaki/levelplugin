package me.nakilex.levelplugin.mob.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.mob.managers.ChatToggleManager;
import me.nakilex.levelplugin.spells.managers.SpellContextManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class DamageChatListener implements Listener {

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity rawDamager = event.getDamager();
        Player player = null;
        String spellName = null;
        boolean isCrit = false;

        Main.getInstance().getLogger()
            .info("[ChatListener] Damage event: damager=" + rawDamager + " target=" + event.getEntity());

        // 1) Projectile‑based spells
        if (rawDamager instanceof Projectile) {
            Projectile proj = (Projectile) rawDamager;
            if (proj.getShooter() instanceof Player) {
                player = (Player) proj.getShooter();
                // e.g. Meteor fireball tagged earlier
                if (proj.hasMetadata("Meteor")) {
                    spellName = "Meteor";
                    // you could also check a crit‑metadata flag here
                }
            }
        }
        // 2) Direct‑damage via SpellContextManager
        else if (rawDamager instanceof Player) {
            player = (Player) rawDamager;
            SpellContextManager.Context ctx =
                SpellContextManager.consume(player.getUniqueId());
            Main.getInstance().getLogger()
                .info("[ChatListener] Consumed context: " + (ctx == null ? "null" : ctx.spellName + ", crit=" + ctx.isCrit));
            if (ctx != null) {
                spellName = ctx.spellName;
                isCrit    = ctx.isCrit;
            }
        }

        // nothing to do if not a player spell/attack
        if (player == null || spellName == null) return;

        // check toggle
        if (!ChatToggleManager.getInstance().isEnabled(player)) return;

        // build and send the message
        String targetName = event.getEntity().getType().name();
        targetName = targetName.charAt(0) + targetName.substring(1).toLowerCase();

        double dmg = event.getFinalDamage();
        String hitType = isCrit ? "critically hit" : "hit";
        String msg = String.format("%s %s %s for %.1f damage", spellName, hitType, targetName, dmg);

        Main.getInstance().getLogger()
            .info("[ChatListener] Sending chat: " + msg);

        player.sendMessage((isCrit ? ChatColor.YELLOW : ChatColor.WHITE) + msg);
    }
}
