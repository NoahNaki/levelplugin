package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.spells.WarriorCombatUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class WarriorExecutionArcSpell implements SpellHandler {
    private final Main plugin;
    private final double range;
    private final double halfAngle;
    private final double damage;

    public WarriorExecutionArcSpell(Main plugin, double range, double halfAngle, double damage) {
        this.plugin = plugin;
        this.range = range;
        this.halfAngle = halfAngle;
        this.damage = damage;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        int hits = WarriorCombatUtil.strikeCone(plugin, caster, caster.getEyeLocation(), range, halfAngle, damage, 0.30);
        caster.getWorld().spawnParticle(Particle.SWEEP_ATTACK, caster.getLocation().add(0.0, 1.0, 0.0), 1);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.95f, 0.95f);
        if (hits <= 0) {
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.WARNING,
                    "Execution Arc did not hit a target.");
        }
    }
}
