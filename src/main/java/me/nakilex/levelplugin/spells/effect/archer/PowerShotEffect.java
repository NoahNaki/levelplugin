package me.nakilex.levelplugin.spells.effect.archer;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PowerShotEffect implements SpellEffect {
    private static final String META_KEY = "ArcherSpell";

    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        double radius = 5.0;
        double baseDamage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 0.8;
        double damage = baseDamage * (1 + ctx.getDamagePercent());
        int arrowCount = 30;
        int durationTicks = 100;

        Location targetLoc = player.getTargetBlockExact(20) != null
            ? player.getTargetBlockExact(20).getLocation().add(0.5, 0.5, 0.5)
            : player.getLocation().add(player.getLocation().getDirection().normalize().multiply(5));

        player.getWorld().playSound(targetLoc, Sound.ENTITY_ARROW_SHOOT, 1f, 1f);

        new BukkitRunnable() {
            int spawned = 0;
            @Override
            public void run() {
                if (spawned++ >= arrowCount) { cancel(); return; }

                double xOff = (Math.random() - 0.5) * 2 * radius;
                double zOff = (Math.random() - 0.5) * 2 * radius;
                Location drop = targetLoc.clone().add(xOff, 15, zOff);
                World w = drop.getWorld();
                w.spawnParticle(Particle.CLOUD, drop, 10, 0.3, 0.3, 0.3, 0.02);
                w.playSound(drop, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.5f, 1.5f);

                Arrow arrow = w.spawnArrow(drop, new Vector(0, -3, 0), 1.5f, 0f);
                arrow.setShooter(player);
                arrow.setCustomName("PowerShot");
                arrow.setCustomNameVisible(false);
                arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                arrow.setMetadata(META_KEY, new FixedMetadataValue(Main.getInstance(), player.getUniqueId()));

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!arrow.isValid() || arrow.isDead()) { cancel(); return; }
                        for (Entity e : arrow.getNearbyEntities(1,1,1)) {
                            if (!(e instanceof LivingEntity le) || le == player) continue;
                            if (le instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId())) continue;
                            SpellUtils.dealWithChat(player, le, damage, "Power Shot");
                            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                        }
                    }
                }.runTaskTimer(Main.getInstance(), 0L, 1L);
            }
        }.runTaskTimer(Main.getInstance(), 0L, durationTicks / arrowCount);
    }
}