package me.nakilex.levelplugin.quests.tasks;

import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.quests.managers.BeaconManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.DyeColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;


/**
 * Periodically displays a temporary beacon beam for players at the location of
 * their next quest objective. The effect is client side only and acts as a
 * navigation aid similar to Wynncraft's quest beacons.
 */
public class QuestBeaconTask extends BukkitRunnable {
    private final QuestManager questManager;
    private final BeaconManager beaconManager;

    public QuestBeaconTask(QuestManager questManager, BeaconManager beaconManager) {
        this.questManager = questManager;
        this.beaconManager = beaconManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId());
            Quest quest = progress != null ? progress.getQuest() : null;
            String trackedId = questManager.getTrackedQuest(player.getUniqueId());
            if (trackedId != null && (quest == null || !quest.getId().equals(trackedId))) {
                Quest other = questManager.getQuest(trackedId);
                if (other != null) quest = other;
            }
            Location loc = null;
            if (quest != null) {
                int index = 0;
                if (progress != null && progress.getQuest().getId().equals(quest.getId())) {
                    for (int i = 0; i < quest.getObjectives().size(); i++) {
                        if (progress.getProgress(i) < quest.getObjectives().get(i).getAmount()) {
                            index = i;
                            break;
                        }
                    }
                }
                QuestObjective obj = quest.getObjectives().get(index);
                loc = obj.getBeaconLocation();
            }
            if (loc != null && player.getWorld().equals(loc.getWorld())) {
                Location playerLoc = player.getLocation();
                double dist = playerLoc.distance(loc);
                Location target = loc;
                if (dist > 64) {
                    org.bukkit.util.Vector dir = loc.toVector().subtract(playerLoc.toVector());
                    dir.setY(0).normalize();
                    // Place the temporary beam further ahead when the destination is far away
                    target = playerLoc.clone().add(dir.multiply(40));
                    target.setY(playerLoc.getY());
                }
                beaconManager.showBeam(player, target, DyeColor.WHITE);
            }
        }
    }
}
