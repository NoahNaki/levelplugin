package me.nakilex.levelplugin.player.fishing.minigame;

import me.nakilex.levelplugin.Main;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.function.Consumer;

public class AccurateClickFishingMiniGame extends AbstractFishingMiniGame {
    private final double speed;
    private final double zoneStart;
    private final double zoneEnd;
    private double position;
    private boolean forward = true;
    public AccurateClickFishingMiniGame(Main plugin, Player player, long durationMs, FileConfiguration config, Consumer<Boolean> completion) {
        super(plugin, player, durationMs, "Precision reel: click inside the green zone!", completion);
        speed = config.getDouble("fishing-mini-games.accurate_click.speed-per-tick", 0.035);
        double width = config.getDouble("fishing-mini-games.accurate_click.zone-width", 0.22);
        zoneStart = 0.5 - width / 2.0;
        zoneEnd = 0.5 + width / 2.0;
    }
    @Override protected void tick() {
        position += forward ? speed : -speed;
        if (position >= 1.0) { position = 1.0; forward = false; }
        if (position <= 0.0) { position = 0.0; forward = true; }
        updateBar("Click inside the green zone!", 1.0 - remainingMs() / (double) durationMs);
        actionBar(ChatColor.AQUA + "Timing: " + pointer(position, zoneStart, zoneEnd, 21));
    }
    @Override public void handleClick() { finish(position >= zoneStart && position <= zoneEnd); }
}
