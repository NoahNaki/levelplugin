package me.nakilex.levelplugin.player.fishing.minigame;

import me.nakilex.levelplugin.Main;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class DanceFishingMiniGame extends AbstractFishingMiniGame {
    private final List<Movement> sequence = new ArrayList<>();
    private int index;
    public DanceFishingMiniGame(Main plugin, Player player, long durationMs, FileConfiguration c, Consumer<Boolean> completion) {
        super(plugin, player, durationMs, "Follow the fishing rhythm!", completion);
        int length = Math.max(1, c.getInt("fishing-mini-games.dance.sequence-length", 5));
        Movement[] choices = Movement.values();
        for (int i = 0; i < length; i++) sequence.add(choices[ThreadLocalRandom.current().nextInt(choices.length)]);
    }
    @Override protected void tick() {
        updateBar("Fishing rhythm: " + label(sequence.get(index)), index / (double) sequence.size());
        actionBar(ChatColor.AQUA + "Next move: " + ChatColor.WHITE + label(sequence.get(index)) + ChatColor.GRAY + "  (wrong moves fail)");
    }
    @Override public void handleSneak(boolean sneaking) { if (sneaking) accept(Movement.JUMP); }
    @Override public void handleMovement(Movement movement) { accept(movement); }
    private void accept(Movement movement) {
        if (movement != sequence.get(index)) { finish(false); return; }
        if (++index >= sequence.size()) finish(true);
    }
    private String label(Movement movement) { return switch (movement) { case LEFT -> "Move left"; case RIGHT -> "Move right"; case JUMP -> "Jump or sneak"; }; }
}
