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
        forward.normalize();
        Vector right = new Vector(0, 1, 0).crossProduct(forward).normalize();

        new BukkitRunnable() {
            private int wave;

            @Override
            public void run() {
                if (!caster.isOnline() || wave >= 3) {
                    cancel();
                    return;
                }

                Location base = caster.getLocation().clone().add(0.0, 1.0, 0.0)
                        .add(forward.clone().multiply(1.8 + (wave * 0.8)));
                Location orientation = caster.getLocation().clone();
                orientation.setDirection(forward);

                ArcSlashCombatUtil.strike(caster, base, orientation, 5.0 + wave, 2.1);
                ArcSlashCombatUtil.strike(caster, base.clone().add(right.clone().multiply(1.05)), orientation, 4.4 + wave, 1.85);
                ArcSlashCombatUtil.strike(caster, base.clone().add(right.clone().multiply(-1.05)), orientation, 4.4 + wave, 1.85);
                ArcSlashCombatUtil.applyConeDamage(caster, base, forward, 3.8 + (wave * 0.6), 60.0, 2.5, 4.8 + (wave * 0.7));

                caster.getWorld().spawnParticle(Particle.CRIT, base, 14, 0.34, 0.18, 0.34, 0.03);
                caster.getWorld().playSound(base, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.92f, 1.0f + (wave * 0.08f));
                wave++;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }
}
