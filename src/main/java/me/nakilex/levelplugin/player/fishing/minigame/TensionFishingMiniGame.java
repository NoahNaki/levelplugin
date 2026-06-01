package me.nakilex.levelplugin.player.fishing.minigame;

import me.nakilex.levelplugin.Main;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class TensionFishingMiniGame extends AbstractFishingMiniGame {
    private final double pull, tensionGain, tensionDecay, struggleChance, struggleStep, struggleTension;
    private final long gracePeriodMs;
    private double fish, tension;
    private boolean sneaking;
    private int frame;
    public TensionFishingMiniGame(Main plugin, Player player, long durationMs, FileConfiguration c,
                                  FishingDifficultyProfile profile, Consumer<Boolean> completion) {
        super(plugin, player, durationMs, "Tension: hold sneak to reel, release before the line snaps!", completion);
        pull = c.getDouble("fishing-mini-games.tension.pull-per-tick", 0.012) / profile.speedMultiplier();
        tensionGain = c.getDouble("fishing-mini-games.tension.gain-per-tick", 0.018) * profile.decayMultiplier();
        tensionDecay = c.getDouble("fishing-mini-games.tension.decay-per-tick", 0.025) * profile.zoneMultiplier();
        struggleChance = c.getDouble("fishing-mini-games.tension.struggle-chance", 0.035) * profile.speedMultiplier();
        struggleStep = c.getDouble("fishing-mini-games.tension.struggle-step", 0.035) * profile.speedMultiplier();
        struggleTension = c.getDouble("fishing-mini-games.tension.struggle-tension", 0.05);
        fish = switch (profile.tier()) {
            case EASY -> 0.35;
            case NORMAL -> 0.28;
            case HARD -> 0.22;
            case EXTREME -> 0.18;
        };
        gracePeriodMs = switch (profile.tier()) {
            case EASY -> 1500L;
            case NORMAL -> 1000L;
            case HARD -> 500L;
            case EXTREME -> 250L;
        };
    }
    @Override protected void tick() {
        if (ThreadLocalRandom.current().nextDouble() < struggleChance) { fish -= struggleStep; tension += struggleTension; }
        if (sneaking) { fish += pull; tension += tensionGain; } else tension -= tensionDecay;
        if (fish <= 0.0) {
            if (System.currentTimeMillis() < endsAtMs - durationMs + gracePeriodMs) fish = 0.02;
            else { finish(false); return; }
        }
        fish = clamp(fish); tension = clamp(tension);
        updateBar("Reel progress", fish);
        boolean struggling = tension >= 0.68;
        if (useResourcePack()) {
            showGameTitle(FishingGlyphs.strainIcon(tension), FishingGlyphs.tension(fish, struggling, frame++));
            actionBar(Component.text("Hold sneak to pull"));
        } else if (useFallbackTextUi()) {
            actionBar(ChatColor.AQUA + "Tension: " + meter(tension, 18) + ChatColor.GRAY + "  Hold sneak to pull");
        } else {
            actionBar(Component.empty());
        }
        if (tension >= 1.0) finish(false); else if (fish >= 1.0) finish(true);
    }
    @Override public void handleSneak(boolean sneaking) { this.sneaking = sneaking; }
}
