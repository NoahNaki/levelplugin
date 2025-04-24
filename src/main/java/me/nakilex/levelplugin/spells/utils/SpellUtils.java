package me.nakilex.levelplugin.spells.utils;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.effects.listeners.StatsEffectListener;
import me.nakilex.levelplugin.spells.managers.SpellContextManager;
import me.nakilex.levelplugin.mob.managers.ChatToggleManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Utility methods for consistent spell damage application and chat output.
 */
public class SpellUtils {

    /**
     * Applies spell damage quietly (suppressing the vanilla chat),
     * then sends exactly one custom log message like:
     *   <SpellName> hit/critically hit <MobName> for <damage> ❤ damage.
     *
     * @param caster     the Player casting the spell
     * @param target     the LivingEntity being damaged
     * @param rawDamage     amount of damage to apply
     * @param spellName  name of the spell (displayed in log)
     */
    public static void dealWithChat(
        Player caster,
        LivingEntity target,
        double rawDamage,
        String spellName
    ) {
        ChatToggleManager chatMgr = ChatToggleManager.getInstance();
        boolean wasChatOn = chatMgr.isEnabled(caster);
        chatMgr.setEnabled(caster, false);

        // 1) Prepare a one‐off listener to catch the final damage value
        Listener damageListener = new Listener() {
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void onEntityDamage(EntityDamageByEntityEvent ev) {
                if (!ev.getEntity().equals(target)) return;

                Player damager = null;
                if (ev.getDamager() instanceof Player p) {
                    damager = p;
                } else if (ev.getDamager() instanceof Projectile proj
                    && proj.getShooter() instanceof Player shooter) {
                    damager = shooter;
                }
                if (damager == null || !damager.equals(caster)) return;

                // now that we know it's your spell hit, grab the final damage:
                double finalDamage = ev.getFinalDamage();
                boolean wasCrit    = StatsEffectListener.consumeLastCrit(caster);
                String hitType     = wasCrit ? "critically hit" : "hit";
                ChatColor color    = wasCrit ? ChatColor.RED : ChatColor.WHITE;
                String mobName     = getMobDisplayName(target);
                double display     = Math.round(finalDamage * 10.0) / 10.0;

                if (wasChatOn) {
                    String msg = String.format(
                        "%s %s %s for %.1f ❤ damage",
                        spellName, hitType, mobName, display
                    );
                    caster.sendMessage(color + msg);
                }

                HandlerList.unregisterAll(this);
                chatMgr.setEnabled(caster, wasChatOn);
            }
        };
        Bukkit.getPluginManager().registerEvents(damageListener, Main.getInstance());

        // 2) Now actually deal the damage (fires the event immediately)
        SpellContextManager.applySpellDamage(
            caster,
            target,
            rawDamage,
            spellName,
            false
        );
    }




    /**
     * Returns the clean display name of the target, stripping MythicMobs tags,
     * otherwise falling back to a formatted vanilla EntityType name.
     */
    public static String getMobDisplayName(LivingEntity e) {
        try {
            ActiveMob am = MythicBukkit.inst()
                .getAPIHelper()
                .getMythicMobInstance(e);
            if (am != null) {
                String raw = am.getType()
                    .getDisplayName()
                    .get();
                String clean = raw.replaceAll("<[^>]+>", "");
                clean = ChatColor.stripColor(clean);
                return clean.trim();
            }
        } catch (Exception ignored) {
            // fallback if not a MythicMob
        }
        // fallback to vanilla name
        return formatMobName(e.getType().name());
    }

    /**
     * Converts names like "RANCID_PIG_ZOMBIE" into "Rancid Pig Zombie".
     */
    private static String formatMobName(String rawName) {
        String[] parts = rawName.split("_");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].toLowerCase();
            if (!part.isEmpty()) {
                parts[i] = Character.toUpperCase(part.charAt(0)) + part.substring(1);
            }
        }
        return String.join(" ", parts);
    }
}
