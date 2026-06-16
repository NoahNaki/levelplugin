package me.nakilex.levelplugin.cooking.service;

import me.nakilex.levelplugin.cooking.model.CookingReward;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Drops successful cooking rewards at the workstation. */
public class CookingRewardService {
    public void grantRewards(Player player, Location workstationLocation, List<CookingReward> rewards) {
        if (rewards == null || rewards.isEmpty()) {
            return;
        }
        Location dropLocation = resolveDropLocation(player, workstationLocation);
        if (dropLocation == null || dropLocation.getWorld() == null) {
            return;
        }
        for (CookingReward reward : rewards) {
            if (reward == null || reward.material() == null) {
                continue;
            }
            ItemStack stack = reward.toItemStack();
            dropLocation.getWorld().dropItemNaturally(dropLocation, stack);
            if (player != null) {
                me.nakilex.levelplugin.Main.getInstance().getCodexManager().recordFood(player, reward.discoveryKey(), displayName(reward));
            }
        }
    }

    private String displayName(CookingReward reward) {
        return reward.nexoItemIdOptional()
                .map(id -> id.replace('_', ' '))
                .map(name -> Character.toUpperCase(name.charAt(0)) + name.substring(1))
                .orElseGet(() -> reward.material().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' '));
    }

    private Location resolveDropLocation(Player player, Location workstationLocation) {
        if (workstationLocation != null && workstationLocation.getWorld() != null) {
            return workstationLocation.clone().add(0.5D, 1.0D, 0.5D);
        }
        return player == null ? null : player.getLocation();
    }
}
