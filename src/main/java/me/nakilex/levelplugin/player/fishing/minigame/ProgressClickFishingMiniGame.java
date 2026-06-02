package me.nakilex.levelplugin.player.fishing.minigame;

import me.nakilex.levelplugin.Main;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import java.util.function.Consumer;

public class ProgressClickFishingMiniGame extends AbstractFishingMiniGame {
    private final double clickGain;
    private final double decay;
    private double progress;
    public ProgressClickFishingMiniGame(Main plugin, Player player, long durationMs, FileConfiguration config, FishingDifficultyProfile profile, Consumer<Boolean> completion) {
        super(plugin, player, durationMs, "Keep reeling: left-click to hold the line!", completion);
        clickGain = config.getDouble("fishing-mini-games.click_v2.click-gain", 0.10) * profile.zoneMultiplier();
        decay = config.getDouble("fishing-mini-games.click_v2.decay-per-tick", 0.012) * profile.decayMultiplier();
        progress = config.getDouble("fishing-mini-games.click_v2.start-progress", 0.35);
    }
    @Override protected void tick() {
        progress = clamp(progress - decay);
        updateBar("Keep reeling!", progress);
        if (useResourcePack()) {
            showGameTitle(FishingGlyphs.progressIcon(progress), Component.text("Keep reeling!"));
            actionBar(Component.text("Keep clicking!"));
        } else if (useFallbackTextUi()) {
            actionBar(ChatColor.AQUA + "Line control: " + meter(progress, 18));
        } else {
            actionBar(Component.empty());
        }
        if (progress <= 0.0) finish(false);
    }
    /** click_v2 is a survival challenge: keep any line progress until the configured timeout. */
    @Override protected boolean timeoutSuccess() { return progress > 0.0; }
    @Override public void handleClick() { progress = clamp(progress + clickGain); }
}
