package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.ArcSlashCombatUtil;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import org.bukkit.Location;
import org.bukkit.Particle;
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
        caster.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 0, true, false, true));
        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.8f, 1.2f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!caster.isOnline()) {
                    return;
                }
                Location impact = caster.getLocation().clone().add(0.0, 1.0, 0.0);
                Location orientation = caster.getLocation().clone();
                ArcSlashCombatUtil.strike(caster, impact, orientation, Particle.SWEEP_ATTACK, 6.5, 2.2);
                caster.getWorld().playSound(impact, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.9f, 0.9f);
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
            }
        }.runTaskLater(plugin, 6L);
    }
}
