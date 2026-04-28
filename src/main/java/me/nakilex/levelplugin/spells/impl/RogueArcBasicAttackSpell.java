package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.spells.ArcSlashCombatUtil;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class RogueArcBasicAttackSpell implements SpellHandler {
    private final double forwardDistance;
    private final double upOffset;
    private final double damageRadius;
    private final double damage;
    private final double coneRange;
    private final double coneHalfAngleDegrees;
    private final double coneRadius;
    private final double slashTravelScale;
    private final double slashRadiusScale;

    public RogueArcBasicAttackSpell() {
        this(1.2, 1.0, 1.55, 3.4, 3.8, 72.0, 5.0, 1.0, 1.0);
    }

    public RogueArcBasicAttackSpell(double forwardDistance,
                                    double upOffset,
                                    double damageRadius,
                                    double damage,
                                    double coneRange,
                                    double coneHalfAngleDegrees,
                                    double coneRadius,
                                    double slashTravelScale,
                                    double slashRadiusScale) {
        this.forwardDistance = Math.max(0.4, forwardDistance);
        this.upOffset = Math.max(0.0, upOffset);
        this.damageRadius = Math.max(0.4, damageRadius);
        this.damage = Math.max(0.1, damage);
        this.coneRange = Math.max(1.0, coneRange);
        this.coneHalfAngleDegrees = Math.max(10.0, coneHalfAngleDegrees);
        this.coneRadius = Math.max(0.8, coneRadius);
        this.slashTravelScale = Math.max(0.35, slashTravelScale);
        this.slashRadiusScale = Math.max(0.35, slashRadiusScale);
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();

        ArcSlashCombatUtil.strikeForward(caster, forwardDistance, upOffset, damage, damageRadius, slashTravelScale, slashRadiusScale);
        int hits = ArcSlashCombatUtil.applyConeDamage(caster, caster.getLocation().clone().add(0.0, 1.0, 0.0),
                caster.getLocation().getDirection(), coneRange, coneHalfAngleDegrees, coneRadius, damage);
        if (hits == 0
                && caster.getTargetEntity(5) instanceof LivingEntity target
                && !target.equals(caster)
                && !(target instanceof ArmorStand)) {
            SpellEffectUtil.applyDirectSpellDamage(context.plugin(), caster, target, damage, true);
        }
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.65f, 1.35f);
    }
}
