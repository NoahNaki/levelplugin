package me.nakilex.levelplugin.player.fishing.minigame;

import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;

/** Configurable sneak- or click-controlled physics game: keep the pointer in the safe zone until progress fills. */
public final class HoldFishingMinigame extends AbstractBossBarFishingMinigame {
    private final String id;
    private final FishingMinigameSettings.Hold settings;
    private double pointer = 0.15;
    private double velocity;
    private double progress;
    private boolean pulling;

    public HoldFishingMinigame(String id, FishingMinigameContext context, FishingMinigameSettings.Hold settings) {
        super(context, settings.clickControl() ? "Click to control the line!" : "Hold sneak to control the line!", BarColor.GREEN);
        this.id = id;
        this.settings = settings;
    }

    @Override public String id() { return id; }

    @Override public void tick() {
        if (complete) return;
        if (expired()) { complete = true; return; }
        velocity += !settings.clickControl() && pulling ? settings.pullingStrength() : -settings.waterResistance();
        velocity = Math.max(-settings.maxVelocity(), Math.min(settings.maxVelocity(), velocity));
        pointer += velocity;
        if (pointer >= 1.0) { pointer = 1.0; velocity *= -0.5; }
        if (pointer <= 0.0) { pointer = 0.0; velocity *= -0.5; }
        boolean inTarget = Math.abs(pointer - 0.5) <= settings.targetWidth() / 2.0;
        progress = Math.max(0.0, progress + (inTarget ? settings.progressGain() : -settings.progressLoss()));
        successful = progress >= settings.requiredProgress();
        complete = successful;
        bar.setProgress(Math.min(1.0, progress / settings.requiredProgress()));
        bar.setTitle(ChatColor.GREEN + "Keep the pointer in the zone! "
                + FishingGaugeRenderer.render(settings.gauge(), pointer, 0.5, settings.targetWidth()));
    }

    @Override public void input(FishingMinigameInput input) {
        if (!complete && input == FishingMinigameInput.REEL) { complete = true; return; }
        if (settings.clickControl() && input == FishingMinigameInput.LEFT_CLICK) {
            velocity = Math.min(settings.maxVelocity(), velocity + settings.pullingStrength());
        }
        if (!settings.clickControl() && input == FishingMinigameInput.SNEAK_START) pulling = true;
        if (!settings.clickControl() && input == FishingMinigameInput.SNEAK_END) pulling = false;
    }
}
