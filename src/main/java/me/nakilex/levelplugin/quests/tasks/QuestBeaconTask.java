package me.nakilex.levelplugin.quests.tasks;

import me.nakilex.levelplugin.quests.managers.BeaconManager;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.quests.util.QuestTargetResolver;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Runs once a tick to keep the rectangular lime beacon “solid”.
 */
public class QuestBeaconTask extends BukkitRunnable {

    private final QuestManager  questManager;
    private final BeaconManager beaconManager;

    public QuestBeaconTask(QuestManager questManager, BeaconManager beaconManager) {
        this.questManager  = questManager;
        this.beaconManager = beaconManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            QuestTargetResolver.QuestTarget questTarget = QuestTargetResolver.resolve(player, questManager);
            Location loc = questTarget != null ? questTarget.location() : null;

            if (loc != null && loc.getWorld() != null && loc.getWorld().equals(player.getWorld())) {
                Location pLoc = player.getLocation();
                double dist = pLoc.distance(loc);

                // hide beam when player is close enough
                if (dist < 10) {
                    beaconManager.removeBeam(player);
                    continue;
                }

                // --- dynamic “lead” distance --------------------------------
                Location beaconTarget = loc;
                if (dist > 64) {                       // far away – point ahead of the player
                    double lead = Math.min(80, dist * 0.6); // between 40 and 80 m
                    Vector dir = loc.toVector().subtract(pLoc.toVector()).setY(0).normalize();
                    beaconTarget = pLoc.clone().add(dir.multiply(lead));
                    beaconTarget.setY(pLoc.getY());          // keep beam foot at eye-level terrain
                }

                beaconManager.showBeam(player, beaconTarget); // rectangular, lime, full height
            } else {
                beaconManager.removeBeam(player);
            }
        }
    }
}
