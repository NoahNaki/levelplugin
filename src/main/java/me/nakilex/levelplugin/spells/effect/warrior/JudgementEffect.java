package me.nakilex.levelplugin.spells.effect.warrior;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Calls down a massive sword from the sky dealing AOE damage on impact.
 */
public class JudgementEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        double damage = ctx.getFinalDamage();
        World world = player.getWorld();

        Location target = player.getTargetBlockExact(20) != null ?
                player.getTargetBlockExact(20).getLocation().add(0.5, 0, 0.5) :
                player.getLocation().add(player.getLocation().getDirection().multiply(8));

        Location spawn = target.clone().add(0, 15, 0);
        ArmorStand sword = (ArmorStand) world.spawnEntity(spawn, EntityType.ARMOR_STAND);
        sword.setInvisible(true);
        sword.setGravity(false);
        sword.setMarker(true);
        sword.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
        sword.setRightArmPose(new org.bukkit.util.EulerAngle(Math.toRadians(-90), 0, 0));

        Vector step = new Vector(0, -1, 0);
        new BukkitRunnable() {
            @Override
            public void run() {
                spawn.add(step);
                sword.teleport(spawn);
                if (spawn.getY() <= target.getY() + 1) {
                    world.playSound(target, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);
                    world.spawnParticle(Particle.EXPLOSION, target, 1);
                    for (Entity e : world.getNearbyEntities(target, 3, 3, 3)) {
                        if (e instanceof LivingEntity le && !le.equals(player)) {
                            if (le instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                                continue;
                            SpellUtils.dealWithChat(player, le, damage, "Judgement");
                        }
                    }
                    sword.remove();
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}
