package me.nakilex.levelplugin.stageddungeon;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface RewardGrant {
    void grant(Player player, int amount);
}
