package me.nakilex.levelplugin.spells.effect;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.context.SpellCastContextCompat;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.spells.managers.SpellContextManager;
import org.bukkit.entity.Player;

/**
 * Simple effect that triggers a MythicMobs skill by name.
 */
public class MythicSkillEffect implements SpellEffect {
    private final String skill;

    public MythicSkillEffect(String skill) {
        this.skill = skill;
    }

    @Override
    public void apply(SpellCastContext ctx) {
        Player caster = ctx.getPlayer();

        // Grab player stats to roll crit chance once, allowing StatsEffectListener
        // to reuse the result when applying final damage
        var stats = StatsManager.getInstance().getPlayerStats(caster.getUniqueId());
        int totalDex = stats.baseDexterity + stats.bonusDexterity;
        double critChance = (double) totalDex / (totalDex + 100.0);
        boolean isCrit = Math.random() < critChance;

        boolean basic = "BASIC_ATTACK".equals(ctx.getBaseSpell().getCombo());
        SpellContextManager.setPending(caster.getUniqueId(), ctx.getBaseSpell().getDisplayName(), isCrit, basic, false);

        me.nakilex.levelplugin.Main.getPlugin().getLogger().info(
                "[MythicSkillEffect] skill=" + skill + " cast by " + caster.getName());

        // Cast skill normally; damage & stat scaling handled in StatsEffectListener
        boolean success;
        try {
            success = MythicBukkit.inst().getAPIHelper().castSkill(caster, skill);
        } catch (NoSuchMethodError e) {
            success = MythicBukkit.inst().getAPIHelper().castSkill(caster, skill);
        }
        SpellCastContextCompat.markSuccess(ctx, success);
    }
}
