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
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Collapses a sphere of the mine into a singularity above the broken block.
 *
 * <p>Debris spirals inward on an eased curve so the pull accelerates, with the wobble decaying to
 * nothing as each piece reaches the core.
 */
public final class BlackHoleEnchant extends CinematicAreaEnchant {

    private double baseRadius;
    private double maxRadius;
    private int durationTicks;
    private double coreHeight;

    public BlackHoleEnchant(File configFile) {
        super(configFile);
    }

    @Override
    protected void loadAreaProperties(JsonObject config) {
        this.baseRadius = JsonUtils.getOptionalDouble(config, "baseRadius", 5.5D);
        this.maxRadius = JsonUtils.getOptionalDouble(config, "maxRadius", 9.0D);
        this.durationTicks = JsonUtils.getOptionalInt(config, "durationTicks", 55);
        this.coreHeight = JsonUtils.getOptionalDouble(config, "coreHeight", 2.5D);
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
        return sphere(origin, region, radius, areaSettings().maxBlocks());
    }

    @Override
    protected void playEffect(Player player, Block origin, List<BlockEcho> echoes, int level) {
        Location center = origin.getLocation().add(0.5, coreHeight, 0.5);
        double radius = Effects.lerp(baseRadius, maxRadius, Effects.levelScale(level, getMaxLevel()));

        BlockDisplay core = spawnCore(center);
        List<Pull> pulls = new ArrayList<>(echoes.size());
        for (BlockEcho echo : echoes) {
            echo.spawn(0.68f, Color.PURPLE);
            pulls.add(new Pull(echo, echo.center(),
                    ThreadLocalRandom.current().nextDouble(0.0, Math.PI * 2.0)));
        }

        center.getWorld().playSound(center, Sound.BLOCK_PORTAL_TRIGGER, 1.0f, 0.45f);

        Effects.animate(durationTicks, tick -> {
            if (!player.isOnline()) {
                return false;
            }
            double progress = tick / (double) durationTicks;
            double eased = Effects.easeIn(progress);
            double remaining = 1.0 - progress;

            for (Pull pull : pulls) {
                Vector delta = center.toVector().subtract(pull.start.toVector()).multiply(eased);
                Location next = pull.start.clone().add(delta);
                double angle = pull.phase + tick * 0.38;
                next.add(Math.cos(angle) * remaining * 1.35,
                        Math.sin(angle * 0.7) * remaining,
                        Math.sin(angle) * remaining * 1.35);
                pull.echo.moveTo(next, 0.68f);
            }

            center.getWorld().spawnParticle(Particle.REVERSE_PORTAL, center, 18,
                    radius * 0.45, radius * 0.35, radius * 0.45, 0.18);
            center.getWorld().spawnParticle(Particle.SQUID_INK, center, 3, 0.8, 0.8, 0.8, 0.02);
            if (tick % 10 == 0) {
                center.getWorld().playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.45f + (float) progress);
            }
            return true;
        }, () -> {
            BlockEcho.removeAll(echoes);
            core.remove();
        });
    }

    private BlockDisplay spawnCore(Location center) {
        Location at = center.clone().subtract(1.1, 1.1, 1.1);
        return at.getWorld().spawn(at, BlockDisplay.class, display -> {
            display.setBlock(Material.BLACK_CONCRETE.createBlockData());
            display.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f(0f, 0f, 0f, 1f),
                    new Vector3f(2.2f, 2.2f, 2.2f),
                    new AxisAngle4f(0f, 0f, 0f, 1f)));
            display.setBrightness(new Display.Brightness(0, 0));
            display.setGlowing(true);
            display.setGlowColorOverride(Color.PURPLE);
            display.setPersistent(false);
        });
    }

    private record Pull(BlockEcho echo, Location start, double phase) {
    }
}
