package me.nakilex.levelplugin.spells.utils;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.listeners.StatsEffectListener;
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

import java.util.logging.Logger;

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
        Logger logger = Main.getInstance().getLogger();
        ChatToggleManager chatMgr = ChatToggleManager.getInstance();
        boolean wasChatOn = chatMgr.isEnabled(caster);

        // ONLY suppress vanilla chat if it was on
        if (wasChatOn) {
            logger.info("dealWithChat: [" + caster.getName() + "] chat ON, suppressing it");
            chatMgr.setEnabled(caster, false);
        } else {
            logger.info("dealWithChat: [" + caster.getName() + "] chat already OFF, skipping suppress");
        }

        // Prepare the one-off damage listener
        Listener damageListener = new Listener() {
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void onEntityDamage(EntityDamageByEntityEvent ev) {
                // match caster → target
                if (!ev.getEntity().equals(target)) return;
                Player damager = null;
                if (ev.getDamager() instanceof Player p) damager = p;
                else if (ev.getDamager() instanceof Projectile proj
                    && proj.getShooter() instanceof Player shooter) damager = shooter;
                if (damager == null || !damager.equals(caster)) return;

                // log final damage
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

                // cleanup listener
                HandlerList.unregisterAll(this);

                // restore chat if we suppressed it
                if (wasChatOn) {
                    chatMgr.setEnabled(caster, true);
                    logger.info("dealWithChat: [" + caster.getName() + "] restored chat ON");
                }
            }
        };
        Bukkit.getPluginManager().registerEvents(damageListener, Main.getInstance());
        logger.info("dealWithChat: listener registered for " + caster.getName());

        // Apply the damage
        SpellContextManager.applySpellDamage(
            caster, target, rawDamage, spellName, false
        );
        logger.info("dealWithChat: applySpellDamage called for " + caster.getName());

        // Fallback: if we suppressed chat but the listener never fired, restore next tick
        if (wasChatOn) {
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                HandlerList.unregisterAll(damageListener);
                chatMgr.setEnabled(caster, true);
                logger.info("dealWithChat: [FALLBACK] restored chat ON for " + caster.getName());
            });
        }
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
