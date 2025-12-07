package me.nakilex.levelplugin.lootchests.listeners;

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
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }

        ItemStack stack = event.getItem();
        if (!lootChestManager.isWand(stack)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        Location loc = event.getClickedBlock().getLocation();
        BlockFace facing = player.getFacing();
        int id = lootChestManager.registerChest(loc, facing);
        player.sendMessage(ChatColor.GREEN + "Registered loot chest #" + id + ChatColor.GRAY + " at "
                + ChatColor.YELLOW + loc.getBlockX() + ChatColor.GRAY + ", "
                + ChatColor.YELLOW + loc.getBlockY() + ChatColor.GRAY + ", "
                + ChatColor.YELLOW + loc.getBlockZ());
    }
}
