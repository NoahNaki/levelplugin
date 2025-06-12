package me.nakilex.levelplugin.leaderboards;

import org.bukkit.scheduler.BukkitRunnable;

public class LeaderboardUpdateTask extends BukkitRunnable {
    private final LeaderboardManager manager;

    public LeaderboardUpdateTask(LeaderboardManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        manager.updateAll();
    }
}
