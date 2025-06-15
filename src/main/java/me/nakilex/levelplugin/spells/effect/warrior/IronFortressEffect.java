package me.nakilex.levelplugin.spells.effect.warrior;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import de.slikey.effectlib.effect.CylinderEffect;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates a rotating shield of armor stands around the caster that absorbs a
 * few hits. Uses the new SpellCastContext for rune integration.
 */
public class IronFortressEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1f);

        boolean explosive = Boolean.TRUE.equals(ctx.getExtraParam("explosiveShields"));
        boolean share = Boolean.TRUE.equals(ctx.getExtraParam("shieldAllies"));
        boolean reflect = true;

        List<ArmorStand> shields = new ArrayList<>();

        spawnShields(player, shields);
        runShieldParticles(player);

        if (share) {
            for (Player ally : player.getWorld().getPlayers()) {
                if (!ally.equals(player) && ally.getLocation().distanceSquared(player.getLocation()) <= 9) {
                    spawnShields(ally, shields);
                    runShieldParticles(ally);
                }
            }
        }

        new BukkitRunnable() {
            double angle = 0;
            @Override
            public void run() {
                if (!player.isOnline() || shields.isEmpty()) {
                    shields.forEach(Entity::remove);
                    cancel();
                    return;
                }
                angle += Math.PI / 60;
                for (int i = 0; i < shields.size(); i++) {
                    ArmorStand shield = shields.get(i);
                    double radians = angle + (2 * Math.PI * i / shields.size());
                    double x = Math.cos(radians) * 2;
                    double z = Math.sin(radians) * 2;
                    Location loc = player.getLocation().clone().add(x, 1, z);
                    shield.teleport(loc);
                    float yaw = (float) Math.toDegrees(Math.atan2(player.getLocation().getZ() - loc.getZ(),
                                                                  player.getLocation().getX() - loc.getX()));
                    shield.setRotation(yaw, 0);
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 1L);

        new BukkitRunnable() {
            @Override
            public void run() {
                shields.forEach(Entity::remove);
                shields.clear();
            }
        }.runTaskLater(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 100L);

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onEntityDamage(EntityDamageEvent event) {
                if (!(event.getEntity() instanceof Player p) || !p.equals(player)) return;
                if (shields.isEmpty()) return;

                event.setCancelled(true);
                ArmorStand shield = shields.remove(0);
                shield.getWorld().playSound(shield.getLocation(), Sound.ITEM_SHIELD_BREAK, 1f, 1f);
                if (explosive) {
                    shield.getWorld().createExplosion(shield.getLocation(), 2f, false, false);
                }
                if (reflect && event.getDamageSource() instanceof LivingEntity attacker) {
                    attacker.damage(event.getDamage() * 0.3, player);
                }
                shield.remove();

                if (shields.isEmpty()) HandlerList.unregisterAll(this);
            }
        }, Bukkit.getPluginManager().getPlugin("LevelPlugin"));
    }

    private void spawnShields(Player target, List<ArmorStand> list) {
        for (int i = 0; i < 4; i++) {
            ArmorStand stand = (ArmorStand) target.getWorld().spawnEntity(target.getLocation(), EntityType.ARMOR_STAND);
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setSmall(true);
            stand.setGravity(false);
            stand.setArms(true);
            stand.setRightArmPose(new EulerAngle(Math.toRadians(-90), 0, 0));
            stand.getEquipment().setItemInMainHand(new org.bukkit.inventory.ItemStack(Material.SHIELD));
            list.add(stand);
        }
    }

    private void runShieldParticles(Player target) {
        CylinderEffect effect = new CylinderEffect(Main.getInstance().getEffectManager());
        effect.setLocation(target.getLocation());
        effect.radius = 2f;
        effect.height = 2.5f;
        effect.particle = Particle.TOTEM_OF_UNDYING;
        effect.period = 2;
        effect.iterations = 50;
        effect.start();
    }
}
