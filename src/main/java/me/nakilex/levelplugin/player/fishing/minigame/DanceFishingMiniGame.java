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

public class DanceFishingMiniGame extends AbstractFishingMiniGame {
    private final List<Movement> sequence = new ArrayList<>();
    private final boolean useMovement;
    private int index;
    public DanceFishingMiniGame(Main plugin, Player player, long durationMs, FileConfiguration c, Consumer<Boolean> completion) {
        super(plugin, player, durationMs, "Follow the fishing rhythm!", completion);
        int length = Math.max(1, c.getInt("fishing-mini-games.dance.sequence-length", 5));
        useMovement = c.getBoolean("fishing-mini-games.dance.use-movement", false);
        Movement[] choices = Movement.values();
        for (int i = 0; i < length; i++) sequence.add(choices[ThreadLocalRandom.current().nextInt(choices.length)]);
    }
    @Override protected void tick() {
        Movement current = sequence.get(index);
        updateBar("Fishing rhythm: " + label(current), index / (double) sequence.size());
        if (useResourcePack()) {
            showGameTitle(Component.text(sequenceDisplay()), Component.text("Next: " + label(current)));
            actionBar(Component.text("Left-click: left | Right-click: right | Sneak: jump"));
        } else if (useFallbackTextUi()) {
            actionBar(ChatColor.AQUA + "Next move: " + ChatColor.WHITE + label(current) + ChatColor.GRAY + "  (wrong moves fail)");
        } else {
            actionBar(Component.empty());
        }
    }
    @Override public void handleClick() { accept(Movement.LEFT); }
    @Override public void handleRightClick() { accept(Movement.RIGHT); }
    @Override public void handleSneak(boolean sneaking) { if (sneaking) accept(Movement.JUMP); }
    @Override public void handleMovement(Movement movement) { if (useMovement) accept(movement); }
    private void accept(Movement movement) {
        if (movement != sequence.get(index)) { finish(false); return; }
        if (++index >= sequence.size()) finish(true);
    }
    private String sequenceDisplay() {
        StringBuilder display = new StringBuilder();
        for (int i = 0; i < sequence.size(); i++) {
            if (i > 0) display.append(' ');
            display.append(i < index ? "[✓]" : i == index ? "[CURRENT]" : "[?]");
        }
        return display.toString();
    }
    private String label(Movement movement) { return switch (movement) { case LEFT -> "Left-click"; case RIGHT -> "Right-click"; case JUMP -> "Sneak"; }; }
}
