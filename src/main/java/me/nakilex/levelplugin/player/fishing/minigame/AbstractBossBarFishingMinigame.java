package me.nakilex.levelplugin.player.fishing.minigame;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;

/** Shared BossBar lifecycle for fishing challenges. */
abstract class AbstractBossBarFishingMinigame implements FishingMinigame {
    protected final FishingMinigameContext context;
    protected final BossBar bar;
    protected final long expiresAtMs;
    protected boolean complete;
    protected boolean successful;

    protected AbstractBossBarFishingMinigame(FishingMinigameContext context, String title, BarColor color) {
        this.context = context;
        this.bar = Bukkit.createBossBar(title, color, BarStyle.SOLID);
        this.expiresAtMs = System.currentTimeMillis() + context.durationMs();
    }

    @Override public void start() { bar.addPlayer(context.player()); bar.setVisible(true); }
    @Override public boolean isComplete() { return complete; }
    @Override public boolean isSuccessful() { return successful; }
    @Override public void dispose() { bar.removeAll(); }

    protected double remainingProgress() {
        return Math.max(0.0, Math.min(1.0, (expiresAtMs - System.currentTimeMillis()) / (double) context.durationMs()));
    }

    protected boolean expired() { return System.currentTimeMillis() > expiresAtMs; }
}
