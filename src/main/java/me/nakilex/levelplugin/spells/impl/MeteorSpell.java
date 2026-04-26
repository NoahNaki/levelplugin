package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.particles.ParticleService;
import me.nakilex.levelplugin.particles.presets.ElementalPresets;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.spells.SpellTargetingUtil;
import me.nakilex.levelplugin.utils.ModelEngineUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.Particle.DustOptions;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class MeteorSpell implements SpellHandler {
    private static final String MODEL_ID = "meteor_of_doom";
    private static final double TARGET_RANGE = 40.0;
    private static final int DOT_DURATION_TICKS = 60;
    private static final int DOT_PERIOD_TICKS = 10;
    private static final double SPEED_PER_TICK = 1.2;
    private static final DustOptions METEOR_ORANGE_DUST = new DustOptions(org.bukkit.Color.fromRGB(255, 140, 0), 1.2f);

    private final Main plugin;
    private final ParticleService particleService;
    private final double spawnHeight;
    private final double impactDamage;
    private final double impactRadius;
    private final double dotRadius;
    private final double dotDamage;
    private final double flowerRadius;
    private final int flowerPetals;

    public MeteorSpell(Main plugin, ParticleService particleService) {
        this(plugin, particleService, 18.0, 12.0, 5.5, 3.5, 2.0, 4.8, 6);
    }

    public MeteorSpell(Main plugin, ParticleService particleService,
                       double spawnHeight, double impactDamage, double impactRadius,
                       double dotRadius, double dotDamage,
                       double flowerRadius, int flowerPetals) {
        this.plugin = plugin;
        this.particleService = particleService;
        this.spawnHeight = spawnHeight;
        this.impactDamage = impactDamage;
        this.impactRadius = impactRadius;
        this.dotRadius = dotRadius;
        this.dotDamage = dotDamage;
        this.flowerRadius = flowerRadius;
        this.flowerPetals = flowerPetals;
    }

    @Override
    public void cast(SpellContext context) {
        Player player = context.player();
        Location impact = SpellTargetingUtil.resolveNearestEnemyGround(player, TARGET_RANGE);
        if (impact == null) {
            impact = SpellTargetingUtil.resolveTargetGround(player, TARGET_RANGE);
        }
        if (impact == null) {
            impact = player.getLocation().clone().add(player.getLocation().getDirection().multiply(8.0));
            impact.setY(player.getWorld().getHighestBlockYAt(impact) + 1.0);
        }
        Location spawn = player.getLocation().clone().add(0, spawnHeight, 0);
        World world = spawn.getWorld();
        if (world == null) {
            return;
        }
        ArmorStand meteor = world.spawn(spawn, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setSilent(true);
            stand.setCollidable(false);
            stand.setInvulnerable(true);
        });
        ModelEngineUtil.ModelApplyResult result = ModelEngineUtil.applyModels(meteor, List.of(MODEL_ID), plugin);
        if (!result.failed().isEmpty()) {
            plugin.getLogger().warning("Meteor spell failed to apply model: " + String.join(", ", result.failed()));
        }
        launchMeteor(player, meteor, impact);
    }

    private void launchMeteor(Player caster, ArmorStand meteor, Location impact) {
        Vector total = impact.toVector().subtract(meteor.getLocation().toVector());
        double distance = total.length();
        int maxTicks = (int) Math.ceil(distance / SPEED_PER_TICK) + 10;
        new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                if (!meteor.isValid()) {
                    cancel();
                    return;
                }
                Location current = meteor.getLocation();
                Vector toTarget = impact.toVector().subtract(current.toVector());
                if (toTarget.lengthSquared() <= 0.6 || ticks >= maxTicks) {
                    explode(caster, impact);
                    meteor.remove();
                    cancel();
                    return;
                }
                Vector step = toTarget.normalize().multiply(SPEED_PER_TICK);
                Location next = current.clone().add(step);
                meteor.teleport(next);
                meteor.getWorld().spawnParticle(Particle.SMOKE, next, 2, 0.1, 0.1, 0.1, 0.01);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void explode(Player caster, Location impact) {
        World world = impact.getWorld();
        if (world == null) {
            return;
        }
        world.spawnParticle(Particle.EXPLOSION, impact, 1, 0.0, 0.0, 0.0, 0.0);
        world.spawnParticle(Particle.LAVA, impact, 36, 0.9, 0.4, 0.9, 0.03);
        spawnOrangeGroundFlower(impact);
        world.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.3f, 0.8f);
        SpellEffectUtil.applyAreaDamage(caster, impact, impactRadius, impactDamage);
        particleService.renderPreset(caster, ElementalPresets.BURNING_SIGIL, impact);
        SpellEffectUtil.startDamageOverTime(plugin, caster, impact, dotRadius, dotDamage,
                DOT_PERIOD_TICKS, DOT_DURATION_TICKS);
    }

    private void spawnOrangeGroundFlower(Location impact) {
        World world = impact.getWorld();
        if (world == null) {
            return;
        }
        for (int i = 0; i < 220; i++) {
            double t = (Math.PI * 2.0 * i) / 220.0;
            double roseRadius = flowerRadius * Math.cos(flowerPetals * t);
            double x = roseRadius * Math.cos(t);
            double z = roseRadius * Math.sin(t);
            world.spawnParticle(Particle.DUST, impact.clone().add(x, 0.08, z), 1, 0.0, 0.0, 0.0, 0.0, METEOR_ORANGE_DUST);
        }
        for (int i = 0; i < 64; i++) {
            double angle = (Math.PI * 2.0 * i) / 64.0;
            double x = Math.cos(angle) * impactRadius;
            double z = Math.sin(angle) * impactRadius;
            world.spawnParticle(Particle.DUST, impact.clone().add(x, 0.1, z), 1, 0.0, 0.0, 0.0, 0.0, METEOR_ORANGE_DUST);
        }
    }
}
