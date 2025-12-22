package me.nakilex.levelplugin.utils;

import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLevelChangeEvent; // Replace with your actual level up event if needed
import org.bukkit.inventory.ItemStack;

public class CustomItemUpdateListener implements Listener {

    @EventHandler
    public void onPlayerLevelChange(PlayerLevelChangeEvent event) {
        Player player = event.getPlayer();
        // Loop through every slot in the player's inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) {
                continue;
            }
            boolean isCustomItem = ItemUtil.getCustomItemId(item) != -1;
            boolean isCustomTool = me.nakilex.levelplugin.items.tools.ToolManager.getInstance().isToolMaterial(item.getType());
            if (isCustomItem || isCustomTool) {
                // Update the tooltip to reflect the new player level (and class, if applicable)
                ItemUtil.updateTooltip(item, player);
            }
        }
        // Force the inventory to update so that the changes are visible
        player.updateInventory();
    }
}
