package me.nakilex.levelplugin.effectdemo;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Handles clicks within the FX Demo GUI and triggers the chosen effect.
 */
public class EffectDemoListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.BLUE + "FX Demo")) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        DemoEffects effect = DemoEffects.bySlot(event.getRawSlot());
        if (effect != null) {
            player.closeInventory();
            effect.play(player);
        }
    }
}
