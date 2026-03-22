package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.ArcSlashCombatUtil;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.spells.SpellTargetingUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class RoguePhantomCrossSpell implements SpellHandler {
    private final Main plugin;

    public RoguePhantomCrossSpell(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        Location target = SpellTargetingUtil.resolveTargetGround(caster, 18.0);
        if (target == null) {
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.WARNING,
                    "No target in sight for Phantom Cross.");
            return;
        }

        Vector baseForward = target.toVector().subtract(caster.getLocation().toVector()).setY(0.0);
        if (baseForward.lengthSquared() <= 0.0001) {
            baseForward = caster.getLocation().getDirection().setY(0.0);
        }
        final Vector forward = baseForward.normalize();
        final Vector right = new Vector(0, 1, 0).crossProduct(forward).normalize();

        new BukkitRunnable() {
            private int pass;

            @Override
            public void run() {
                if (!caster.isOnline() || pass >= 2) {
                    cancel();
                    return;
                }
                double side = pass == 0 ? 1.15 : -1.15;
                Location impact = target.clone().add(right.clone().multiply(side));
                Location orientation = caster.getLocation().clone();
                orientation.setDirection(forward.clone());
                double damage = pass == 0 ? 5.2 : 7.4;
                ArcSlashCombatUtil.strike(caster, impact, orientation, Particle.SWEEP_ATTACK, damage, 2.1);
                caster.getWorld().playSound(impact, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.95f, pass == 0 ? 1.0f : 1.35f);
                pass++;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }
}
