package me.nakilex.xprisonenchants.enchant;

import com.google.gson.JsonObject;
import dev.drawethree.xprison.api.enchants.area.AreaBounds;
import dev.drawethree.xprison.api.enchants.area.BreakEventStrategy;
import dev.drawethree.xprison.api.utils.JsonUtils;
import me.nakilex.xprisonenchants.fx.BlockEcho;
import me.nakilex.xprisonenchants.fx.Effects;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Rips a column of the mine into a rising spiral of debris.
 *
 * <p>Selection is a vertical cylinder around the broken block; the animation lifts the debris while
 * pulling it inward, so it reads as a funnel rather than a fountain.
 */
public final class TornadoEnchant extends CinematicAreaEnchant {

    private double baseRadius;
    private double maxRadius;
    private int columnHeight;
    private int durationTicks;

    public TornadoEnchant(File configFile) {
        super(configFile);
    }

    @Override
    protected void loadAreaProperties(JsonObject config) {
        this.baseRadius = JsonUtils.getOptionalDouble(config, "baseRadius", 5.0D);
        this.maxRadius = JsonUtils.getOptionalDouble(config, "maxRadius", 8.0D);
        this.columnHeight = JsonUtils.getOptionalInt(config, "columnHeight", 4);
        this.durationTicks = JsonUtils.getOptionalInt(config, "durationTicks", 70);
    }

    /**
     * A tornado can lift a few hundred blocks; announcing each one individually is the expensive
     * part of a proc, so the aggregate event is the sane default here.
     */
    @Override
    @NotNull
    protected BreakEventStrategy defaultEventStrategy() {
        return BreakEventStrategy.AGGREGATE;
    }

    @Override
    @NotNull
    public List<Block> selectTargets(Player player, Block origin, @Nullable AreaBounds region, int level) {
        double radius = Effects.lerp(baseRadius, maxRadius, Effects.levelScale(level, getMaxLevel()));
        return cylinder(origin, region, radius, columnHeight, areaSettings().maxBlocks());
    }

    @Override
    protected void playEffect(Player player, Block origin, List<BlockEcho> echoes, int level) {
        Location center = origin.getLocation().add(0.5, 1.0, 0.5);
        double radius = Effects.lerp(baseRadius, maxRadius, Effects.levelScale(level, getMaxLevel()));

        List<Debris> debris = new ArrayList<>(echoes.size());
        for (BlockEcho echo : echoes) {
            echo.spawn(0.72f, Color.WHITE);
            Vector offset = echo.center().toVector().subtract(center.toVector());
            double distance = Math.max(0.8, Math.hypot(offset.getX(), offset.getZ()));
            debris.add(new Debris(echo, Math.atan2(offset.getZ(), offset.getX()), distance));
        }

        center.getWorld().playSound(center, Sound.ENTITY_PHANTOM_FLAP, 1.2f, 0.55f);

        Effects.animate(durationTicks, tick -> {
            if (!player.isOnline()) {
                return false;
            }
            double progress = tick / (double) durationTicks;
            for (int i = 0; i < debris.size(); i++) {
                Debris piece = debris.get(i);
                double angle = piece.angle + tick * 0.31 + i * 0.12;
                double orbit = Math.max(0.35, piece.radius * (1.0 - progress * 0.72));
                double y = center.getY() + progress * 9.0 + (i % 7) * 0.18;
                piece.echo.moveTo(Effects.orbit(center, angle, orbit, y), 0.72f);
            }

            for (int i = 0; i < 12; i++) {
                double angle = tick * 0.34 + i * Math.PI / 6.0;
                double ringRadius = 1.0 + (i / 12.0) * radius * (1.0 - progress * 0.35);
                Location at = Effects.orbit(center, angle, ringRadius, center.getY() + (i / 12.0) * 7.0);
                center.getWorld().spawnParticle(Particle.CLOUD, at, 1, 0.08, 0.08, 0.08, 0.01);
            }
            if (tick % 12 == 0) {
                center.getWorld().playSound(center, Sound.ENTITY_PHANTOM_FLAP, 0.65f, 0.65f + (float) progress);
            }
            return true;
        }, Effects.cleanup(echoes));
    }

    private record Debris(BlockEcho echo, double angle, double radius) {
    }
}
