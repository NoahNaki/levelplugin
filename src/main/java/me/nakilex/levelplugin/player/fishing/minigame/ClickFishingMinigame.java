package me.nakilex.levelplugin.player.fishing.minigame;

import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;

/** Configurable rapid-click challenge with optional v2-style progress decay. */
public final class ClickFishingMinigame extends AbstractBossBarFishingMinigame {
    private final String id;
    private final FishingMinigameSettings.Click settings;
    private double progress;

    public ClickFishingMinigame(String id, FishingMinigameContext context, FishingMinigameSettings.Click settings) {
        super(context, "Click to reel in!", BarColor.PURPLE);
        this.id = id;
        this.settings = settings;
    }

    @Override public String id() { return id; }

    @Override public void tick() {
        if (complete) return;
        if (expired()) complete = true;
        progress = Math.max(0.0, progress - settings.decayPerTick());
        updateBar();
    }

    @Override public void input(FishingMinigameInput input) {
        if (!complete && input == FishingMinigameInput.REEL) { complete = true; return; }
        if (complete || input != FishingMinigameInput.LEFT_CLICK) return;
        progress += settings.progressPerClick();
        successful = progress >= settings.requiredProgress();
        complete = successful;
        updateBar();
    }

    private void updateBar() {
        bar.setProgress(Math.min(1.0, progress / settings.requiredProgress()));
        bar.setTitle(ChatColor.LIGHT_PURPLE + "Click to reel in! " + ChatColor.WHITE + Math.round(progress)
                + ChatColor.GRAY + "/" + ChatColor.WHITE + Math.round(settings.requiredProgress()));
    }
}
