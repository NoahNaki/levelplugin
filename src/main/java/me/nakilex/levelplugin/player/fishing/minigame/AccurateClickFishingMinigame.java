package me.nakilex.levelplugin.player.fishing.minigame;

import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;

/** Timing challenge: reel while the moving pointer overlaps the target zone. */
public final class AccurateClickFishingMinigame extends AbstractBossBarFishingMinigame {
    private static final double TARGET_WIDTH = 0.22;
    private static final double POINTER_SPEED = 0.055;
    private final double targetCenter = ThreadLocalRandom.current().nextDouble(0.20, 0.80);
    private double pointer;
    private double direction = 1.0;

    public AccurateClickFishingMinigame(FishingMinigameContext context) {
        super(context, "Time your reel!", BarColor.YELLOW);
    }

    @Override public String id() { return "accurate_click"; }

    @Override
    public void tick() {
        if (complete) return;
        if (expired()) { complete = true; return; }
        pointer += POINTER_SPEED * direction;
        if (pointer >= 1.0) { pointer = 1.0; direction = -1.0; }
        if (pointer <= 0.0) { pointer = 0.0; direction = 1.0; }
        bar.setProgress(pointer);
        bar.setTitle(ChatColor.YELLOW + "Reel in the target! " + renderGauge());
    }

    @Override
    public void reel() {
        if (complete) return;
        successful = Math.abs(pointer - targetCenter) <= TARGET_WIDTH / 2.0;
        complete = true;
    }

    private String renderGauge() {
        StringBuilder gauge = new StringBuilder(ChatColor.DARK_GRAY + "[");
        for (int index = 0; index < 15; index++) {
            double position = index / 14.0;
            if (Math.abs(position - pointer) < 0.04) gauge.append(ChatColor.WHITE).append("|");
            else if (Math.abs(position - targetCenter) <= TARGET_WIDTH / 2.0) gauge.append(ChatColor.GREEN).append("■");
            else gauge.append(ChatColor.GRAY).append("-");
        }
        return gauge.append(ChatColor.DARK_GRAY).append("]").toString();
    }
}
