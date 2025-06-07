package me.nakilex.levelplugin.fasttravel.listeners;

import me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class NexoItemListener implements Listener {
    private final FastTravelGUI gui;

    public NexoItemListener(FastTravelGUI gui) {
        this.gui = gui;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        ItemStack item = event.getItem();
        if (item == null) return;
        if (isNexoItem(item, "base_beacon_blue_inventory")) {
            event.setCancelled(true);
            gui.open(event.getPlayer());
        }
    }

    private boolean isNexoItem(ItemStack item, String id) {
        try {
            Class<?> api = Class.forName("com.tanguygab.nexo.api.NexoAPI");
            java.lang.reflect.Method m = api.getMethod("getItemId", ItemStack.class);
            String itemId = (String) m.invoke(null, item);
            if (itemId != null && itemId.equalsIgnoreCase(id)) return true;
        } catch (Exception ignored) {
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 40001;
    }
}
