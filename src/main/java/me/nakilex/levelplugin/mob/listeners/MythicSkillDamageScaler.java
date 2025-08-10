package me.nakilex.levelplugin.mob.listeners;

import io.lumine.mythic.bukkit.events.MythicDamageEvent;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Scales damage dealt by MythicMobs skills according to the caster's stats.
 * This catches MythicDamageEvent so we can boost skill damage without editing
 * the skill configs.
 */
public class MythicSkillDamageScaler implements Listener {

    private final boolean debug = me.nakilex.levelplugin.Main.getInstance()
            .getCustomConfig().getBoolean("debug.mythic-skill-damage", false);

    private Player resolvePlayer(Object casterObj) {
        var entity = me.nakilex.levelplugin.mob.utils.MythicMobModifier.toBukkitEntity(casterObj);
        return entity instanceof Player p ? p : null;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMythicDamage(MythicDamageEvent event) {
        Player player = null;

        // First try direct caster
        player = resolvePlayer(event.getCaster());

        if (player == null) {
            // Fallback: try trigger -> caster (for projectile skills)
            try {
                Object trigger = event.getClass().getMethod("getTrigger").invoke(event);
                if (trigger != null) {
                    Object trigCaster = trigger.getClass().getMethod("getCaster").invoke(trigger);
                    player = resolvePlayer(trigCaster);
                }
            } catch (Exception ignore) {
            }
        }

        if (player == null) {
            // Final attempt: method getShooter if present
            try {
                Object shooter = event.getClass().getMethod("getShooter").invoke(event);
                if (shooter instanceof Player p) player = p;
            } catch (Exception ignore) {
            }
        }

        if (player == null) return;

        var stats = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        double strength = stats.baseStrength + stats.bonusStrength;
        double scaled = event.getDamage() + strength * 0.5;
        if (debug) {
            me.nakilex.levelplugin.Main.getPlugin().getLogger().info(
                    "[MythicDamageScaler] base=" + event.getDamage() + " scaled=" + scaled +
                    " caster=" + player.getName());
        }
        event.setDamage(scaled);
    }
}
