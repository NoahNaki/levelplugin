package me.nakilex.levelplugin.cooking.service;

import me.nakilex.levelplugin.cooking.model.CookingReward;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/** Delivers cooking rewards to inventory with a workstation-location drop fallback. */
public class CookingRewardService {
    public void grantRewards(Player player, Location fallbackDropLocation, List<CookingReward> rewards) {
        if (player == null || rewards == null || rewards.isEmpty()) {
            return;
        }
        Location dropLocation = resolveDropLocation(player, fallbackDropLocation);
        for (CookingReward reward : rewards) {
            if (reward == null || reward.material() == null) {
                continue;
            }
            ItemStack stack = reward.toItemStack();
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
            for (ItemStack leftover : overflow.values()) {
                dropLocation.getWorld().dropItemNaturally(dropLocation, leftover);
            }
        }
    }

    private Location resolveDropLocation(Player player, Location fallbackDropLocation) {
        if (fallbackDropLocation != null && fallbackDropLocation.getWorld() != null) {
            return fallbackDropLocation.clone().add(0.5, 0.5, 0.5);
        }
        World world = player.getWorld();
        return world == null ? player.getLocation() : player.getLocation();
    }
}
