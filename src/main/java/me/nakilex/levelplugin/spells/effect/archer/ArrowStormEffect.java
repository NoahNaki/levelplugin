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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ArrowStormEffect implements SpellEffect {
    private static final String META_KEY = "ArcherSpell";

    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        int arrowCount = 10;
        double spread = 0.1;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 0.5 * (1 + ctx.getDamagePercent());
        player.getWorld().playSound(player.getLocation(),Sound.ENTITY_ARROW_SHOOT,1f,1f);

        new BukkitRunnable() {
            int fired=0;
            @Override public void run() {
                if (fired++>=arrowCount){cancel();return;}
                Arrow arrow = player.launchProjectile(Arrow.class);
                arrow.setDamage(0);
                Vector dir = player.getLocation().getDirection().clone();
                dir.add(new Vector((Math.random()-0.5)*spread,(Math.random()-0.5)*spread,(Math.random()-0.5)*spread));
                arrow.setVelocity(dir.multiply(2));
                arrow.setCritical(true);
                arrow.setCustomName("ArrowStorm"); arrow.setCustomNameVisible(false);
                arrow.setMetadata(META_KEY,new FixedMetadataValue(Main.getInstance(),player.getUniqueId()));

                new BukkitRunnable() {
                    @Override public void run() {
                        if (!arrow.isValid()||arrow.isDead()){cancel();return;}
                        arrow.getWorld().spawnParticle(Particle.CRIT,arrow.getLocation(),5,0.1,0.1,0.1);
                        for(Entity e:arrow.getNearbyEntities(1,1,1)){
                            if(!(e instanceof LivingEntity le))continue;
                            if(le instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(),p.getUniqueId()))continue;
                            SpellUtils.dealWithChat(player,le,damage,"Arrow Storm");
                            arrow.remove(); cancel(); return;
                        }
                    }
                }.runTaskTimer(Main.getInstance(),0L,1L);
            }
        }.runTaskTimer(Main.getInstance(),0L,5L);
    }
}