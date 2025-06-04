package me.nakilex.levelplugin.quests.tasks;

import me.nakilex.levelplugin.quests.data.*;
import me.nakilex.levelplugin.quests.managers.*;
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

            // --- pick quest -------------------------------------------------
            PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId());
            Quest quest = progress != null ? progress.getQuest() : null;

            String tracked = questManager.getTrackedQuest(player.getUniqueId());
            if (tracked != null && (quest == null || !quest.getId().equals(tracked))) {
                quest = questManager.getQuest(tracked);
            }

            // --- pick objective ---------------------------------------------
            Location loc = null;
            if (quest != null) {
                int idx = 0;
                if (progress != null && quest.getId().equals(progress.getQuest().getId())) {
                    // first unfinished objective
                    for (int i = 0; i < quest.getObjectives().size(); i++) {
                        if (progress.getProgress(i) < quest.getObjectives().get(i).getAmount()) {
                            idx = i;
                            break;
                        }
                    }
                }
                loc = quest.getObjectives().get(idx).getBeaconLocation();
            }

            if (loc != null && loc.getWorld().equals(player.getWorld())) {
                Location pLoc = player.getLocation();
                double dist = pLoc.distance(loc);

                // --- dynamic “lead” distance --------------------------------
                Location target = loc;
                if (dist > 64) {                       // far away – point ahead of the player
                    double lead = Math.min(80, dist * 0.6); // between 40 and 80 m
                    Vector dir = loc.toVector().subtract(pLoc.toVector()).setY(0).normalize();
                    target = pLoc.clone().add(dir.multiply(lead));
                    target.setY(pLoc.getY());          // keep beam foot at eye-level terrain
                }

                beaconManager.showBeam(player, target); // rectangular, lime, full height
            }
        }
    }
}
