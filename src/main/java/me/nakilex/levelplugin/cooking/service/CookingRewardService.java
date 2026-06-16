package me.nakilex.levelplugin.cooking.service;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.advancement.AdvancementToastUtil;
import me.nakilex.levelplugin.advancement.model.AdvancementDisplay;
import me.nakilex.levelplugin.codex.CodexManager;
import me.nakilex.levelplugin.cooking.model.CookingReward;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Drops successful cooking rewards at the workstation. */
public class CookingRewardService {
    public void grantRewards(Player player, Location workstationLocation, List<CookingReward> rewards) {
        grantRewards(player, workstationLocation, rewards, 1);
    }

    public void grantRewards(Player player, Location workstationLocation, List<CookingReward> rewards, int craftAmount) {
        if (rewards == null || rewards.isEmpty()) {
            return;
        }
        Location dropLocation = resolveDropLocation(player, workstationLocation);
        if (dropLocation == null || dropLocation.getWorld() == null) {
            return;
        }
        int multiplier = Math.max(1, craftAmount);
        for (CookingReward reward : rewards) {
            if (reward == null || reward.material() == null) {
                continue;
            }
            ItemStack baseStack = reward.toItemStack();
            int totalAmount = Math.max(1, reward.amount() * multiplier);
            dropRewardStacks(dropLocation, baseStack, totalAmount);
            recordFoodDiscovery(player, reward);
        }
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

    private void recordFoodDiscovery(Player player, CookingReward reward) {
        if (player == null || reward == null) {
            return;
        }
        Main plugin = Main.getInstance();
        if (plugin == null || plugin.getCodexManager() == null) {
            return;
        }
        CodexManager codexManager = plugin.getCodexManager();
        String discoveryKey = reward.discoveryKey();
        boolean firstDiscovery = !codexManager.hasDiscoveredFood(player.getUniqueId(), discoveryKey);
        codexManager.recordFood(player, discoveryKey, reward.displayName());
        if (firstDiscovery) {
            AdvancementToastUtil.showToast(player,
                    reward.material() == null ? Material.PAPER : reward.material(),
                    "New Recipe Crafted",
                    reward.displayName(),
                    AdvancementDisplay.FrameType.TASK);
        }
    }

    private Location resolveDropLocation(Player player, Location workstationLocation) {
        if (workstationLocation != null && workstationLocation.getWorld() != null) {
            return workstationLocation.clone().add(0.5D, 1.0D, 0.5D);
        }
        return player == null ? null : player.getLocation();
    }
}
