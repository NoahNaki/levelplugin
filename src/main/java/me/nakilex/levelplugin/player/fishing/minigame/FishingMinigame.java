package me.nakilex.levelplugin.player.fishing.minigame;

/** Runtime fishing challenge launched after a bite. */
public interface FishingMinigame {
    String id();
    void start();
    void tick();
    void reel();
    boolean isComplete();
    boolean isSuccessful();
    void dispose();
}
