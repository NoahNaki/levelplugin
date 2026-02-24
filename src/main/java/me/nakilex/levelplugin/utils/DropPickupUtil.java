package me.nakilex.levelplugin.utils;

import me.nakilex.levelplugin.Main;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class DropPickupUtil {
    private DropPickupUtil() {
    }

    public static void dropForPlayerWithDelayedAutoPickup(Player player, ItemStack stack, long delayTicks) {
        if (player == null || stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
            return;
        }
        World world = player.getWorld();
        Item dropped = world.dropItemNaturally(player.getLocation(), stack.clone());
        scheduleAutoPickup(player, dropped, delayTicks);
    }

    private static void scheduleAutoPickup(Player player, Item dropped, long delayTicks) {
        if (player == null || dropped == null) {
            return;
        }
        long pickupDelayTicks = Math.max(0L, delayTicks);
        dropped.setPickupDelay((int) pickupDelayTicks);

        Main plugin = Main.getInstance();
        if (plugin == null) {
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!dropped.isValid() || dropped.isDead() || !player.isOnline()) {
                return;
            }
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(dropped.getItemStack());
            if (overflow.isEmpty()) {
                dropped.remove();
                return;
            }
            ItemStack remaining = overflow.values().iterator().next();
            dropped.setItemStack(remaining);
            dropped.setPickupDelay(0);
            FullInventoryListener.sendFullInventoryTitle(player, Main.getInstance().getSettingsManager());
        }, pickupDelayTicks);
    }
}
