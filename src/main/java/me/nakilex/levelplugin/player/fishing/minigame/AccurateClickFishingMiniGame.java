package me.nakilex.levelplugin.player.fishing.minigame;

import me.nakilex.levelplugin.Main;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class AccurateClickFishingMiniGame extends AbstractFishingMiniGame {
    private static final List<String> DEFAULT_BARS = List.of("BAR_1", "BAR_2", "BAR_3", "BAR_4", "BAR_5", "BAR_6", "BAR_7", "BAR_8", "BAR_9");

    private final double speed;
    private final double zoneStart;
    private final double zoneEnd;
    private final FishingGlyphs.AccurateClickBar glyphBar;
    private double position;
    private boolean forward = true;
    public AccurateClickFishingMiniGame(Main plugin, Player player, long durationMs, FileConfiguration config, Consumer<Boolean> completion) {
        super(plugin, player, durationMs, "Precision reel: click inside the green zone!", completion);
        speed = config.getDouble("fishing-mini-games.accurate_click.speed-per-tick", 0.035);
        double width = config.getDouble("fishing-mini-games.accurate_click.zone-width", 0.22);
        zoneStart = 0.5 - width / 2.0;
        zoneEnd = 0.5 + width / 2.0;
        List<String> bars = new ArrayList<>(config.getStringList("fishing-mini-games.accurate_click.bars"));
        if (bars.isEmpty()) bars.addAll(DEFAULT_BARS);
        glyphBar = FishingGlyphs.accurateClickBar(bars.get(ThreadLocalRandom.current().nextInt(bars.size())));
    }
    @Override protected void tick() {
        position += forward ? speed : -speed;
        if (position >= 1.0) { position = 1.0; forward = false; }
        if (position <= 0.0) { position = 0.0; forward = true; }
        updateBar("Click inside the green zone!", 1.0 - remainingMs() / (double) durationMs);
        if (useResourcePack()) {
            showGameTitle(Component.text("Click in the green zone!"), FishingGlyphs.accurateClick(glyphBar, position));
            actionBar(Component.empty());
        } else if (useFallbackTextUi()) {
            actionBar(ChatColor.AQUA + "Timing: " + pointer(position, zoneStart, zoneEnd, 21));
        } else {
            actionBar(Component.empty());
        }
    }
    @Override public void handleClick() { finish(useResourcePack() ? glyphBar.isHit(position) : position >= zoneStart && position <= zoneEnd); }
}
