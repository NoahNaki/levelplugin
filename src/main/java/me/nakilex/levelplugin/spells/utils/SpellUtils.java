package me.nakilex.levelplugin.spells;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.effects.listeners.StatsEffectListener;
import me.nakilex.levelplugin.spells.managers.SpellContextManager;
import me.nakilex.levelplugin.mob.managers.ChatToggleManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

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
     * @param damage     amount of damage to apply
     * @param spellName  name of the spell (displayed in log)
     */
    public static void dealWithChat(
        Player caster,
        LivingEntity target,
        double damage,
        String spellName
    ) {
        ChatToggleManager chatMgr = ChatToggleManager.getInstance();
        boolean wasChatOn = chatMgr.isEnabled(caster);
        // suppress vanilla chat
        chatMgr.setEnabled(caster, false);

        // apply damage without default chat
        SpellContextManager.applySpellDamage(
            caster,
            target,
            damage,
            spellName,
            false
        );

        // restore chat toggle
        chatMgr.setEnabled(caster, wasChatOn);

        // consume crit flag
        boolean wasCrit = StatsEffectListener.consumeLastCrit(caster);
        String hitType = wasCrit ? "critically hit" : "hit";
        ChatColor color = wasCrit ? ChatColor.RED : ChatColor.WHITE;

        if (wasChatOn) {
            // get a clean mob name
            String mobName = getMobDisplayName(target);
            // build and send log message
            String msg = String.format(
                "%s %s %s for %.1f ❤ damage",
                spellName,
                hitType,
                mobName,
                damage
            );
            caster.sendMessage(color + msg);
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
