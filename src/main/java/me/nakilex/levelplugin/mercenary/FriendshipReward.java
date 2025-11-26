package me.nakilex.levelplugin.mercenary;

import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Represents rewards granted when a player claims a friendship milestone.
 */
public record FriendshipReward(int coins, int experience, List<ItemStack> items) {
    public FriendshipReward {
        if (items == null) {
            items = List.of();
        }
    }
}
