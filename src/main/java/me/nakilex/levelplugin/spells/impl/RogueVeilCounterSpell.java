package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.ArcSlashCombatUtil;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class RogueVeilCounterSpell implements SpellHandler {
    private final Main plugin;

    public RogueVeilCounterSpell(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        caster.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 50, 0, true, false, true));
        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.8f, 1.2f);
        caster.getWorld().spawnParticle(org.bukkit.Particle.SMOKE, caster.getLocation().clone().add(0.0, 1.0, 0.0),
                16, 0.35, 0.5, 0.35, 0.005);

        new BukkitRunnable() {
            private int wave;

            @Override
            public void run() {
                if (!caster.isOnline() || wave >= 2) {
                    cancel();
                    return;
                }
                Location impact = caster.getLocation().clone().add(0.0, 1.0, 0.0);
                Location orientation = caster.getLocation().clone();
                double damage = wave == 0 ? 4.0 : 6.6;
                ArcSlashCombatUtil.strike(caster, impact, orientation, damage, 2.3);
                caster.getWorld().playSound(impact, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.9f, wave == 0 ? 1.05f : 0.88f);
                for (LivingEntity target : SpellEffectUtil.getLivingTargets(impact, 2.2,
                        living -> !living.equals(caster))) {
                    Vector push = target.getLocation().toVector().subtract(caster.getLocation().toVector()).setY(0.0);
                    if (push.lengthSquared() <= 0.0001) {
                        push = new Vector(0.0, 0.0, 1.0);
                    }
                    target.setVelocity(target.getVelocity().multiply(0.5)
                            .add(push.normalize().multiply(0.55))
                            .add(new Vector(0.0, 0.16, 0.0)));
                }
                wave++;
            }
        }.runTaskTimer(plugin, 6L, 6L);
    }
}
