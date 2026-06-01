package me.nakilex.levelplugin.player.fishing.minigame;

import me.nakilex.levelplugin.Main;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.function.Consumer;

public class ClickFishingMiniGame extends AbstractFishingMiniGame {
    private final int requiredClicks;
    private int clicks;

    public ClickFishingMiniGame(Main plugin, Player player, long durationMs, FileConfiguration config, Consumer<Boolean> completion) {
        super(plugin, player, durationMs, "Fish struggle: left-click rapidly!", completion);
        requiredClicks = Math.max(1, config.getInt("fishing-mini-games.click.required-clicks", 12));
    }
    @Override protected void tick() {
        double progress = clicks / (double) requiredClicks;
        updateBar("Left-click rapidly! " + clicks + "/" + requiredClicks, progress);
        actionBar(ChatColor.AQUA + "Clicks remaining: " + ChatColor.WHITE + Math.max(0, requiredClicks - clicks));
    }
    @Override public void handleClick() { if (++clicks >= requiredClicks) finish(true); }
}
