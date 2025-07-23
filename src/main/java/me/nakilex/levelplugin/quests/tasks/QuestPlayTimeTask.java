package me.nakilex.levelplugin.quests.tasks;

import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.player.profile.ProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Periodically awards play time quest progress.
 */
public class QuestPlayTimeTask extends BukkitRunnable {
    private final QuestManager questManager;

    public QuestPlayTimeTask(QuestManager questManager) {
        this.questManager = questManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            questManager.handlePlayTime(player, 1);
            ProfileManager.getInstance().addPlayMinutes(player.getUniqueId(), 1);
        }
    }
}
