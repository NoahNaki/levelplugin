package me.nakilex.levelplugin.player.fishing.minigame;

import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;

/** Sneak-controlled tension game: alternate pulling and releasing to remain in the safe range. */
public final class TensionFishingMinigame extends AbstractBossBarFishingMinigame {
    private final FishingMinigameSettings.Tension settings;
    private double tension = 0.5;
    private double progress;
    private boolean pulling;

    public TensionFishingMinigame(FishingMinigameContext context, FishingMinigameSettings.Tension settings) {
        super(context, "Balance your line tension!", BarColor.RED);
        this.settings = settings;
    }

    @Override public String id() { return "tension"; }

    @Override public void tick() {
        if (complete) return;
        if (expired()) { complete = true; return; }
        tension += pulling ? settings.increasePerTick() : -settings.decreasePerTick();
        tension = Math.max(0.0, Math.min(1.0, tension));
        boolean safe = tension >= settings.safeMin() && tension <= settings.safeMax();
        progress = Math.max(0.0, progress + (safe ? settings.progressGain() : -settings.progressLoss()));
        successful = progress >= settings.requiredProgress();
        complete = successful;
        bar.setProgress(Math.min(1.0, progress / settings.requiredProgress()));
        double center = (settings.safeMin() + settings.safeMax()) / 2.0;
        bar.setTitle(ChatColor.RED + "Balance line tension! "
                + FishingGaugeRenderer.render(settings.gauge(), tension, center, settings.safeMax() - settings.safeMin()));
    }

    @Override public void input(FishingMinigameInput input) {
        if (!complete && input == FishingMinigameInput.REEL) { complete = true; return; }
        if (input == FishingMinigameInput.SNEAK_START) pulling = true;
        if (input == FishingMinigameInput.SNEAK_END) pulling = false;
    }
}
