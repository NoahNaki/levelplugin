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
import org.bukkit.WeatherType;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Corrosive rain that eats the mine away one block at a time.
 *
 * <p>This is the one effect where the timing carries the whole idea, so it is worth being explicit
 * about how it is built. The break pipeline settles a proc in a single pass, which would normally
 * mean the whole patch vanishes on the same tick the rain starts — the exact thing that made this
 * enchant read as "a green particle cloud" rather than as rain dissolving rock.
 *
 * <p>So the real blocks are broken up front (once, as the pipeline requires) and the animation runs
 * over {@link BlockEcho} copies. Each copy is assigned a falling acid streak with its own start
 * tick; when a streak lands, that one copy is removed with a splash. What a player sees is acid
 * falling from the sky and each block melting as it is hit.
 *
 * <p>Because the animation never touches world state, it behaves identically on a normal mine and
 * on an X-PrivateMines packet mine.
 */
public final class AcidRainEnchant extends CinematicAreaEnchant {

    private double baseRadius;
    private double maxRadius;
    private int depth;
    private int fallTicks;
    private double spawnHeight;
    private int spreadTicks;
    private boolean personalWeather;

    public AcidRainEnchant(File configFile) {
        super(configFile);
    }

    @Override
    protected void loadAreaProperties(JsonObject config) {
        this.baseRadius = JsonUtils.getOptionalDouble(config, "baseRadius", 5.0D);
        this.maxRadius = JsonUtils.getOptionalDouble(config, "maxRadius", 9.0D);
        this.depth = JsonUtils.getOptionalInt(config, "depth", 3);
        this.fallTicks = Math.max(3, JsonUtils.getOptionalInt(config, "fallTicks", 12));
        this.spawnHeight = Math.max(4.0D, JsonUtils.getOptionalDouble(config, "spawnHeight", 14.0D));
        this.spreadTicks = Math.max(1, JsonUtils.getOptionalInt(config, "spreadTicks", 45));
        this.personalWeather = JsonUtils.getOptionalBoolean(config, "personalWeather", true);
    }

    @Override
    @NotNull
    protected BreakEventStrategy defaultEventStrategy() {
        return BreakEventStrategy.AGGREGATE;
    }

    /**
     * A shallow disc centred on the broken block, eaten downward. Selecting by shape rather than by
     * "highest block in the column" is deliberate: on a packet mine the world is really air, so a
     * top-down world scan would find nothing at all.
     */
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
                // Downward from the broken layer: acid runs down, it does not climb.
                for (int y = 0; y > -depth; y--) {
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
        Location center = origin.getLocation().add(0.5, 0.5, 0.5);
        WeatherType previousWeather = personalWeather ? player.getPlayerWeather() : null;
        if (personalWeather) {
            player.setPlayerWeather(WeatherType.DOWNFALL);
        }
        center.getWorld().playSound(center, Sound.WEATHER_RAIN, 1.0f, 0.75f);

        // Higher blocks are hit first, so the patch dissolves from the top down.
        List<BlockEcho> ordered = new ArrayList<>(echoes);
        ordered.sort(Comparator.comparingDouble((BlockEcho e) -> e.center().getY()).reversed());

        List<Drop> drops = new ArrayList<>(ordered.size());
        for (int i = 0; i < ordered.size(); i++) {
            BlockEcho echo = ordered.get(i);
            echo.spawn(1.0f, null);
            // Spread the impacts across spreadTicks so it rains rather than landing all at once.
            int startTick = (int) Math.round((i / (double) Math.max(1, ordered.size())) * spreadTicks)
                    + ThreadLocalRandom.current().nextInt(0, 4);
            drops.add(new Drop(echo, startTick));
        }

        int totalTicks = spreadTicks + fallTicks + 8;
        Effects.animate(totalTicks, tick -> {
            if (!player.isOnline()) {
                return false;
            }
            for (Drop drop : drops) {
                if (drop.done || tick < drop.startTick) {
                    continue;
                }
                double progress = (tick - drop.startTick) / (double) fallTicks;
                Location impact = drop.echo.center();
                if (progress >= 1.0) {
                    splash(impact);
                    drop.echo.remove();
                    drop.done = true;
                    continue;
                }
                double y = Effects.lerp(impact.getY() + spawnHeight, impact.getY() + 1.05,
                        Effects.easeIn(progress));
                streak(new Location(impact.getWorld(), impact.getX(), y, impact.getZ()));
            }
            if (tick % 6 == 0) {
                center.getWorld().playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.55f);
            }
            return true;
        }, () -> {
            BlockEcho.removeAll(echoes);
            if (personalWeather && player.isOnline()) {
                if (previousWeather == null) {
                    player.resetPlayerWeather();
                } else {
                    player.setPlayerWeather(previousWeather);
                }
            }
        });
    }

    private void streak(Location head) {
        Particle.DustTransition acid = new Particle.DustTransition(
                Color.fromRGB(185, 255, 45), Color.fromRGB(35, 185, 15), 1.35f);
        for (int i = 0; i < 4; i++) {
            head.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION,
                    head.clone().add(0, i * 0.38, 0), 1, 0.025, 0.025, 0.025, 0.0, acid);
        }
    }

    private void splash(Location at) {
        Particle.DustTransition acid = new Particle.DustTransition(
                Color.fromRGB(205, 255, 70), Color.fromRGB(25, 150, 10), 1.7f);
        at.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, at, 14, 0.42, 0.16, 0.42, 0.03, acid);
        at.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, at, 4, 0.3, 0.12, 0.3, 0.015);
    }

    private static final class Drop {
        private final BlockEcho echo;
        private final int startTick;
        private boolean done;

        private Drop(BlockEcho echo, int startTick) {
            this.echo = echo;
            this.startTick = startTick;
        }
    }
}
