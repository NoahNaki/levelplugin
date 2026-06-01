package me.nakilex.levelplugin.player.fishing.minigame;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;

/** Quick-time sequence game using inputs Bukkit can reliably observe without packet hooks. */
public final class DanceFishingMinigame extends AbstractBossBarFishingMinigame {
    private static final List<FishingMinigameInput> AVAILABLE_INPUTS = List.of(
            FishingMinigameInput.LEFT_CLICK, FishingMinigameInput.SNEAK_START, FishingMinigameInput.SNEAK_END);
    private final List<FishingMinigameInput> sequence = new ArrayList<>();
    private int index;

    public DanceFishingMinigame(FishingMinigameContext context, FishingMinigameSettings.Dance settings) {
        super(context, "Follow the fishing rhythm!", BarColor.WHITE);
        for (int step = 0; step < settings.sequenceLength(); step++) {
            sequence.add(AVAILABLE_INPUTS.get(ThreadLocalRandom.current().nextInt(AVAILABLE_INPUTS.size())));
        }
    }

    @Override public String id() { return "dance"; }

    @Override public void start() { super.start(); updateBar(); }

    @Override public void tick() {
        if (!complete && expired()) complete = true;
    }

    @Override public void input(FishingMinigameInput input) {
        if (!complete && input == FishingMinigameInput.REEL) { complete = true; return; }
        if (complete || input == FishingMinigameInput.REEL || input == FishingMinigameInput.RIGHT_CLICK) return;
        if (input == sequence.get(index)) index++;
        else index = 0;
        successful = index >= sequence.size();
        complete = successful;
        updateBar();
    }

    private void updateBar() {
        bar.setProgress(index / (double) sequence.size());
        bar.setTitle(ChatColor.AQUA + "Fishing rhythm: " + ChatColor.WHITE + prompt(sequence.get(index < sequence.size() ? index : sequence.size() - 1))
                + ChatColor.DARK_GRAY + " [" + ChatColor.GRAY + index + "/" + sequence.size() + ChatColor.DARK_GRAY + "]");
    }

    private String prompt(FishingMinigameInput input) {
        return switch (input) {
            case LEFT_CLICK -> "LEFT CLICK";
            case SNEAK_START -> "SNEAK";
            case SNEAK_END -> "RELEASE SNEAK";
            default -> input.name();
        };
    }
}
