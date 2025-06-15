package me.nakilex.levelplugin.effectdemo;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;

/**
 * Handles clicks within the FX Demo GUI and triggers the chosen effect.
 */
public class EffectDemoListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        if (!view.getTitle().startsWith(ChatColor.BLUE + "FX Demo")) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        String stripped = ChatColor.stripColor(view.getTitle());
        int page = 0;
        int idx = stripped.indexOf("Page ");
        if (idx >= 0) {
            try { page = Integer.parseInt(stripped.substring(idx + 5).trim()) - 1; } catch (NumberFormatException ignored) {}
        }

        int slot = event.getRawSlot();
        if (slot == EffectDemoGUI.nextSlot()) {
            EffectDemoGUI.open(player, page + 1);
            return;
        } else if (slot == EffectDemoGUI.prevSlot()) {
            EffectDemoGUI.open(player, page - 1);
            return;
        }

        int indexInPage = EffectDemoGUI.getItemSlots().indexOf(slot);
        if (indexInPage != -1) {
            int effectIndex = page * EffectDemoGUI.itemsPerPage() + indexInPage;
            DemoEffects[] effects = DemoEffects.values();
            if (effectIndex >= 0 && effectIndex < effects.length) {
                player.closeInventory();
                effects[effectIndex].play(player);
            }
        }
    }
}
