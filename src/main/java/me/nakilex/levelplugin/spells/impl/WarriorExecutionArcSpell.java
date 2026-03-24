package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.util.EulerAngle;

public class WarriorExecutionArcSpell implements SpellHandler {
    private final Main plugin;
    private final int durationTicks;
    private final double orbitRadius;
    private final double strikeDamage;

    public WarriorExecutionArcSpell(Main plugin, int durationTicks, double orbitRadius, double strikeDamage) {
        this.plugin = plugin;
        this.durationTicks = Math.max(12, durationTicks);
        this.orbitRadius = Math.max(0.6, orbitRadius);
        this.strikeDamage = Math.max(0.1, strikeDamage);
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        ItemStack hand = caster.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir() || hand.getType() == Material.AIR) {
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.WARNING,
                    "Hold your weapon to cast Cyclone Brand.");
            return;
        }

        ArmorStand stand = caster.getWorld().spawn(caster.getLocation().add(0.0, 1.0, 0.0), ArmorStand.class, armorStand -> {
            armorStand.setVisible(false);
            armorStand.setMarker(true);
            armorStand.setGravity(false);
            armorStand.setArms(true);
            armorStand.setBasePlate(false);
            armorStand.setInvulnerable(true);
            armorStand.setSilent(true);
            armorStand.getEquipment().setItemInMainHand(hand.clone());
            armorStand.setRightArmPose(new EulerAngle(Math.toRadians(270), 0.0, 0.0));
        });

        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 0.8f, 1.3f);

        new BukkitRunnable() {
            private int ticks;
            private double angle;

            @Override
            public void run() {
                if (!caster.isOnline() || ticks >= durationTicks || !stand.isValid()) {
                    stand.remove();
                    cancel();
                    return;
                }
                angle += 0.52;
                Vector offset = new Vector(Math.cos(angle) * orbitRadius, 1.1 + (Math.sin(angle * 1.8) * 0.22), Math.sin(angle) * orbitRadius);
                var orbitLocation = caster.getLocation().clone().add(offset);
                float yaw = (float) Math.toDegrees(Math.atan2(offset.getZ(), offset.getX())) - 90.0f;
                orbitLocation.setYaw(yaw);
                stand.teleport(orbitLocation);
                stand.setRightArmPose(new EulerAngle(Math.toRadians(270), 0.0, 0.0));
                caster.getWorld().spawnParticle(Particle.SWEEP_ATTACK, orbitLocation, 1, 0.0, 0.0, 0.0, 0.0);
                caster.getWorld().spawnParticle(Particle.CRIT, orbitLocation, 3, 0.08, 0.08, 0.08, 0.01);

                if (ticks % 4 == 0) {
                    for (LivingEntity target : SpellEffectUtil.getLivingTargets(orbitLocation, 1.25,
                            living -> !living.equals(caster))) {
                        SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, strikeDamage, true);
                        Vector away = target.getLocation().toVector().subtract(caster.getLocation().toVector()).setY(0.08);
                        if (away.lengthSquared() > 0.0001) {
                            target.setVelocity(target.getVelocity().multiply(0.74).add(away.normalize().multiply(0.28)));
                        }
                    }
                    caster.getWorld().playSound(orbitLocation, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.55f, 1.45f);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
