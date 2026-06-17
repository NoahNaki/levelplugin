package me.nakilex.levelplugin.cooking.service;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.codex.CodexManager;
import me.nakilex.levelplugin.cooking.model.CookingReward;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Drops successful cooking rewards at the workstation. */
public class CookingRewardService {
    public boolean grantRewards(Player player, Location workstationLocation, List<CookingReward> rewards) {
        return grantRewards(player, workstationLocation, rewards, 1);
    }

    public boolean grantRewards(Player player, Location workstationLocation, List<CookingReward> rewards, int craftAmount) {
        if (rewards == null || rewards.isEmpty()) {
            return false;
        }
        Location dropLocation = resolveDropLocation(player, workstationLocation);
        if (dropLocation == null || dropLocation.getWorld() == null) {
            return false;
        }
        boolean discoveredNewFood = false;
        int multiplier = Math.max(1, craftAmount);
        for (CookingReward reward : rewards) {
            if (reward == null || reward.material() == null) {
                continue;
            }
            ItemStack baseStack = reward.toItemStack();
            int totalAmount = Math.max(1, reward.amount() * multiplier);
            dropRewardStacks(dropLocation, baseStack, totalAmount);
            discoveredNewFood |= recordFoodDiscovery(player, reward);
        }
        return discoveredNewFood;
    }

    private void dropRewardStacks(Location dropLocation, ItemStack baseStack, int totalAmount) {
        if (baseStack == null || baseStack.getType().isAir()) {
            return;
        }
        int remaining = Math.max(1, totalAmount);
        int maxStackSize = Math.max(1, baseStack.getMaxStackSize());
        while (remaining > 0) {
            int amount = Math.min(remaining, maxStackSize);
            ItemStack stack = baseStack.clone();
            stack.setAmount(amount);
            dropLocation.getWorld().dropItemNaturally(dropLocation, stack);
            remaining -= amount;
        }
    }

    private boolean recordFoodDiscovery(Player player, CookingReward reward) {
        if (player == null || reward == null) {
            return false;
        }
        Main plugin = Main.getInstance();
        if (plugin == null || plugin.getCodexManager() == null) {
            return false;
        }
        CodexManager codexManager = plugin.getCodexManager();
        String discoveryKey = reward.discoveryKey();
        boolean firstDiscovery = !codexManager.hasDiscoveredFood(player.getUniqueId(), discoveryKey);
        codexManager.recordFood(player, discoveryKey, reward.displayName());
        return firstDiscovery;
    }

    private Location resolveDropLocation(Player player, Location workstationLocation) {
        if (workstationLocation != null && workstationLocation.getWorld() != null) {
            return workstationLocation.clone().add(0.5D, 1.0D, 0.5D);
        }
        return player == null ? null : player.getLocation();
    }
}
