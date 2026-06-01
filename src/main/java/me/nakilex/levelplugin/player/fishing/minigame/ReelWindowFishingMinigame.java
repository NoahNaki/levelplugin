package me.nakilex.levelplugin.player.fishing.minigame;

import org.bukkit.boss.BarColor;

/** Backwards-compatible reaction window: reel before the countdown expires. */
public final class ReelWindowFishingMinigame extends AbstractBossBarFishingMinigame {
    public ReelWindowFishingMinigame(FishingMinigameContext context) { super(context, "Reel in!", BarColor.BLUE); }
    @Override public String id() { return "simple_reel"; }
    @Override public void tick() { bar.setProgress(remainingProgress()); if (expired()) complete = true; }
    @Override public void input(FishingMinigameInput input) {
        if (complete || (input != FishingMinigameInput.REEL && input != FishingMinigameInput.RIGHT_CLICK)) return;
        successful = !expired();
        complete = true;
    }
}
