package me.nakilex.levelplugin.fishing.api.game;

public interface FishingGame {
    void start();

    void handlePlayerAction();

    void cancel();
}
