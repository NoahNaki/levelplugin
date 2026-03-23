package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.ArcSlashCombatUtil;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.spells.SpellTargetingUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class RogueNightfallLungeSpell implements SpellHandler {
    private final Main plugin;

    public RogueNightfallLungeSpell(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        LivingEntity target = SpellTargetingUtil.resolveTargetLivingEntity(caster, 16.0, 0.45,
                living -> !living.equals(caster));
        if (target == null) {
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.WARNING,
                    "Look at a target mob for Nightfall Lunge.");
            return;
        }

        new BukkitRunnable() {
            private int wave;

            @Override
            public void run() {
                if (!caster.isOnline() || !target.isValid() || target.isDead() || wave >= 4) {
                    cancel();
                    return;
                }

                Vector toTarget = target.getLocation().toVector().subtract(caster.getLocation().toVector()).setY(0.0);
                if (toTarget.lengthSquared() <= 0.0001) {
                    toTarget = caster.getLocation().getDirection().setY(0.0);
                }
                toTarget.normalize();

                Location origin = caster.getLocation().clone().add(0.0, 1.0, 0.0);
                Location impact = origin.clone().add(toTarget.clone().multiply(2.6 + (wave * 1.3)));

                ArcSlashCombatUtil.applyConeDamage(caster, origin, toTarget,
                        4.0 + wave,
                        34.0 + (wave * 6.0),
                        2.1 + (wave * 0.25),
                        5.2 + (wave * 0.9));
                SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, 4.4 + (wave * 1.1), true);

                caster.getWorld().spawnParticle(Particle.CRIT, impact, 12 + (wave * 3), 0.26, 0.14, 0.26, 0.03);
                caster.getWorld().playSound(impact, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.86f, 0.95f + (wave * 0.08f));
                wave++;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }
}
