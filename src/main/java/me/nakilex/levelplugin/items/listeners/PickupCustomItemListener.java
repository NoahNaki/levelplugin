package me.nakilex.levelplugin.items.listeners;

import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class PickupCustomItemListener implements Listener {
    private final JavaPlugin plugin;

    public PickupCustomItemListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        ItemStack picked = event.getItem().getItemStack();

        // only care about your CustomItems
        if (picked.hasItemMeta()
            && picked.getItemMeta().getPersistentDataContainer()
            .has(ItemUtil.ITEM_UUID_KEY, PersistentDataType.STRING)) {

            // wait 1 tick so the item is actually in their inventory
            new BukkitRunnable() {
                @Override
                public void run() {
                    // find any matching stacks and refresh their lore
                    for (ItemStack s : player.getInventory().getContents()) {
                        if (s != null && s.isSimilar(picked)) {
                            ItemUtil.updateCustomItemTooltip(s, player);
                        }
                    }
                    player.updateInventory();
                }
            }.runTaskLater(plugin, 1L);
        }
    }
}
