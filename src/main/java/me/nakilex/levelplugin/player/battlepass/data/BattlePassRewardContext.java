package me.nakilex.levelplugin.player.battlepass.data;

import me.nakilex.levelplugin.items.managers.ItemManager;

/**
 * Provides lookup helpers so reward definitions can resolve names for
 * additional systems without tightly coupling to concrete managers.
 */
public record BattlePassRewardContext(
        ItemManager itemManager
) {
}
