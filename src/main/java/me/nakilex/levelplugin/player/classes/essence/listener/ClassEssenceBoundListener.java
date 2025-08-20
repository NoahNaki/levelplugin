package me.nakilex.levelplugin.player.classes.essence.listener;

import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

/** Prevents trading or dropping of soulbound essences. */
public class ClassEssenceBoundListener implements Listener {

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack stack = event.getItemDrop().getItemStack();
        if (ClassEssence.isEssence(stack) && ClassEssence.isSoulbound(stack)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventory(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory().getType() == InventoryType.PLAYER) return;
        ItemStack stack = event.getCurrentItem();
        if (stack != null && ClassEssence.isEssence(stack) && ClassEssence.isSoulbound(stack)) {
            event.setCancelled(true);
        }
    }
}

