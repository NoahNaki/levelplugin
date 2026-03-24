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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class WarriorExecutionArcSpell implements SpellHandler {
    private final Main plugin;
    private final int durationTicks;
    private final double strikeDamage;
    private static final CycloneVisualConfig VISUAL_CONFIG = new CycloneVisualConfig();

    public WarriorExecutionArcSpell(Main plugin, int durationTicks, double orbitRadius, double strikeDamage) {
        this.plugin = plugin;
        this.durationTicks = Math.max(12, durationTicks);
        this.strikeDamage = Math.max(0.1, strikeDamage);
        VISUAL_CONFIG.orbitRadius = Math.max(0.6, orbitRadius);
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

        ArmorStand[] stands = new ArmorStand[] {
                spawnCycloneStand(caster, hand),
                spawnCycloneStand(caster, hand)
        };

        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 0.8f, 1.3f);

        new BukkitRunnable() {
            private int ticks;
            private double angle;

            @Override
            public void run() {
                if (!caster.isOnline() || ticks >= durationTicks || !areStandsValid(stands)) {
                    removeStands(stands);
                    cancel();
                    return;
                }
                CycloneVisualConfig cfg = VISUAL_CONFIG.copy();
                angle += cfg.angularSpeed;
                var orbitLocations = new org.bukkit.Location[stands.length];
                for (int i = 0; i < stands.length; i++) {
                    double phase = angle + (i * Math.PI);
                    ArmorStand stand = stands[i];
                    stand.setInvisible(cfg.invisibleStand);
                    Vector offset = new Vector(
                            Math.cos(phase) * cfg.orbitRadius,
                            cfg.baseHeight + (Math.sin(phase * 1.8) * cfg.heightWaveAmplitude),
                            Math.sin(phase) * cfg.orbitRadius
                    );
                    var orbitLocation = caster.getLocation().clone().add(offset);
                    float yaw = (float) Math.toDegrees(Math.atan2(offset.getZ(), offset.getX())) - 90.0f;
                    orbitLocation.setYaw(yaw);
                    stand.teleport(orbitLocation);
                    stand.setRightArmPose(new EulerAngle(
                            Math.toRadians(cfg.armPitchDegrees),
                            Math.toRadians(cfg.armYawDegrees),
                            Math.toRadians(cfg.armRollDegrees)
                    ));
                    caster.getWorld().spawnParticle(Particle.SWEEP_ATTACK, orbitLocation, 1, 0.0, 0.0, 0.0, 0.0);
                    caster.getWorld().spawnParticle(Particle.CRIT, orbitLocation, 3, 0.08, 0.08, 0.08, 0.01);
                    orbitLocations[i] = orbitLocation;
                }

                if (ticks % 4 == 0) {
                    Set<UUID> hitTargetsThisTick = new HashSet<>();
                    for (var orbitLocation : orbitLocations) {
                        for (LivingEntity target : SpellEffectUtil.getLivingTargets(orbitLocation, 1.25,
                                living -> !living.equals(caster) && hitTargetsThisTick.add(living.getUniqueId()))) {
                            SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, strikeDamage, true);
                            Vector away = target.getLocation().toVector().subtract(caster.getLocation().toVector()).setY(0.08);
                            if (away.lengthSquared() > 0.0001) {
                                target.setVelocity(target.getVelocity().multiply(0.74).add(away.normalize().multiply(0.28)));
                            }
                        }
                    }
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.55f, 1.45f);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private ArmorStand spawnCycloneStand(Player caster, ItemStack hand) {
        return caster.getWorld().spawn(caster.getLocation().add(0.0, 1.0, 0.0), ArmorStand.class, armorStand -> {
            CycloneVisualConfig cfg = VISUAL_CONFIG.copy();
            armorStand.setVisible(false);
            armorStand.setMarker(true);
            armorStand.setCollidable(false);
            armorStand.setGravity(false);
            armorStand.setArms(true);
            armorStand.setBasePlate(false);
            armorStand.setInvulnerable(true);
            armorStand.setSilent(true);
            armorStand.getEquipment().setItemInMainHand(hand.clone());
            armorStand.setRightArmPose(new EulerAngle(
                    Math.toRadians(cfg.armPitchDegrees),
                    Math.toRadians(cfg.armYawDegrees),
                    Math.toRadians(cfg.armRollDegrees)
            ));
            armorStand.setInvisible(cfg.invisibleStand);
        });
    }

    private static boolean areStandsValid(ArmorStand[] stands) {
        for (ArmorStand stand : stands) {
            if (stand == null || !stand.isValid()) {
                return false;
            }
        }
        return true;
    }

    private static void removeStands(ArmorStand[] stands) {
        for (ArmorStand stand : stands) {
            if (stand != null && stand.isValid()) {
                stand.remove();
            }
        }
    }

    public static CycloneVisualConfig getVisualConfig() {
        return VISUAL_CONFIG.copy();
    }

    public static void updateVisualConfig(java.util.function.Consumer<CycloneVisualConfig> mutator) {
        if (mutator == null) {
            return;
        }
        CycloneVisualConfig cfg = VISUAL_CONFIG.copy();
        mutator.accept(cfg);
        VISUAL_CONFIG.applyFrom(cfg);
    }

    public static final class CycloneVisualConfig {
        private double orbitRadius = 2.0;
        private double baseHeight = -0.35;
        private double heightWaveAmplitude = 0.0;
        private double angularSpeed = 0.20;
        private double armPitchDegrees = 0.0;
        private double armYawDegrees = 0.0;
        private double armRollDegrees = 90.0;
        private boolean invisibleStand = true;

        public CycloneVisualConfig copy() {
            CycloneVisualConfig copy = new CycloneVisualConfig();
            copy.orbitRadius = orbitRadius;
            copy.baseHeight = baseHeight;
            copy.heightWaveAmplitude = heightWaveAmplitude;
            copy.angularSpeed = angularSpeed;
            copy.armPitchDegrees = armPitchDegrees;
            copy.armYawDegrees = armYawDegrees;
            copy.armRollDegrees = armRollDegrees;
            copy.invisibleStand = invisibleStand;
            return copy;
        }

        private void applyFrom(CycloneVisualConfig other) {
            this.orbitRadius = clamp(other.orbitRadius, 0.4, 3.5);
            this.baseHeight = clamp(other.baseHeight, -1.5, 2.5);
            this.heightWaveAmplitude = clamp(other.heightWaveAmplitude, 0.0, 1.0);
            this.angularSpeed = clamp(other.angularSpeed, 0.1, 1.2);
            this.armPitchDegrees = clamp(other.armPitchDegrees, 0.0, 360.0);
            this.armYawDegrees = clamp(other.armYawDegrees, -180.0, 180.0);
            this.armRollDegrees = clamp(other.armRollDegrees, -180.0, 180.0);
            this.invisibleStand = other.invisibleStand;
        }

        private static double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }

        public double orbitRadius() { return orbitRadius; }
        public double baseHeight() { return baseHeight; }
        public double heightWaveAmplitude() { return heightWaveAmplitude; }
        public double angularSpeed() { return angularSpeed; }
        public double armPitchDegrees() { return armPitchDegrees; }
        public double armYawDegrees() { return armYawDegrees; }
        public double armRollDegrees() { return armRollDegrees; }
        public boolean invisibleStand() { return invisibleStand; }

        public void setOrbitRadius(double orbitRadius) { this.orbitRadius = orbitRadius; }
        public void setBaseHeight(double baseHeight) { this.baseHeight = baseHeight; }
        public void setHeightWaveAmplitude(double heightWaveAmplitude) { this.heightWaveAmplitude = heightWaveAmplitude; }
        public void setAngularSpeed(double angularSpeed) { this.angularSpeed = angularSpeed; }
        public void setArmPitchDegrees(double armPitchDegrees) { this.armPitchDegrees = armPitchDegrees; }
        public void setArmYawDegrees(double armYawDegrees) { this.armYawDegrees = armYawDegrees; }
        public void setArmRollDegrees(double armRollDegrees) { this.armRollDegrees = armRollDegrees; }
        public void setInvisibleStand(boolean invisibleStand) { this.invisibleStand = invisibleStand; }
    }
}
