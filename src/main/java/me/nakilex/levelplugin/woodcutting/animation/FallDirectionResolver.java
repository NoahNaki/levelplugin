package me.nakilex.levelplugin.woodcutting.animation;

import me.nakilex.levelplugin.woodcutting.tree.TreeDetectionResult;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class FallDirectionResolver {
    public Vector resolve(Player player, TreeDetectionResult tree) {
        Vector root = tree.root().getLocation().toVector();
        Vector playerPos = player.getLocation().toVector();
        Vector awayFromPlayer = root.subtract(playerPos);
        awayFromPlayer.setY(0);
        if (awayFromPlayer.lengthSquared() < 0.01D) return randomHorizontalDirection();
        return snapCardinal(awayFromPlayer.normalize());
    }

    private Vector snapCardinal(Vector vector) {
        if (Math.abs(vector.getX()) >= Math.abs(vector.getZ())) return new Vector(Math.signum(vector.getX()), 0, 0);
        return new Vector(0, 0, Math.signum(vector.getZ()));
    }

    private Vector randomHorizontalDirection() {
        return switch (ThreadLocalRandom.current().nextInt(4)) {
            case 0 -> new Vector(1, 0, 0);
            case 1 -> new Vector(-1, 0, 0);
            case 2 -> new Vector(0, 0, 1);
            default -> new Vector(0, 0, -1);
        };
    }
}
