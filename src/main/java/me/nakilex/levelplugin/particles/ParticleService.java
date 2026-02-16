package me.nakilex.levelplugin.particles;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import me.nakilex.levelplugin.particles.patterns.ParticlePattern;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class ParticleService {
    private final JavaPlugin plugin;

    public ParticleService(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public BukkitTask renderPreset(Player player, ParticlePreset preset) {
        return renderPreset(player, preset, null);
    }

    public BukkitTask renderPreset(Player player, ParticlePreset preset, Location centerOverride) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(preset, "preset");
        ParticlePresetSettings settings = preset.settings();

        return new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                if (tick >= settings.ticks()) {
                    cancel();
                    return;
                }
                Location center = centerOverride == null
                        ? resolveCenter(player, settings)
                        : centerOverride.clone();
                ParticleRenderContext context = new ParticleRenderContext(player, center, player.getLocation(),
                        settings.points(), tick, settings.ticks());
                for (ParticlePattern pattern : preset.patterns()) {
                    pattern.render(context);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Render a preset continuously while a condition is true.
     *
     * This is reusable for mounted trails, pet cosmetics, and future follow effects.
     */
    public BukkitTask renderPresetWhile(Player player,
                                        ParticlePreset preset,
                                        Supplier<Location> centerSupplier,
                                        BooleanSupplier shouldContinue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(preset, "preset");
        Objects.requireNonNull(centerSupplier, "centerSupplier");
        Objects.requireNonNull(shouldContinue, "shouldContinue");

        ParticlePresetSettings settings = preset.settings();
        int cycle = Math.max(1, settings.ticks());

        return new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !shouldContinue.getAsBoolean()) {
                    cancel();
                    return;
                }
                Location center = centerSupplier.get();
                if (center == null || center.getWorld() == null) {
                    cancel();
                    return;
                }
                int phase = tick % cycle;
                ParticleRenderContext context = new ParticleRenderContext(player, center, player.getLocation(),
                        settings.points(), phase, cycle);
                for (ParticlePattern pattern : preset.patterns()) {
                    pattern.render(context);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private Location resolveCenter(Player player, ParticlePresetSettings settings) {
        Location base = player.getLocation();
        if (settings.center() == ParticleCenter.SELF) {
            return base.clone();
        }
        Location eye = player.getEyeLocation();
        var ray = player.rayTraceBlocks(settings.lookDistance());
        if (ray != null && ray.getHitPosition() != null) {
            Vector hit = ray.getHitPosition();
            return hit.toLocation(eye.getWorld());
        }
        Vector direction = eye.getDirection().clone().normalize().multiply(settings.lookDistance());
        return eye.clone().add(direction);
    }
}
