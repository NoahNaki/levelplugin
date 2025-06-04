package me.nakilex.levelplugin.quests.tasks;

import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Periodically sends temporary beacon blocks to players at the location of
 * their next quest objective. This is purely client side and acts as a
 * navigation aid similar to Wynncraft's compass beacons.
 */
public class QuestBeaconTask extends BukkitRunnable {
    private final QuestManager questManager;
    private final Map<UUID, Location> last = new HashMap<>();

    public QuestBeaconTask(QuestManager questManager) {
        this.questManager = questManager;
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
            Location old = last.get(player.getUniqueId());
            if (old != null && (loc == null || !old.equals(loc))) {
                player.sendBlockChange(old, old.getBlock().getBlockData());
                Location base = old.clone().add(0, -1, 0);
                player.sendBlockChange(base, base.getBlock().getBlockData());
                last.remove(player.getUniqueId());
            }
            if (loc != null) {
                last.put(player.getUniqueId(), loc);
                player.sendBlockChange(loc, Material.BEACON.createBlockData());
                Location base = loc.clone().add(0, -1, 0);
                player.sendBlockChange(base, Material.IRON_BLOCK.createBlockData());
            }
        }
    }
}
