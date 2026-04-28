package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WarriorExecutionArcSpell implements SpellHandler {
    private static final Map<UUID, CycloneGuardState> ACTIVE_CYCLONES = new ConcurrentHashMap<>();

    private final Main plugin;
    private final int durationTicks;
    private final double strikeDamage;
    private static final CycloneVisualConfig VISUAL_CONFIG = new CycloneVisualConfig();
    private static final double CYCLONE_INCOMING_DAMAGE_MULTIPLIER = 0.5;
    private static final double CYCLONE_PULL_STRENGTH = 0.24;
    private static final double CYCLONE_CONTINUOUS_DAMAGE = 1.3;

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
            hand = new ItemStack(Material.IRON_AXE);
        }

        ArmorStand[] stands = new ArmorStand[] {
                spawnCycloneStand(caster, hand),
                spawnCycloneStand(caster, hand)
        };

        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 0.8f, 1.3f);
        ACTIVE_CYCLONES.put(caster.getUniqueId(), new CycloneGuardState(System.currentTimeMillis() + (durationTicks * 50L),
                CYCLONE_INCOMING_DAMAGE_MULTIPLIER));

        new BukkitRunnable() {
            private int ticks;
            private double angle;

            @Override
            public void run() {
                if (!caster.isOnline() || ticks >= durationTicks || !areStandsValid(stands)) {
                    removeStands(stands);
                    ACTIVE_CYCLONES.remove(caster.getUniqueId());
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
                    spawnOrbitSwirlParticles(caster, orbitLocation, phase, ticks);
                    orbitLocations[i] = orbitLocation;
                }

                double pullRadius = Math.max(1.4, cfg.orbitRadius + 0.9);
                Set<UUID> affectedTargets = new HashSet<>();
                for (LivingEntity target : SpellEffectUtil.getLivingTargets(caster.getLocation(), pullRadius,
                        living -> !living.equals(caster) && affectedTargets.add(living.getUniqueId()))) {
                    Vector pull = caster.getLocation().toVector().subtract(target.getLocation().toVector()).setY(0.0);
                    if (pull.lengthSquared() > 0.0001) {
                        target.setVelocity(target.getVelocity().multiply(0.68).add(pull.normalize().multiply(CYCLONE_PULL_STRENGTH).setY(0.04)));
                    }
                }

                if (ticks % 3 == 0) {
                    for (LivingEntity target : SpellEffectUtil.getLivingTargets(caster.getLocation(), pullRadius,
                            living -> !living.equals(caster))) {
                        SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, CYCLONE_CONTINUOUS_DAMAGE, true);
                    }
                }

                if (ticks % 6 == 0) {
                    Set<UUID> hitTargetsThisTick = new HashSet<>();
                    for (var orbitLocation : orbitLocations) {
                        for (LivingEntity target : SpellEffectUtil.getLivingTargets(orbitLocation, 1.25,
                                living -> !living.equals(caster) && hitTargetsThisTick.add(living.getUniqueId()))) {
                            SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, strikeDamage, true);
                        }
                    }
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.55f, 1.45f);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> ACTIVE_CYCLONES.remove(caster.getUniqueId()),
                durationTicks + 1L);
    }


    private static void spawnOrbitSwirlParticles(Player caster, org.bukkit.Location orbitLocation, double phase, int ticks) {
        if (caster == null || caster.getWorld() == null || orbitLocation == null) {
            return;
        }
        double spin = phase + (ticks * 0.32);
        for (int i = 0; i < 3; i++) {
            double subPhase = spin + (i * (Math.PI * 2.0 / 3.0));
            double swirlRadius = 0.32;
            double x = Math.cos(subPhase) * swirlRadius;
            double z = Math.sin(subPhase) * swirlRadius;
            double y = 0.18 + (i * 0.14);
            var point = orbitLocation.clone().add(x, y, z);
            caster.getWorld().spawnParticle(Particle.DUST, point, 1, 0.0, 0.0, 0.0, 0.0,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(184, 236, 255), 1.0f));
            caster.getWorld().spawnParticle(Particle.CRIT, point, 1, 0.01, 0.01, 0.01, 0.0);
        }
    }

    public static double getIncomingDamageMultiplier(Player player) {
        if (player == null) {
            return 1.0;
        }
        CycloneGuardState state = ACTIVE_CYCLONES.get(player.getUniqueId());
        if (state == null) {
            return 1.0;
        }
        if (System.currentTimeMillis() > state.expiresAtMs()) {
            ACTIVE_CYCLONES.remove(player.getUniqueId());
            return 1.0;
        }
        return state.incomingDamageMultiplier();
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

    private record CycloneGuardState(long expiresAtMs, double incomingDamageMultiplier) {
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
