package me.nakilex.levelplugin.player.fishing.minigame;

/** Runtime fishing challenge launched after a bite. */
public interface FishingMinigame {
    String id();
    void start();
    void tick();
    void input(FishingMinigameInput input);
    boolean isComplete();
    boolean isSuccessful();
    void dispose();
}
