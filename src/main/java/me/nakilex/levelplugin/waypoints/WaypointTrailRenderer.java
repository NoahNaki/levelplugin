package me.nakilex.levelplugin.waypoints;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Renders a simple particle trail between a player and a destination.
 */
public class WaypointTrailRenderer {
    private final Particle particle;
    private final double step;
    private final double maxDistance;

    public WaypointTrailRenderer(Particle particle, double step, double maxDistance) {
        this.particle = particle;
        this.step = step;
        this.maxDistance = maxDistance;
    }

    public void render(Player player, Location target, double distance) {
        if (player == null || target == null || distance <= 0.1) {
            return;
        }

        Location start = player.getLocation().clone().add(0, 0.2, 0);
        Location end = target.clone().add(0, 0.2, 0);
        double clamped = Math.min(distance, maxDistance);
        int steps = (int) Math.floor(clamped / step);
        if (steps <= 0) {
            return;
        }

        Vector direction = end.toVector().subtract(start.toVector()).normalize().multiply(step);
        Location cursor = start.clone();
        for (int i = 0; i < steps; i++) {
            cursor.add(direction);
            player.spawnParticle(particle, cursor, 1, 0, 0, 0, 0);
        }
    }
}
