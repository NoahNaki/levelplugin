package me.nakilex.levelplugin.cooking.service;

import me.nakilex.levelplugin.cooking.model.CookingReward;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Drops successful cooking rewards and completion effects at the workstation. */
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
        }
    }

    public void playCompletionEffects(Location workstationLocation) {
        if (workstationLocation == null || workstationLocation.getWorld() == null) {
            return;
        }
        Location center = workstationLocation.clone().add(0.5D, 1.0D, 0.5D);
        World world = center.getWorld();
        world.spawnParticle(Particle.SMOKE, center, 12, 0.25D, 0.25D, 0.25D, 0.01D);
        world.spawnParticle(Particle.FLAME, center, 8, 0.2D, 0.2D, 0.2D, 0.01D);
        world.spawnParticle(Particle.HAPPY_VILLAGER, center.clone().add(0.0D, 0.3D, 0.0D), 6, 0.25D, 0.25D, 0.25D, 0.01D);
    }

    private Location resolveDropLocation(Player player, Location workstationLocation) {
        if (workstationLocation != null && workstationLocation.getWorld() != null) {
            return workstationLocation.clone().add(0.5D, 1.0D, 0.5D);
        }
        return player == null ? null : player.getLocation();
    }
}
