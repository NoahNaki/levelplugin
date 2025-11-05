package me.nakilex.levelplugin.chat.games;

/** Debug information describing a chat game. */
public record ChatGameStatus(String id, String displayName, boolean enabled, boolean playable) {
}
