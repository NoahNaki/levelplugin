package me.nakilex.levelplugin.effects.tasks;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.List;

public class SwordCircleTask extends BukkitRunnable {

    private final List<ArmorStand> armorStands;
    private final Player player;
    private double angle = 0;
    private final double speed = Math.PI / 16;
    private final double radius = 2.0;

    public SwordCircleTask(List<ArmorStand> armorStands, Player player) {
        this.armorStands = armorStands;
        this.player = player;
    }

    @Override
    public void run() {
        if (!player.isOnline() || armorStands.isEmpty()) {
            this.cancel();
            return;
        }

        Location center = player.getLocation(); // Follow player movement
        angle += speed;
        for (int i = 0; i < armorStands.size(); i++) {
            double offsetAngle = angle + (2 * Math.PI * i / armorStands.size());
            double x = center.getX() + radius * Math.cos(offsetAngle);
            double z = center.getZ() + radius * Math.sin(offsetAngle);
            Location loc = new Location(center.getWorld(), x, center.getY(), z);
            armorStands.get(i).teleport(loc); // Update each sword position

            // Make armor stand face away from the player
            double direction = Math.toDegrees(Math.atan2(center.getZ() - z, center.getX() - x)) - 270;
            armorStands.get(i).setRotation((float) direction, 0);
        }
    }
}