package me.nakilex.levelplugin.mob.listeners;

import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.mob.managers.ChatToggleManager;
import me.nakilex.levelplugin.mob.utils.MobNameUtil;
import me.nakilex.levelplugin.player.attributes.listeners.StatsEffectListener;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.spells.managers.SpellContextManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
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
                        SpellContextManager.consume(player.getUniqueId());
                if (ctx != null) {
                    spellName = ctx.spellName;
                    isCrit = ctx.isCrit;
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
                SpellContextManager.consume(player.getUniqueId());

            if (ctx != null) {
                spellName = ctx.spellName;
                isCrit    = ctx.isCrit;
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

        // check chat toggle
        if (!ChatToggleManager.getInstance().isEnabled(player)) return;

        // build & send message
        String targetName;
        ActiveMob mythic = Main.getInstance().getMythicHelper()
                .getMythicMobInstance(event.getEntity());
        if (mythic != null) {
            PlaceholderString disp = mythic.getType().getDisplayName();
            String name = disp != null ? disp.get() : mythic.getType().getInternalName();
            targetName = ChatColor.stripColor(name != null ? name :
                    MobNameUtil.toPrettyName(mythic.getType().getInternalName()));
        } else {
            targetName = event.getEntity().getType().name();
            targetName = targetName.charAt(0) + targetName.substring(1).toLowerCase();
        }

        double dmg = event.getFinalDamage();
        String hitWord = isCrit ? "critically hit" : "hit";
        ChatColor mainColor = isCrit ? ChatColor.YELLOW : ChatColor.WHITE;

        String msg = mainColor + spellName +
            ChatColor.GRAY + " " + hitWord + " " +
            ChatColor.YELLOW + targetName +
            ChatColor.GRAY + " for " +
            ChatColor.GRAY + String.format("%.1f", dmg) + " " +
                ChatColor.RED + "\u2764";

        player.sendMessage(msg);
    }

}
