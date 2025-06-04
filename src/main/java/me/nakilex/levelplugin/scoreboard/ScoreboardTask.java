package me.nakilex.levelplugin.scoreboard;

import org.bukkit.scheduler.BukkitRunnable;

public class ScoreboardTask extends BukkitRunnable {
    private final PlayerScoreboardManager manager;

    public ScoreboardTask(PlayerScoreboardManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        manager.updateAll();
    }
}
