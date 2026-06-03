package me.nakilex.levelplugin.player.woodcutting.animation;

import me.nakilex.levelplugin.player.woodcutting.WoodcuttingConfig;
import me.nakilex.levelplugin.player.woodcutting.tree.TreeDetectionResult;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class FallDirectionResolver {
    private final WoodcuttingConfig config;

    public FallDirectionResolver(WoodcuttingConfig config) {
        this.config = config;
    }

    public Vector resolve(Player player, TreeDetectionResult tree) {
        Location pivot = tree.pivotLocation();
        Vector root = pivot == null ? tree.root().getLocation().toVector() : pivot.toVector();
        Vector playerPos = player.getLocation().toVector();
        Vector awayFromPlayer = root.subtract(playerPos);
        awayFromPlayer.setY(0);
        if (awayFromPlayer.lengthSquared() < 0.01D) return randomHorizontalDirection();
        Vector normalized = awayFromPlayer.normalize();
        return switch (config.directionMode()) {
            case FREE -> normalized;
            case EIGHT -> snap(normalized, 8);
            case FOUR -> snap(normalized, 4);
        };
    }

    private Vector snap(Vector vector, int directions) {
        double slice = (Math.PI * 2.0D) / directions;
        double angle = Math.atan2(vector.getZ(), vector.getX());
        double snapped = Math.round(angle / slice) * slice;
        return new Vector(Math.cos(snapped), 0.0D, Math.sin(snapped)).normalize();
    }

    private Vector randomHorizontalDirection() {
        double radians = ThreadLocalRandom.current().nextDouble(0.0D, Math.PI * 2.0D);
        return new Vector(Math.cos(radians), 0.0D, Math.sin(radians)).normalize();
    }
}
