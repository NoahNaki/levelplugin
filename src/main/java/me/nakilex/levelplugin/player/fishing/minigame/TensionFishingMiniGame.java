package me.nakilex.levelplugin.player.fishing.minigame;

import me.nakilex.levelplugin.Main;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class TensionFishingMiniGame extends AbstractFishingMiniGame {
    private final double pull, tensionGain, tensionDecay, struggleChance, struggleStep, struggleTension;
    private double fish = 0.18, tension;
    private boolean sneaking;
    private int frame;
    public TensionFishingMiniGame(Main plugin, Player player, long durationMs, FileConfiguration c, Consumer<Boolean> completion) {
        super(plugin, player, durationMs, "Tension: hold sneak to reel, release before the line snaps!", completion);
        pull = c.getDouble("fishing-mini-games.tension.pull-per-tick", 0.012);
        tensionGain = c.getDouble("fishing-mini-games.tension.gain-per-tick", 0.018);
        tensionDecay = c.getDouble("fishing-mini-games.tension.decay-per-tick", 0.025);
        struggleChance = c.getDouble("fishing-mini-games.tension.struggle-chance", 0.035);
        struggleStep = c.getDouble("fishing-mini-games.tension.struggle-step", 0.035);
        struggleTension = c.getDouble("fishing-mini-games.tension.struggle-tension", 0.05);
    }
    @Override protected void tick() {
        if (ThreadLocalRandom.current().nextDouble() < struggleChance) { fish -= struggleStep; tension += struggleTension; }
        if (sneaking) { fish += pull; tension += tensionGain; } else tension -= tensionDecay;
        fish = clamp(fish); tension = clamp(tension);
        updateBar("Reel progress", fish);
        boolean struggling = tension >= 0.68;
        if (useResourcePack()) {
            actionBar(FishingGlyphs.tension(fish, struggling, frame++)
                    .append(Component.text("  ")).append(FishingGlyphs.strain(tension))
                    .append(Component.text("  Hold sneak to pull")));
        } else if (useFallbackTextUi()) {
            actionBar(ChatColor.AQUA + "Tension: " + meter(tension, 18) + ChatColor.GRAY + "  Hold sneak to pull");
        } else {
            actionBar(Component.empty());
        }
        if (tension >= 1.0 || fish <= 0.0) finish(false); else if (fish >= 1.0) finish(true);
    }
    @Override public void handleSneak(boolean sneaking) { this.sneaking = sneaking; }
}
