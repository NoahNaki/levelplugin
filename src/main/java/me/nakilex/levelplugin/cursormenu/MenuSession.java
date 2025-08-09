package me.nakilex.levelplugin.cursormenu;

import java.util.UUID;

/**
 * Tracks an active menu for a player. Focus and cooldown handling can be
 * expanded later as needed.
 */
public class MenuSession {
    private final UUID playerId;
    private final MenuDefinition menu;

    public MenuSession(UUID playerId, MenuDefinition menu) {
        this.playerId = playerId;
        this.menu = menu;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public MenuDefinition getMenu() {
        return menu;
    }
}
