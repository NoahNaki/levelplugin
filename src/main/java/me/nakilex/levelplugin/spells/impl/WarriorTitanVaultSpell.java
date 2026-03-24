package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.spells.WarriorCombatUtil;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class WarriorTitanVaultSpell implements SpellHandler {
    private final Main plugin;
    private final double forwardSpeed;
    private final double upwardSpeed;
    private final double impactRadius;
    private final double impactDamage;

    public WarriorTitanVaultSpell(Main plugin,
                                  double forwardSpeed,
                                  double upwardSpeed,
                                  double impactRadius,
                                  double impactDamage) {
        this.plugin = plugin;
        this.forwardSpeed = Math.max(0.1, forwardSpeed);
        this.upwardSpeed = Math.max(0.1, upwardSpeed);
        this.impactRadius = Math.max(1.0, impactRadius);
        this.impactDamage = Math.max(0.1, impactDamage);
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        Vector direction = caster.getLocation().getDirection().clone().setY(0.0);
        if (direction.lengthSquared() <= 0.0001) {
            direction = caster.getLocation().getDirection().clone();
        }
        Vector launch = direction.normalize().multiply(forwardSpeed).setY(upwardSpeed);
        WarriorCombatUtil.leapAndSlam(plugin, caster, launch, 22, impactRadius, impactDamage, null);
    }
}
