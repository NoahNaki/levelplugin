package me.nakilex.levelplugin.mob.listeners;

import io.lumine.mythic.bukkit.events.MythicDamageEvent;
import me.nakilex.levelplugin.mob.utils.MythicEventUtil;
import me.nakilex.levelplugin.player.attributes.listeners.StatsEffectListener;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.spells.managers.SpellContextManager;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class DamageChatListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity rawDamager = event.getDamager();
        Player player = null;
        String spellName = null;
        boolean isCrit = false;

        // 1) Projectile-based spells & basic‐attack arrows
        if (rawDamager instanceof Projectile) {
            Projectile proj = (Projectile) rawDamager;
            if (proj.getShooter() instanceof Player) {
                player = (Player) proj.getShooter();

                SpellContextManager.Context ctx =
                        SpellContextManager.peek(player.getUniqueId());
                if (ctx != null) {
                    spellName = ctx.spellName;
                    isCrit = ctx.isCrit;
                    SpellContextManager.consume(player.getUniqueId());
                } else if (proj.hasMetadata("Meteor")) {
                    spellName = "Meteor";
                } else if (proj.hasMetadata("BasicAttack")) {
                    spellName = "Basic Attack";
                }
            }
        }
        // 2) Direct-damage via SpellContextManager or melee basic‐attack
        else if (rawDamager instanceof Player) {
            player = (Player) rawDamager;

            // a) consume any spell context
            SpellContextManager.Context ctx =
                SpellContextManager.peek(player.getUniqueId());

            if (ctx != null) {
                spellName = ctx.spellName;
                isCrit    = ctx.isCrit;
                SpellContextManager.consume(player.getUniqueId());
            } else {
                // b) no spell → check for Warrior/Rogue basic melee
                StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
                String className = ps.playerClass.name().toLowerCase();
                if ("warrior".equals(className) || "rogue".equals(className)) {
                    spellName = "Basic Attack";
                    isCrit = StatsEffectListener.consumeLastCrit(player);
                }
            }
        }

        // nothing to do if not a player spell/attack
        if (player == null || spellName == null) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        SpellUtils.maybeSendDamageChat(player, target, event.getFinalDamage(), spellName, isCrit);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMythicDamage(MythicDamageEvent event) {
        Player player = MythicEventUtil.resolvePlayer(event);
        if (player == null) return;

        Entity damager = MythicEventUtil.resolveDamager(event);
        if (damager instanceof Player) return;
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player) return;

        LivingEntity target = MythicEventUtil.resolveTarget(event);
        if (target == null) return;

        SpellContextManager.Context ctx = SpellContextManager.consume(player.getUniqueId());
        if (ctx == null) return;

        StatsEffectListener.recordCrit(player, ctx.isCrit);
        SpellUtils.maybeSendDamageChat(player, target, event.getDamage(), ctx.spellName, ctx.isCrit);
    }

}
