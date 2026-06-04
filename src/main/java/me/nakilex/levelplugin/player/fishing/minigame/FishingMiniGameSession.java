package me.nakilex.levelplugin.player.fishing.minigame;

import me.nakilex.levelplugin.player.fishing.data.FishDefinition;
import org.bukkit.entity.FishHook;

import java.util.UUID;

/** Owns the Bukkit hook and immutable catch context for one active fishing mini-game. */
public record FishingMiniGameSession(FishingMiniGame game, Store store) {

    /** Context retained until the mini-game resolves independently of vanilla fishing events. */
    public record Store(
            UUID playerId,
            FishHook hook,
            boolean inLava,
            FishDefinition hookedFish,
            FishingDifficultyProfile difficultyProfile,
            long startedAtMs,
            long endsAtMs
    ) { }
}
