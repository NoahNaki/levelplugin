package me.nakilex.levelplugin.player.woodcutting.animation;

import me.nakilex.levelplugin.player.woodcutting.tree.TreeDetectionResult;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class FallDirectionResolver {
    public Vector resolve(Player player, TreeDetectionResult tree) {
        Location pivot = tree.pivotLocation();
        Vector root = pivot == null ? tree.root().getLocation().toVector() : pivot.toVector();
        Vector playerPos = player.getLocation().toVector();
        Vector awayFromPlayer = root.subtract(playerPos);
        awayFromPlayer.setY(0);
        if (awayFromPlayer.lengthSquared() < 0.01D) return randomHorizontalDirection();
        return awayFromPlayer.normalize();
    }

    private Vector randomHorizontalDirection() {
        double radians = ThreadLocalRandom.current().nextDouble(0.0D, Math.PI * 2.0D);
        return new Vector(Math.cos(radians), 0.0D, Math.sin(radians)).normalize();
    }
}
