package me.nakilex.levelplugin.spells.effect.warrior;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.Location;

public class HeroicLeapEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        int agility = StatsManager.getInstance().getStatValue(player, StatType.AGI);

        double damageRadius = Math.min(4.0 + agility * 0.02, 8.0);
        double damageMultiplier = 1.8 + agility * 0.01;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * damageMultiplier;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 1f);

        Location start = player.getLocation();
        Vector direction = start.getDirection().normalize();
        double horizVel = Math.min(2.0 + agility * 0.005, 2.0);
        Vector leapVector = direction.clone().multiply(horizVel);
        leapVector.setY(0.5 + Math.min(agility * 0.001, 0.5));
        player.setVelocity(leapVector);

        player.getWorld().spawnParticle(Particle.FLAME, start, 30, 0.5, 1, 0.5);
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, start, 15, 0.5, 1, 0.5);

        new BukkitRunnable() {
            boolean landed = false;
            @Override
            public void run() {
                if (!landed && player.isOnGround()) {
                    landed = true;
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                    player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 20, 0.5, 1, 0.5);
                    player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 50, 1, 0.5, 1, 0.1);
                    player.getWorld().spawnParticle(Particle.LAVA, player.getLocation(), 20, 0.5, 0.5, 0.5);

                    for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), damageRadius, damageRadius, damageRadius)) {
                        if (!(entity instanceof LivingEntity target) || entity.equals(player)) continue;
                        if (target instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                            continue;
                        SpellUtils.dealWithChat(player, target, damage, "Heroic Leap");
                        Vector kb = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.5);
                        kb.setY(0.5);
                        target.setVelocity(kb);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 6L, 1L);
    }
}
