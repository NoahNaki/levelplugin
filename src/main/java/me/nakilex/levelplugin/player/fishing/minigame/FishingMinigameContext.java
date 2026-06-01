package me.nakilex.levelplugin.player.fishing.minigame;

import org.bukkit.entity.Player;

/** Immutable values shared by fishing minigames for one bite. */
public record FishingMinigameContext(Player player, long durationMs) {
    public FishingMinigameContext {
        durationMs = Math.max(500L, durationMs);
    }
}
