package me.nakilex.xprisonenchants.enchant;

import com.google.gson.JsonObject;
import dev.drawethree.xprison.api.enchants.area.AreaBounds;
import dev.drawethree.xprison.api.enchants.area.BreakEventStrategy;
import dev.drawethree.xprison.api.utils.JsonUtils;
import me.nakilex.xprisonenchants.fx.BlockEcho;
import me.nakilex.xprisonenchants.fx.Effects;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A volley of meteors that crater the mine one impact at a time.
 *
 * <p>Selection is a wide, shallow disc; the animation then splits those blocks into impact clusters
 * and drops one meteor per cluster on a stagger, removing that cluster's {@link BlockEcho}s when the
 * meteor lands. Clustering in the animation rather than the selection keeps the enchant stateless
 * between the two phases, which matters because a single instance serves every player at once.
 */
public final class MeteorShowerEnchant extends CinematicAreaEnchant {

    private double baseRadius;
    private double maxRadius;
    private int depth;
    private int baseMeteors;
    private int maxMeteors;
    private int intervalTicks;
    private int fallTicks;
    private double spawnHeight;

    public MeteorShowerEnchant(File configFile) {
        super(configFile);
    }

    @Override
    protected void loadAreaProperties(JsonObject config) {
        this.baseRadius = JsonUtils.getOptionalDouble(config, "baseRadius", 6.0D);
        this.maxRadius = JsonUtils.getOptionalDouble(config, "maxRadius", 11.0D);
        this.depth = JsonUtils.getOptionalInt(config, "depth", 3);
        this.baseMeteors = Math.max(1, JsonUtils.getOptionalInt(config, "baseMeteors", 7));
        this.maxMeteors = Math.max(1, JsonUtils.getOptionalInt(config, "maxMeteors", 14));
        this.intervalTicks = Math.max(1, JsonUtils.getOptionalInt(config, "intervalTicks", 6));
        this.fallTicks = Math.max(3, JsonUtils.getOptionalInt(config, "fallTicks", 14));
        this.spawnHeight = Math.max(6.0D, JsonUtils.getOptionalDouble(config, "spawnHeight", 15.0D));
    }

    @Override
    @NotNull
    protected BreakEventStrategy defaultEventStrategy() {
        return BreakEventStrategy.AGGREGATE;
    }

    @Override
    @NotNull
    public List<Block> selectTargets(Player player, Block origin, @Nullable AreaBounds region, int level) {
        double radius = Effects.lerp(baseRadius, maxRadius, Effects.levelScale(level, getMaxLevel()));
        List<Block> blocks = new ArrayList<>();
        int r = (int) Math.ceil(radius);
        double rSquared = radius * radius;
        int cap = areaSettings().maxBlocks();
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (x * x + z * z > rSquared) {
                    continue;
                }
                for (int y = 1; y > -depth; y--) {
                    if (!accept(origin.getRelative(x, y, z), region, blocks, cap)) {
                        return blocks;
                    }
                }
            }
        }
        return blocks;
    }

    @Override
    protected void playEffect(Player player, Block origin, List<BlockEcho> echoes, int level) {
        int meteors = Effects.lerpInt(baseMeteors, maxMeteors, Effects.levelScale(level, getMaxLevel()));
        List<List<BlockEcho>> clusters = cluster(echoes, meteors);

        for (BlockEcho echo : echoes) {
            echo.spawn(1.0f, null);
        }

        List<Meteor> pending = new ArrayList<>(clusters.size());
        for (int i = 0; i < clusters.size(); i++) {
            List<BlockEcho> cluster = clusters.get(i);
            Location impact = centroid(cluster);
            Location start = impact.clone().add(
                    ThreadLocalRandom.current().nextDouble(-5.0, 5.0),
                    spawnHeight,
                    ThreadLocalRandom.current().nextDouble(-5.0, 5.0));
            pending.add(new Meteor(cluster, start, impact, i * intervalTicks));
        }

        int totalTicks = clusters.size() * intervalTicks + fallTicks + 8;
        Effects.animate(totalTicks, tick -> {
            if (!player.isOnline()) {
                return false;
            }
            for (Meteor meteor : pending) {
                if (meteor.done || tick < meteor.startTick) {
                    continue;
                }
                if (meteor.display == null) {
                    meteor.display = spawnMeteor(meteor.start);
                    meteor.start.getWorld().playSound(meteor.start,
                            Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.55f, 0.65f);
                }
                double progress = (tick - meteor.startTick) / (double) fallTicks;
                if (progress >= 1.0) {
                    impact(meteor);
                    continue;
                }
                Location at = Effects.travel(meteor.start, meteor.impact, Effects.easeIn(progress));
                meteor.display.teleport(at.clone().subtract(0.45, 0.45, 0.45));
                at.getWorld().spawnParticle(Particle.FLAME, at, 4, 0.18, 0.18, 0.18, 0.015);
                at.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, at, 2, 0.12, 0.12, 0.12, 0.01);
            }
            return true;
        }, () -> {
            for (Meteor meteor : pending) {
                if (meteor.display != null) {
                    meteor.display.remove();
                }
            }
            BlockEcho.removeAll(echoes);
        });
    }

    private void impact(Meteor meteor) {
        meteor.done = true;
        if (meteor.display != null) {
            meteor.display.remove();
            meteor.display = null;
        }
        BlockEcho.removeAll(meteor.cluster);
        Location at = meteor.impact;
        at.getWorld().spawnParticle(Particle.EXPLOSION, at, 4, 1.0, 0.7, 1.0, 0.05);
        at.getWorld().spawnParticle(Particle.LAVA, at, 12, 1.3, 0.6, 1.3, 0.08);
        at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.25f);
    }

    /**
     * Splits the echoes into roughly {@code count} spatial groups by repeatedly taking an
     * unassigned echo as a seed and claiming its nearest neighbours. Cheap and good enough — the
     * only requirement is that each meteor lands on a contiguous-looking patch.
     */
    private List<List<BlockEcho>> cluster(List<BlockEcho> echoes, int count) {
        List<List<BlockEcho>> clusters = new ArrayList<>();
        int groups = Math.max(1, Math.min(count, echoes.size()));
        int perCluster = (int) Math.ceil(echoes.size() / (double) groups);

        List<BlockEcho> remaining = new ArrayList<>(echoes);
        Set<BlockEcho> taken = new HashSet<>();
        while (!remaining.isEmpty() && clusters.size() < groups) {
            BlockEcho seed = remaining.removeLast();
            if (!taken.add(seed)) {
                continue;
            }
            Location seedAt = seed.center();
            remaining.sort((a, b) -> Double.compare(
                    a.center().distanceSquared(seedAt), b.center().distanceSquared(seedAt)));

            List<BlockEcho> cluster = new ArrayList<>();
            cluster.add(seed);
            while (cluster.size() < perCluster && !remaining.isEmpty()) {
                BlockEcho next = remaining.removeFirst();
                if (taken.add(next)) {
                    cluster.add(next);
                }
            }
            clusters.add(cluster);
        }
        // Anything left over joins the last cluster rather than being silently dropped.
        if (!remaining.isEmpty() && !clusters.isEmpty()) {
            for (BlockEcho leftover : remaining) {
                if (taken.add(leftover)) {
                    clusters.getLast().add(leftover);
                }
            }
        }
        return clusters;
    }

    private Location centroid(List<BlockEcho> cluster) {
        double x = 0, y = 0, z = 0;
        for (BlockEcho echo : cluster) {
            Location at = echo.center();
            x += at.getX();
            y += at.getY();
            z += at.getZ();
        }
        int n = cluster.size();
        return new Location(cluster.getFirst().center().getWorld(), x / n, y / n, z / n);
    }

    private BlockDisplay spawnMeteor(Location at) {
        Location spawnAt = at.clone().subtract(0.45, 0.45, 0.45);
        return spawnAt.getWorld().spawn(spawnAt, BlockDisplay.class, display -> {
            display.setBlock(Material.MAGMA_BLOCK.createBlockData());
            display.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f(0f, 0f, 0f, 1f),
                    new Vector3f(0.9f, 0.9f, 0.9f),
                    new AxisAngle4f(0f, 0f, 0f, 1f)));
            display.setBrightness(new Display.Brightness(15, 15));
            display.setGlowing(true);
            display.setGlowColorOverride(Color.RED);
            display.setPersistent(false);
        });
    }

    private static final class Meteor {
        private final List<BlockEcho> cluster;
        private final Location start;
        private final Location impact;
        private final int startTick;
        private BlockDisplay display;
        private boolean done;

        private Meteor(List<BlockEcho> cluster, Location start, Location impact, int startTick) {
            this.cluster = cluster;
            this.start = start;
            this.impact = impact;
            this.startTick = startTick;
        }
    }
}
