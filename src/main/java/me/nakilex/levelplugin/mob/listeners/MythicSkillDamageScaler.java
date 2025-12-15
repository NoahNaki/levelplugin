package me.nakilex.levelplugin.mob.listeners;

import io.lumine.mythic.bukkit.events.MythicDamageEvent;
import me.nakilex.levelplugin.mob.utils.MythicEventUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.ClassUtil;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMythicDamage(MythicDamageEvent event) {
        Player player = MythicEventUtil.resolvePlayer(event);

        if (player == null) return;

        var stats = StatsManager.getInstance().getPlayerStats(player.getUniqueId());

        boolean isMage = ClassUtil.isMageFamily(
                PlayerClassManager.getInstance().getPlayerClass(player));

        int mainStat = isMage
                ? stats.baseIntelligence + stats.bonusIntelligence
                : stats.baseStrength + stats.bonusStrength;
        int totalTec = stats.baseTechnique + stats.bonusTechnique;

        double scaled = event.getDamage() + mainStat * 0.5;
        scaled *= (1.0 + totalTec * 0.003);

        if (debug) {
            me.nakilex.levelplugin.Main.getPlugin().getLogger().info(
                    "[MythicDamageScaler] base=" + event.getDamage() +
                    " scaled=" + scaled +
                    " caster=" + player.getName());
        }
        event.setDamage(scaled);
    }
}
