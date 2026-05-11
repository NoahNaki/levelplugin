package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.spells.SpellTargetingUtil;
import me.nakilex.levelplugin.utils.CombatTargetUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DeckFireballSpell implements SpellHandler {
    private static final double MAX_RANGE = 34.0;
    private static final double HIT_RADIUS = 0.75;
    private static final double TICK_DISTANCE_SCALE = 1.0;

    private final Main plugin;
    private final Config config;

    public DeckFireballSpell(Main plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        double charge = resolveCharge(caster, context);
        Location eye = caster.getEyeLocation();
        Vector direction = eye.getDirection().clone().normalize();
        Location start = eye.clone().add(direction.clone().multiply(0.65));
        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.9f, config.pitch);
        launchProjectile(caster, start, direction, charge);
    }

    private double resolveCharge(Player caster, SpellContext context) {
        if (!config.chargeable) {
            return 0.0;
        }
        String sequence = context == null || context.inputEvent() == null ? "" : context.inputEvent().getInputSequence();
        if (caster.isSneaking() || sequence != null && sequence.toUpperCase(java.util.Locale.ROOT).contains("RIGHT")) {
            return 1.0;
        }
        return 0.0;
    }

    private void launchProjectile(Player caster, Location start, Vector direction, double charge) {
        World world = start.getWorld();
        if (world == null) {
            return;
        }
        double speed = Math.max(0.2, config.projectileSpeed);
        new BukkitRunnable() {
            private final Set<UUID> ignored = new HashSet<>();
            private Location current = start.clone();
            private double traveled = 0.0;

            @Override
            public void run() {
                if (!caster.isOnline() || traveled >= MAX_RANGE || current.getBlock().getType().isSolid()) {
                    impact(caster, current, charge);
                    cancel();
                    return;
                }
                LivingEntity hit = SpellTargetingUtil.rayTraceLivingEntity(current, direction.clone().multiply(speed * TICK_DISTANCE_SCALE), HIT_RADIUS,
                        living -> isValidTarget(living, caster, ignored));
                if (hit != null) {
                    impact(caster, hit.getLocation().clone().add(0, Math.min(1.0, hit.getHeight() * 0.5), 0), charge);
                    cancel();
                    return;
                }
                spawnTrail(world, current, charge);
                current.add(direction.clone().multiply(speed));
                traveled += speed;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private boolean isValidTarget(Entity entity, Player caster, Set<UUID> ignored) {
        if (!(entity instanceof LivingEntity living) || living.equals(caster) || living instanceof ArmorStand || living.isDead()) {
            return false;
        }
        return !ignored.contains(living.getUniqueId()) && CombatTargetUtil.isSpellValidTarget(living);
    }

    private void impact(Player caster, Location center, double charge) {
        if (center == null || center.getWorld() == null) {
            return;
        }
        World world = center.getWorld();
        double radius = interpolate(config.radius, config.chargedRadius, charge);
        double damage = interpolate(config.damage, config.chargedDamage, charge);
        double burnDamage = interpolate(config.burnDamagePerSecond, config.chargedBurnDamagePerSecond, charge);
        int burnTicks = (int) Math.round(interpolate(config.burnSeconds, config.chargedBurnSeconds, charge) * 20.0);
        int secondaryCount = Math.max(config.secondaryExplosions, (int) Math.round(config.chargedSecondaryExplosions * charge));

        world.spawnParticle(Particle.EXPLOSION, center, Math.max(1, (int) Math.ceil(radius / 2.5)), 0.1, 0.1, 0.1, 0.0);
        world.spawnParticle(Particle.FLAME, center, 35, radius * 0.35, 0.35, radius * 0.35, 0.035);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.75f, config.pitch);
        Set<UUID> primaryHit = damageAndBurn(caster, center, radius, damage, burnTicks, burnDamage, 0, new HashSet<>());

        if (config.burningGroundSeconds > 0 && config.burningGroundRadius > 0.0) {
            startBurningGround(caster, center.clone(), config.burningGroundRadius, config.burningGroundDamagePerSecond, config.burningGroundSeconds);
        }
        if (config.spreadBurn) {
            startSpreadBurn(caster, primaryHit, burnTicks, burnDamage);
        }
        if (secondaryCount > 0) {
            scheduleSecondaryExplosions(caster, center.clone(), secondaryCount, config.secondaryDelayTicks, config.secondaryRadius, config.secondaryDamage, burnTicks, burnDamage);
        }
        if (config.stunTicks > 0 && charge >= 0.99) {
            for (LivingEntity target : SpellEffectUtil.getLivingTargets(center, radius, living -> !living.equals(caster))) {
                SpellEffectUtil.applyStun(target, config.stunTicks, true);
            }
        }
        if (config.knockback > 0.0) {
            applyKnockback(caster, center, radius, config.knockback * (1.0 + charge));
        }
        if (config.selfDamageBelowHealthRatio > 0.0 && caster.getHealth() <= caster.getMaxHealth() * config.selfDamageBelowHealthRatio) {
            caster.damage(Math.max(1.0, caster.getHealth() * config.selfDamageCurrentHealthRatio));
        }
    }

    private Set<UUID> damageAndBurn(Player caster, Location center, double radius, double damage, int burnTicks, double burnDamage, int chainDepth, Set<UUID> chainExploded) {
        Set<UUID> hitIds = new HashSet<>();
        for (LivingEntity target : SpellEffectUtil.getLivingTargets(center, radius, living -> !living.equals(caster))) {
            if (!CombatTargetUtil.isSpellValidTarget(target)) {
                continue;
            }
            SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, damage, true);
            if (burnTicks > 0) {
                target.setFireTicks(Math.max(target.getFireTicks(), burnTicks));
                startTargetBurn(caster, target, burnTicks, burnDamage, chainDepth, chainExploded);
            }
            hitIds.add(target.getUniqueId());
        }
        return hitIds;
    }

    private void startTargetBurn(Player caster, LivingEntity target, int totalTicks, double damagePerSecond, int chainDepth, Set<UUID> chainExploded) {
        if (damagePerSecond <= 0.0 || totalTicks <= 0) {
            return;
        }
        new BukkitRunnable() {
            private int elapsed;

            @Override
            public void run() {
                if (!caster.isOnline() || target.isDead() || !target.isValid()) {
                    if (config.livingInferno && target.isDead()) {
                        triggerLivingInferno(caster, target.getLocation(), chainDepth, chainExploded, config.livingInfernoDamage * Math.pow(0.85, chainDepth));
                    }
                    cancel();
                    return;
                }
                SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, damagePerSecond, true);
                target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 0.8, 0), 7, 0.25, 0.35, 0.25, 0.02);
                elapsed += 20;
                if (elapsed >= totalTicks) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void triggerLivingInferno(Player caster, Location center, int chainDepth, Set<UUID> chainExploded, double damage) {
        if (!config.livingInferno || center == null || center.getWorld() == null || chainDepth >= config.maxLivingInfernoChains || damage <= 1.0) {
            return;
        }
        String key = center.getWorld().getUID() + ":" + center.getBlockX() + ":" + center.getBlockY() + ":" + center.getBlockZ();
        UUID pseudo = UUID.nameUUIDFromBytes(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (!chainExploded.add(pseudo)) {
            return;
        }
        center.getWorld().spawnParticle(Particle.LAVA, center, 18, 0.8, 0.35, 0.8, 0.04);
        center.getWorld().playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.75f, 0.75f);
        damageAndBurn(caster, center, config.livingInfernoRadius, damage, config.burnSeconds * 20, config.burnDamagePerSecond, chainDepth + 1, chainExploded);
    }

    private void startBurningGround(Player caster, Location center, double radius, double damagePerSecond, int seconds) {
        new BukkitRunnable() {
            private int elapsed;

            @Override
            public void run() {
                if (!caster.isOnline()) {
                    cancel();
                    return;
                }
                World world = center.getWorld();
                if (world != null) {
                    SpellEffectUtil.spawnRingParticles(center, radius, Particle.FLAME, 28, 0.05);
                    world.spawnParticle(Particle.FLAME, center, 12, radius * 0.35, 0.05, radius * 0.35, 0.02);
                }
                SpellEffectUtil.applyAreaDamage(caster, center, radius, damagePerSecond);
                elapsed += 20;
                if (elapsed >= seconds * 20) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void startSpreadBurn(Player caster, Set<UUID> burningIds, int burnTicks, double originalBurnDamage) {
        if (burningIds == null || burningIds.isEmpty()) {
            return;
        }
        new BukkitRunnable() {
            private int elapsed;
            private Set<UUID> current = new HashSet<>(burningIds);
            private int depth;

            @Override
            public void run() {
                if (!caster.isOnline() || depth >= config.maxSpreadDepth) {
                    cancel();
                    return;
                }
                Set<UUID> next = new HashSet<>();
                for (UUID id : current) {
                    Entity entity = BukkitHolder.entity(id);
                    if (!(entity instanceof LivingEntity source) || source.isDead()) {
                        continue;
                    }
                    for (LivingEntity nearby : SpellEffectUtil.getLivingTargets(source.getLocation(), config.spreadRadius, living -> !living.equals(caster))) {
                        if (nearby.getUniqueId().equals(id) || current.contains(nearby.getUniqueId())) {
                            continue;
                        }
                        nearby.setFireTicks(Math.max(nearby.getFireTicks(), config.spreadBurnSeconds * 20));
                        startTargetBurn(caster, nearby, config.spreadBurnSeconds * 20, config.spreadDamagePerSecond, 0, new HashSet<>());
                        next.add(nearby.getUniqueId());
                    }
                }
                current = next;
                depth++;
                elapsed += 20;
                if (elapsed >= burnTicks || current.isEmpty()) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void scheduleSecondaryExplosions(Player caster, Location center, int count, int delayTicks, double radius, double damage, int burnTicks, double burnDamage) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        for (int i = 0; i < count; i++) {
            int index = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                double angle = (Math.PI * 2.0 * index) / Math.max(1, count);
                double distance = 1.5 + (index % 2) * 1.2;
                Location secondary = center.clone().add(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance);
                world.spawnParticle(Particle.EXPLOSION, secondary, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.FLAME, secondary, 20, radius * 0.35, 0.2, radius * 0.35, 0.02);
                world.playSound(secondary, Sound.ENTITY_GENERIC_EXPLODE, 0.45f, 1.25f);
                damageAndBurn(caster, secondary, radius, damage, burnTicks, burnDamage, 0, new HashSet<>());
            }, Math.max(1, delayTicks));
        }
    }

    private void applyKnockback(Player caster, Location center, double radius, double strength) {
        for (LivingEntity target : SpellEffectUtil.getLivingTargets(center, radius, living -> !living.equals(caster))) {
            Vector direction = target.getLocation().toVector().subtract(center.toVector());
            if (direction.lengthSquared() <= 0.001) {
                direction = caster.getLocation().getDirection();
            }
            target.setVelocity(direction.normalize().multiply(strength).setY(0.35));
        }
    }

    private void spawnTrail(World world, Location current, double charge) {
        world.spawnParticle(Particle.FLAME, current, charge >= 0.99 ? 5 : 2, 0.06, 0.06, 0.06, 0.01);
        if (config.rarityOrdinal >= 3) {
            world.spawnParticle(Particle.SMOKE, current, 1, 0.03, 0.03, 0.03, 0.004);
        }
    }

    private double interpolate(double base, double charged, double charge) {
        return charged <= 0.0 ? base : base + ((charged - base) * Math.max(0.0, Math.min(1.0, charge)));
    }

    public record Config(int rarityOrdinal,
                         double damage,
                         int manaCost,
                         int cooldownSeconds,
                         double radius,
                         int burnSeconds,
                         double burnDamagePerSecond,
                         double projectileSpeed,
                         int burningGroundSeconds,
                         double burningGroundDamagePerSecond,
                         double burningGroundRadius,
                         boolean spreadBurn,
                         double spreadRadius,
                         int spreadBurnSeconds,
                         double spreadDamagePerSecond,
                         int maxSpreadDepth,
                         int secondaryExplosions,
                         int secondaryDelayTicks,
                         double secondaryRadius,
                         double secondaryDamage,
                         boolean chargeable,
                         double chargedDamage,
                         double chargedRadius,
                         int chargedBurnSeconds,
                         double chargedBurnDamagePerSecond,
                         int chargedSecondaryExplosions,
                         double knockback,
                         int stunTicks,
                         boolean livingInferno,
                         double livingInfernoRadius,
                         double livingInfernoDamage,
                         int maxLivingInfernoChains,
                         double selfDamageBelowHealthRatio,
                         double selfDamageCurrentHealthRatio,
                         float pitch) {
    }

    private static final class BukkitHolder {
        private static Entity entity(UUID id) {
            for (World world : org.bukkit.Bukkit.getWorlds()) {
                Entity entity = world.getEntity(id);
                if (entity != null) {
                    return entity;
                }
            }
            return null;
        }
    }
}
