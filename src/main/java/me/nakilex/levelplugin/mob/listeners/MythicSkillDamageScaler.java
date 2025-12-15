package me.nakilex.levelplugin.mob.listeners;

import io.lumine.mythic.bukkit.events.MythicDamageEvent;
import me.nakilex.levelplugin.mob.utils.MythicEventUtil;
import me.nakilex.levelplugin.player.attributes.listeners.StatsEffectListener;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.ClassUtil;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.spells.managers.SpellContextManager;
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

    private final java.util.Random random = new java.util.Random();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMythicDamage(MythicDamageEvent event) {
        Player player = MythicEventUtil.resolvePlayer(event);

        if (player == null) return;

        SpellContextManager.Context ctx = SpellContextManager.peek(player.getUniqueId());

        var stats = StatsManager.getInstance().getPlayerStats(player.getUniqueId());

        boolean isMage = ClassUtil.isMageFamily(
                PlayerClassManager.getInstance().getPlayerClass(player));

        int mainStat = isMage
                ? stats.baseIntelligence + stats.bonusIntelligence
                : stats.baseStrength + stats.bonusStrength;
        int totalTec = stats.baseTechnique + stats.bonusTechnique;

        double scaled = event.getDamage() + mainStat * 0.5;
        scaled *= (1.0 + totalTec * 0.003);

        int totalDex = stats.baseDexterity + stats.bonusDexterity;
        double critChance = (double) totalDex / (totalDex + 100.0);
        critChance = Math.max(0.0, Math.min(1.0, critChance));

        boolean isCrit = (ctx != null) ? ctx.isCrit : random.nextDouble() < critChance;
        if (isCrit) scaled *= 2;

        double multiplier = (ctx != null && ctx.basicAttack)
                ? StatsEffectListener.BASIC_ATTACK_MULTIPLIER
                : StatsEffectListener.SPELL_DAMAGE_MULTIPLIER;
        scaled *= multiplier;

        if (debug) {
            me.nakilex.levelplugin.Main.getPlugin().getLogger().info(
                    "[MythicDamageScaler] base=" + event.getDamage() +
                    " scaled=" + scaled +
                    " caster=" + player.getName() +
                    " spellCtx=" + (ctx == null ? "none" : ctx.spellName) +
                    " crit=" + isCrit +
                    " basic=" + (ctx != null && ctx.basicAttack));
        }
        event.setDamage(scaled);
        StatsEffectListener.recordCrit(player, isCrit);
    }
}
