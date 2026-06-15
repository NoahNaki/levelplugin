package me.nakilex.levelplugin.cooking.minigame;

/** Immutable title/subtitle payload for player-facing cooking mini-game visuals. */
public record CookingMiniGameVisual(String title, String subtitle) {
    public CookingMiniGameVisual {
        title = title == null ? "" : title;
        subtitle = subtitle == null ? "" : subtitle;
    }
}
