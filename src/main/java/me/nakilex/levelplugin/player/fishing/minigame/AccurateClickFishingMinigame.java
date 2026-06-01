package me.nakilex.levelplugin.player.fishing.minigame;

import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;

/** Configurable timing challenge: reel while the moving pointer overlaps the target zone. */
public final class AccurateClickFishingMinigame extends AbstractBossBarFishingMinigame {
    private final String id;
    private final FishingMinigameSettings.AccurateClick settings;
    private final double targetCenter;
    private double pointer;
    private double direction = 1.0;

    public AccurateClickFishingMinigame(String id, FishingMinigameContext context, FishingMinigameSettings.AccurateClick settings) {
        super(context, "Time your reel!", BarColor.YELLOW);
        this.id = id;
        this.settings = settings;
        this.targetCenter = ThreadLocalRandom.current().nextDouble(settings.minTarget(), settings.maxTarget());
    }

    @Override public String id() { return id; }

    @Override public void tick() {
        if (complete) return;
        if (expired()) { complete = true; return; }
        pointer += settings.pointerSpeed() * direction;
        if (pointer >= 1.0) { pointer = 1.0; direction = -1.0; }
        if (pointer <= 0.0) { pointer = 0.0; direction = 1.0; }
        bar.setProgress(pointer);
        bar.setTitle(ChatColor.YELLOW + "Reel in the target! "
                + FishingGaugeRenderer.render(settings.gauge(), pointer, targetCenter, settings.targetWidth()));
    }

    @Override public void input(FishingMinigameInput input) {
        if (complete || (input != FishingMinigameInput.REEL && input != FishingMinigameInput.RIGHT_CLICK)) return;
        successful = Math.abs(pointer - targetCenter) <= settings.targetWidth() / 2.0;
        complete = true;
    }
}
