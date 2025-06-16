package me.nakilex.levelplugin.spells.effect.archer;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import de.slikey.effectlib.effect.CloudEffect;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.*;

public class GrappleHookEffect implements SpellEffect {
    private static final String META_KEY = "ArcherSpell";
    private static final Set<UUID> cooldown = new HashSet<>();

    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        UUID id = player.getUniqueId();
        if (!player.isOnGround()) { player.sendMessage(ChatColor.RED+"Land before using Grapple Hook!"); return; }
        if (cooldown.contains(id))   { player.sendMessage(ChatColor.RED+"Grapple Hook recharging..."); return; }
        cooldown.add(id);
        player.getWorld().playSound(player.getLocation(),Sound.ENTITY_ENDER_PEARL_THROW,1f,1f);
        Snowball hook = player.launchProjectile(Snowball.class, player.getLocation().getDirection().multiply(2));
        hook.setCustomName("GrappleHook"); hook.setCustomNameVisible(false);
        hook.setMetadata(META_KEY,new FixedMetadataValue(Main.getInstance(),id));

        new BukkitRunnable() {
            @Override public void run() {
                if (!hook.isValid()||hook.isDead()){cancel();return;}
                hook.getWorld().spawnParticle(Particle.WITCH,hook.getLocation(),5,0.1,0.1,0.1);
                boolean hit = !hook.getNearbyEntities(1,1,1).isEmpty()||!hook.getLocation().getBlock().getType().equals(Material.AIR);
                if (hit) {
                    Location loc=hook.getLocation();
                    Vector pull=loc.toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.5);
                    player.setVelocity(pull.add(new Vector(0,0.5,0)));

                    CloudEffect cloud = new CloudEffect(Main.getInstance().getEffectManager());
                    cloud.setEntity(player);
                    cloud.cloudParticle = Particle.CLOUD;
                    cloud.mainParticle = Particle.END_ROD;
                    cloud.iterations = 40;
                    cloud.start();

                    handleGlide(player);
                    hook.remove(); cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(),0L,1L);
    }

    private void handleGlide(Player player) {
        new BukkitRunnable() {
            boolean slam=false;
            @Override public void run() {
                if (player.isOnGround()) {
                    cooldown.remove(player.getUniqueId());
                    if (slam) performSlam(player);
                    cancel(); return;
                }
                if (slam) {
                    Vector v=player.getVelocity(); v.setY(Math.max(v.getY()-0.5,-2.5)); player.setVelocity(v);
                } else {
                    Vector v=player.getVelocity().multiply(0.9); v.setY(Math.max(player.getVelocity().getY()-0.05,-0.1)); player.setVelocity(v);
                    if (player.isSneaking()) { slam=true; player.getWorld().playSound(player.getLocation(),Sound.ENTITY_WITHER_BREAK_BLOCK,1f,0.8f); player.spawnParticle(Particle.SMOKE,player.getLocation(),20,0.5,1,0.5); player.setVelocity(new Vector(0,-2,0)); }
                }
            }
        }.runTaskTimer(Main.getInstance(),10L,1L);
    }

    private void performSlam(Player player) {
        double radius=5.0; double damage=player.getAttribute(Attribute.ATTACK_DAMAGE).getValue()*2.0;
        World w=player.getWorld(); w.playSound(player.getLocation(),Sound.ENTITY_GENERIC_EXPLODE,1f,1f); w.spawnParticle(Particle.EXPLOSION,player.getLocation(),20);
        for(Entity e:w.getNearbyEntities(player.getLocation(),radius,radius,radius)){
            if(!(e instanceof LivingEntity le)||le.equals(player))continue;
            if(le instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(),p.getUniqueId())) continue;
            SpellUtils.dealWithChat(player,le,damage,"Grapple Hook");
            Vector kb=le.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.5); kb.setY(0.5); le.setVelocity(kb);
        }
    }
}