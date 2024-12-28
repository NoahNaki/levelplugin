package me.nakilex.levelplugin.effects.tasks;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class SwordFireTask extends BukkitRunnable {

    private final List<ArmorStand> armorStands;
    private final double speed = 0.5;
    private int ticks = 0;

    public SwordFireTask(List<ArmorStand> armorStands) {
        this.armorStands = armorStands;
    }

    @Override
    public void run() {
        if (armorStands.isEmpty() || ticks > 40) { // Runs for 2 seconds (40 ticks)
            this.cancel();
            armorStands.forEach(ArmorStand::remove);
            return;
        }

        for (ArmorStand armorStand : armorStands) {
            Location loc = armorStand.getLocation();
            // Move in the direction the armor stand is facing
            loc.add(loc.getDirection().multiply(speed));
            armorStand.teleport(loc);
        }
        ticks++;
    }

}