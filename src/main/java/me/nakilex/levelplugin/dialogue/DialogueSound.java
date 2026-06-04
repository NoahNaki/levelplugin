package me.nakilex.levelplugin.dialogue;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public record DialogueSound(String soundKey, float volume, float pitch) {
    public static final DialogueSound UI_CLICK = new DialogueSound("UI_BUTTON_CLICK", 1.0f, 1.0f);
    public static final DialogueSound UI_SELECT = new DialogueSound("UI_BUTTON_CLICK", 0.8f, 1.25f);

    public void play(Player player) {
        if (player == null || soundKey == null || soundKey.isBlank()) return;
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundKey.toUpperCase(java.util.Locale.ROOT)), volume, pitch);
        } catch (IllegalArgumentException ignored) {
            player.playSound(player.getLocation(), soundKey, volume, pitch);
        }
    }
}
