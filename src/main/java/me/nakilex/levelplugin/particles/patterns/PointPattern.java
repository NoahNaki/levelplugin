package me.nakilex.levelplugin.particles.patterns;

import me.nakilex.levelplugin.particles.ParticleRenderContext;
import me.nakilex.levelplugin.particles.ParticleSpawnUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

public record PointPattern(Particle particle, Object data, Vector offset) implements ParticlePattern {

    @Override
    public void render(ParticleRenderContext context) {
        World world = context.center().getWorld();
        Vector resolvedOffset = offset == null ? new Vector() : offset;
        Location spawn = context.center().clone().add(resolvedOffset);
        ParticleSpawnUtil.spawn(world, spawn, particle, context.points(), data);
    }
}
