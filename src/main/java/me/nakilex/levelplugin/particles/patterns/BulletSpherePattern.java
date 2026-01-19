package me.nakilex.levelplugin.particles.patterns;

import java.util.concurrent.ThreadLocalRandom;

import me.nakilex.levelplugin.particles.ParticleRenderContext;
import me.nakilex.levelplugin.particles.ParticleSpawnUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.util.Vector;

/**
 * Renders a particle sphere with a ricocheting "bullet" particle that bounces
 * inside the sphere.
 */
public class BulletSpherePattern implements ParticlePattern {
    private static final double EPSILON = 0.001;
    private static final int IMPACT_PARTICLE_COUNT = 6;
    private static final double IMPACT_SPREAD = 0.12;
    private static final double RANDOM_BOUNCE_INTENSITY = 0.35;

    private final Particle sphereParticle;
    private final Object sphereData;
    private final Particle bulletParticle;
    private final Object bulletData;
    private final double radius;
    private final double bulletSpeed;
    private final int spherePoints;
    private final int trailPoints;

    private Vector position;
    private Vector velocity;
    private boolean initialized;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    public BulletSpherePattern(Particle sphereParticle, Object sphereData, Particle bulletParticle, Object bulletData,
                               double radius, double bulletSpeed, int spherePoints, int trailPoints) {
        this.sphereParticle = sphereParticle;
        this.sphereData = sphereData;
        this.bulletParticle = bulletParticle;
        this.bulletData = bulletData;
        this.radius = Math.max(0.1, radius);
        this.bulletSpeed = Math.max(0.01, bulletSpeed);
        this.spherePoints = Math.max(12, spherePoints);
        this.trailPoints = Math.max(1, trailPoints);
    }

    @Override
    public void render(ParticleRenderContext context) {
        Location center = context.center();
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        renderSphere(world, center);
        renderBullet(world, center);
    }

    private void renderSphere(World world, Location center) {
        for (int i = 0; i < spherePoints; i++) {
            double t = (i + 0.5) / spherePoints;
            double phi = Math.acos(1 - 2 * t);
            double theta = Math.PI * (1 + Math.sqrt(5)) * (i + 0.5);
            double x = Math.cos(theta) * Math.sin(phi);
            double y = Math.cos(phi);
            double z = Math.sin(theta) * Math.sin(phi);
            Vector offset = new Vector(x, y, z).multiply(radius);
            ParticleSpawnUtil.spawn(world, center.clone().add(offset), sphereParticle, 1, sphereData);
        }
    }

    private void renderBullet(World world, Location center) {
        if (!initialized) {
            initializeBullet();
        }
        Vector previous = position.clone();
        Vector next = position.clone().add(velocity);
        if (next.length() >= radius) {
            Vector normal = next.clone().normalize();
            double dot = velocity.dot(normal);
            velocity = velocity.clone().subtract(normal.multiply(2 * dot));
            velocity = randomizeBounce(velocity);
            position = normal.multiply(radius - EPSILON);
            playImpact(world, center.clone().add(position));
            next = position.clone().add(velocity);
        }
        position = next;
        renderTrail(world, center, previous, position);
    }

    private void renderTrail(World world, Location center, Vector from, Vector to) {
        for (int i = 0; i < trailPoints; i++) {
            double progress = trailPoints == 1 ? 1.0 : (double) i / (trailPoints - 1);
            Vector point = from.clone().multiply(1.0 - progress).add(to.clone().multiply(progress));
            ParticleSpawnUtil.spawn(world, center.clone().add(point), bulletParticle, 1, bulletData);
        }
    }

    private void initializeBullet() {
        Vector direction = randomUnitVector(random);
        double distance = Math.cbrt(random.nextDouble()) * radius * 0.8;
        position = direction.multiply(distance);
        velocity = randomUnitVector(random).multiply(bulletSpeed);
        initialized = true;
    }

    private Vector randomUnitVector(ThreadLocalRandom random) {
        double theta = random.nextDouble(0, Math.PI * 2);
        double phi = Math.acos(1 - 2 * random.nextDouble());
        double x = Math.cos(theta) * Math.sin(phi);
        double y = Math.cos(phi);
        double z = Math.sin(theta) * Math.sin(phi);
        return new Vector(x, y, z);
    }

    private Vector randomizeBounce(Vector baseVelocity) {
        Vector normalized = baseVelocity.clone().normalize();
        Vector jitter = randomUnitVector(random).multiply(RANDOM_BOUNCE_INTENSITY);
        Vector combined = normalized.add(jitter).normalize();
        return combined.multiply(bulletSpeed);
    }

    private void playImpact(World world, Location location) {
        world.spawnParticle(Particle.END_ROD, location, IMPACT_PARTICLE_COUNT,
                IMPACT_SPREAD, IMPACT_SPREAD, IMPACT_SPREAD, 0.01);
        world.playSound(location, Sound.ENTITY_ARROW_HIT_PLAYER, 0.6f, 1.4f);
    }
}
