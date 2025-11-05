package me.nakilex.levelplugin.chat.games;

import java.util.UUID;

/** Details returned when a game has been solved. */
public record ChatGameResult(UUID winnerId, String winnerName, String solution, ChatGameReward reward) {
}
