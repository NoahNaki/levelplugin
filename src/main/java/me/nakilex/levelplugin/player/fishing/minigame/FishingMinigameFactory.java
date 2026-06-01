package me.nakilex.levelplugin.player.fishing.minigame;

@FunctionalInterface
public interface FishingMinigameFactory {
    FishingMinigame create(FishingMinigameContext context);
}
