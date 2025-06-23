package me.nakilex.levelplugin.spells.effect;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
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

        // Calculate scaled damage using player stats and any modifiers
        var stats = StatsManager.getInstance().getPlayerStats(caster.getUniqueId());
        double strength = stats.baseStrength + stats.bonusStrength;
        double damage = ctx.getFinalDamage() + strength * 0.5;

        // Compute damage for our own stats system. MythicBukkit's helper does
        // not let us pass custom variables, so we simply invoke the skill and
        // let StatsEffectListener apply the finalDamage value.
        MythicBukkit.inst().getAPIHelper().castSkill(caster, skill);
    }
}
