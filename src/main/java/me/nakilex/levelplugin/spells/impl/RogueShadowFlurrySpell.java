package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.spells.SpellTargetingUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class RogueShadowFlurrySpell implements SpellHandler {
    private static final int DART_COUNT = 5;

    private final Main plugin;

    public RogueShadowFlurrySpell(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        LivingEntity target = SpellTargetingUtil.resolveTargetLivingEntity(caster, 16.0, 0.45,
                living -> !living.equals(caster));
        if (target == null) {
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.WARNING,
                    "Look at a target mob for Shadow Flurry.");
            return;
        }

        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.55f, 0.75f);

        for (int i = 0; i < DART_COUNT; i++) {
            int shotIndex = i;
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> launchShadowDart(caster, target, shotIndex), shotIndex * 3L);
        }
    }

    private void launchShadowDart(Player caster, LivingEntity target, int shotIndex) {
        if (!caster.isOnline() || !target.isValid() || target.isDead()) {
            return;
        }

        Location point = caster.getEyeLocation().clone();
        Vector direction = target.getLocation().clone().add(0.0, 0.9, 0.0)
                .toVector().subtract(point.toVector()).normalize();

        new BukkitRunnable() {
            private int step;

            @Override
            public void run() {
                if (!caster.isOnline() || step >= 16) {
                    cancel();
                    return;
                }
                if (!target.isValid() || target.isDead()) {
                    cancel();
                    return;
                }

                Location previous = point.clone();
                point.add(direction.clone().multiply(0.9));
                point.getWorld().spawnParticle(Particle.DUST, point, 2, 0.06, 0.06, 0.06,
                        new Particle.DustOptions(Color.fromRGB(22, 22, 22), 1.0f));
                point.getWorld().spawnParticle(Particle.CRIT, point, 2, 0.06, 0.06, 0.06, 0.01);

                LivingEntity hit = SpellTargetingUtil.rayTraceLivingEntity(previous,
                        point.toVector().subtract(previous.toVector()),
                        0.35,
                        living -> !living.equals(caster));
                if (hit != null) {
                    double damage = 5.2 + (shotIndex * 0.6);
                    if (shotIndex == DART_COUNT - 1) {
                        damage += 2.4;
                    }
                    SpellEffectUtil.applyDirectSpellDamage(plugin, caster, hit, damage, true);
                    if (shotIndex == DART_COUNT - 1) {
                        SpellEffectUtil.applyStun(hit, 14, true);
                    }
                    hit.getWorld().playSound(hit.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER,
                            0.85f, 1.15f + (shotIndex * 0.03f));
                    cancel();
                    return;
                }

                if (point.getBlock().isSolid()) {
                    cancel();
                    return;
                }
                step++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
