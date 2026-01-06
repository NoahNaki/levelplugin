package me.nakilex.levelplugin.spells.effect;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.context.SpellCastContextCompat;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.spells.managers.SpellContextManager;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * Simple effect that triggers a MythicMobs skill by name.
 */
public class MythicSkillEffect implements SpellEffect {
    private final List<String> skills;

    public MythicSkillEffect(String skill) {
        this.skills = List.of(skill);
    }

    public MythicSkillEffect(String... skills) {
        this.skills = Arrays.asList(skills);
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
        SpellContextManager.setPending(
                caster.getUniqueId(),
                ctx.getBaseSpell().getDisplayName(),
                isCrit,
                basic,
                false);

        me.nakilex.levelplugin.Main.getPlugin().getLogger().info(
                "[MythicSkillEffect] skills=" + skills + " cast by " + caster.getName());

        // Cast skill normally; damage & stat scaling handled in StatsEffectListener
        boolean success = false;
        for (String skill : skills) {
            boolean castResult;
            try {
                castResult = MythicBukkit.inst().getAPIHelper().castSkill(caster, skill);
            } catch (NoSuchMethodError e) {
                castResult = MythicBukkit.inst().getAPIHelper().castSkill(caster, skill);
            }
            success = success || castResult;
            me.nakilex.levelplugin.Main.getPlugin().getLogger().info(
                    "[MythicSkillEffect] result=" + castResult
                            + " skill=" + skill
                            + " sneaking=" + caster.isSneaking());
        }
        me.nakilex.levelplugin.Main.getPlugin().getLogger().info(
                "[MythicSkillEffect] success=" + success + " skills=" + skills);
        SpellCastContextCompat.markSuccess(ctx, success);
    }
}
