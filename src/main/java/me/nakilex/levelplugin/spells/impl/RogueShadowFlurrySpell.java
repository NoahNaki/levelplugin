package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.ArcSlashCombatUtil;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class RogueShadowFlurrySpell implements SpellHandler {
    private final Main plugin;

    public RogueShadowFlurrySpell(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        Vector forward = caster.getLocation().getDirection().setY(0.0);
        if (forward.lengthSquared() <= 0.0001) {
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.WARNING,
                    "No forward direction for Shadow Flurry.");
            return;
        }

        Vector direction = forward.normalize();
        Vector right = new Vector(0, 1, 0).crossProduct(direction).normalize();
        Location origin = caster.getLocation().clone().add(0.0, 1.0, 0.0);

        new BukkitRunnable() {
            private int hit;

            @Override
            public void run() {
                if (!caster.isOnline() || hit >= 5) {
                    cancel();
                    return;
                }

                double travel = 1.4 + (hit * 0.9);
                double sway = (hit % 2 == 0 ? 0.45 : -0.45) + (hit * 0.04);
                Location impact = origin.clone()
                        .add(direction.clone().multiply(travel))
                        .add(right.clone().multiply(sway));
                Location orientation = caster.getLocation().clone();
                orientation.setDirection(direction.clone());

                double damage = 4.5 + (hit * 0.55);
                ArcSlashCombatUtil.strike(caster, impact, orientation, damage, 2.2 + (hit * 0.08));
                caster.getWorld().spawnParticle(Particle.SMOKE, impact, 10 + hit * 2,
                        0.28, 0.20, 0.28, 0.003);
                caster.getWorld().spawnParticle(Particle.CRIT, impact, 9 + hit * 2,
                        0.24, 0.12, 0.24, 0.02);
                caster.getWorld().playSound(impact, Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                        0.84f, 1.15f + (hit * 0.08f));
                hit++;
            }
        }.runTaskTimer(plugin, 0L, 3L);
    }
}
