package me.nakilex.levelplugin.spells.effect.warrior;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Plants a banner that periodically buffs nearby allies.
 */
public class RallyBannerEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        World world = player.getWorld();
        Location loc = player.getLocation();

        ArmorStand stand = world.spawn(loc, ArmorStand.class, a -> {
            a.setInvisible(true);
            a.setGravity(false);
            a.setMarker(true);
            a.setInvulnerable(true);
            a.getEquipment().setHelmet(new ItemStack(Material.WHITE_BANNER));
        });

        world.playSound(loc, Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 200 || stand.isDead()) {
                    stand.remove();
                    cancel();
                    return;
                }
                world.spawnParticle(Particle.NOTE, stand.getLocation().add(0,2,0), 2, 0.3,0.3,0.3, 0.2);
                for (Entity e : stand.getNearbyEntities(6,6,6)) {
                    if (e instanceof Player p) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 0, true, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0, true, false));
                    }
                }
                ticks += 20;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 20L);
    }
}
