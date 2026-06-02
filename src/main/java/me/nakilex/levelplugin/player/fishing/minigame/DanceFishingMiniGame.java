package me.nakilex.levelplugin.player.fishing.minigame;

import me.nakilex.levelplugin.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    private final String correctSound;
    private final String wrongSound;
    private int index;
    public DanceFishingMiniGame(Main plugin, Player player, long durationMs, FileConfiguration c,
                                FishingDifficultyProfile profile, Consumer<Boolean> completion) {
        super(plugin, player, durationMs, "Follow the fishing rhythm!", completion);
        int baseLength = Math.max(1, c.getInt("fishing-mini-games.dance.sequence-length", 4));
        int length = Math.max(3, baseLength + profile.sequenceBonusLength());
        useMovement = c.getBoolean("fishing-mini-games.dance.use-movement", false);
        correctSound = c.getString("fishing-mini-games.dance.sound.correct", "block.amethyst_block.hit");
        wrongSound = c.getString("fishing-mini-games.dance.sound.wrong", "block.anvil.land");
        Movement[] choices = profile.tier() == FishingMiniGameDifficulty.EASY
                ? new Movement[]{Movement.LEFT, Movement.RIGHT}
                : Movement.values();
        for (int i = 0; i < length; i++) sequence.add(choices[ThreadLocalRandom.current().nextInt(choices.length)]);
    }
    @Override protected void tick() {
        Movement current = sequence.get(index);
        updateBar("Fishing rhythm: " + label(current), index / (double) sequence.size());
        if (useResourcePack()) {
            // TODO: Replace the functional text sequence with dedicated dance-sequence glyph layers.
            showGameTitle(sequenceDisplay(), Component.text("Next: ", NamedTextColor.GRAY)
                    .append(Component.text(label(current), NamedTextColor.WHITE)));
            actionBar(Component.text("Left-click: left | Right-click: right | Sneak: jump"));
        } else if (useFallbackTextUi()) {
            actionBar(ChatColor.AQUA + "Next move: " + ChatColor.WHITE + label(current) + ChatColor.GRAY + "  (wrong moves fail)");
        } else {
            actionBar(Component.empty());
        }
    }
    @Override public void handleClick() { accept(Movement.LEFT); }
    @Override public boolean usesRightClickInput() { return true; }
    @Override public void handleRightClick() { accept(Movement.RIGHT); }
    @Override public void handleSneak(boolean sneaking) { if (sneaking) accept(Movement.JUMP); }
    @Override public boolean usesMovementInput() { return useMovement; }
    @Override public void handleMovement(Movement movement) { if (useMovement) accept(movement); }
    private void accept(Movement movement) {
        if (movement != sequence.get(index)) {
            player.playSound(player.getLocation(), wrongSound, 1f, 0.8f);
            finish(false);
            return;
        }
        player.playSound(player.getLocation(), correctSound, 1f, 1.2f);
        if (++index >= sequence.size()) finish(true);
    }
    private Component sequenceDisplay() {
        Component display = Component.empty();
        for (int i = 0; i < sequence.size(); i++) {
            if (i > 0) display = display.append(separator());
            if (i < index) {
                display = display.append(Component.text("✓", NamedTextColor.GREEN));
                continue;
            }
            if (i == index) {
                display = display.append(bracketed(moveWord(sequence.get(i))));
                continue;
            }
            display = display.append(Component.text("[", NamedTextColor.DARK_GRAY)
                    .append(Component.text("?", NamedTextColor.GRAY))
                    .append(Component.text("]", NamedTextColor.DARK_GRAY)));
        }
        return display;
    }
    private Component bracketed(Component inner) {
        return Component.text("[", NamedTextColor.GRAY)
                .append(inner)
                .append(Component.text("]", NamedTextColor.GRAY));
    }
    private Component moveWord(Movement movement) { return Component.text(label(movement), NamedTextColor.WHITE); }
    private Component separator() { return Component.space(); }
    private String label(Movement movement) { return switch (movement) { case LEFT -> "Left-click"; case RIGHT -> "Right-click"; case JUMP -> "Sneak"; }; }
}
