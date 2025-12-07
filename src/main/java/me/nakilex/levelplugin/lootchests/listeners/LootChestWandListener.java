package me.nakilex.levelplugin.lootchests.listeners;

import me.nakilex.levelplugin.lootchests.data.ChestData;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Allows builders to register loot chest locations in-game using the /lootchest wand.
 */
public class LootChestWandListener implements Listener {

    private final LootChestManager lootChestManager;

    public LootChestWandListener(LootChestManager lootChestManager) {
        this.lootChestManager = lootChestManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        ItemStack stack = event.getItem();
        if (!lootChestManager.isWand(stack)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Location loc = event.getClickedBlock().getLocation();
            BlockFace facing = player.getFacing();
            int id = lootChestManager.registerChest(loc, facing);
            player.sendMessage(ChatColor.GREEN + "Registered loot chest #" + id + ChatColor.GRAY + " at "
                    + ChatColor.YELLOW + loc.getBlockX() + ChatColor.GRAY + ", "
                    + ChatColor.YELLOW + loc.getBlockY() + ChatColor.GRAY + ", "
                    + ChatColor.YELLOW + loc.getBlockZ());
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Location reference = event.getClickedBlock().getLocation();
            Integer nearestId = lootChestManager.findNearestChestId(reference);
            if (nearestId == null) {
                player.sendMessage(ChatColor.RED + "No loot chests found to delete in this world.");
                return;
            }

            Location chestLoc = lootChestManager.getLocationForChestId(nearestId);
            lootChestManager.deleteChest(nearestId);

            if (chestLoc == null && lootChestManager.getAllChestData() != null) {
                // Fallback to config data if the chest wasn't currently spawned
                for (ChestData data : lootChestManager.getAllChestData()) {
                    if (data.getChestId() == nearestId) {
                        chestLoc = data.toLocation();
                        break;
                    }
                }
            }

            String position = chestLoc != null
                    ? ChatColor.YELLOW + "" + chestLoc.getBlockX() + ChatColor.GRAY + ", "
                    + ChatColor.YELLOW + chestLoc.getBlockY() + ChatColor.GRAY + ", "
                    + ChatColor.YELLOW + chestLoc.getBlockZ()
                    : ChatColor.GRAY + "unknown location";

            player.sendMessage(ChatColor.RED + "Deleted loot chest #" + nearestId + ChatColor.GRAY + " at " + position);
        }
    }
}
