package me.nakilex.levelplugin.items.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.settings.data.PlayerSettings;
import me.nakilex.levelplugin.settings.managers.SettingsManager;
import me.nakilex.levelplugin.items.data.ItemRarity;
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

        SettingsManager settingsManager = Main.getInstance().getSettingsManager();
        if (settingsManager != null) {
            PlayerSettings settings = settingsManager.getSettings(player);
            ItemRarity rarity = ItemUtil.getCustomItemRarity(picked);
            if (rarity != null && ItemUtil.isWeaponOrArmor(picked)
                    && !settings.isLootPickupAllowed(rarity)) {
                event.setCancelled(true);
                return;
            }
        }

        // only care about your CustomItems or custom mining tools
        boolean isCustomItem = picked.hasItemMeta()
            && picked.getItemMeta().getPersistentDataContainer()
                .has(ItemUtil.ITEM_UUID_KEY, PersistentDataType.STRING);
        boolean isCustomTool = me.nakilex.levelplugin.items.tools.ToolManager.getInstance().isToolMaterial(picked.getType());

        if (isCustomItem || isCustomTool) {

            // wait 1 tick so the item is actually in their inventory
            new BukkitRunnable() {
                @Override
                public void run() {
                    // find any matching stacks and refresh their lore
                    for (ItemStack s : player.getInventory().getContents()) {
                        if (s != null && s.isSimilar(picked)) {
                            ItemUtil.updateTooltip(s, player);
                        }
                    }
                    player.updateInventory();
                }
            }.runTaskLater(plugin, 1L);
        }
    }
}
