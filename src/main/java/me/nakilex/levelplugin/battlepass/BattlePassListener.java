package me.nakilex.levelplugin.battlepass;

import me.nakilex.levelplugin.battlepass.gui.BattlePassGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Handles interactions within the battle pass inventory.
 */
public class BattlePassListener implements Listener {

    private final BattlePassManager manager;

    public BattlePassListener(BattlePassManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!BattlePassGUI.TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String action = pdc.get(BattlePassGUI.ACTION_KEY, PersistentDataType.STRING);
        if (action == null) return;

        switch (action) {
            case "prev" -> BattlePassGUI.changePage(player, manager, -1);
            case "next" -> BattlePassGUI.changePage(player, manager, 1);
            case "unlock" -> {
                if (manager.unlockPremium(player)) {
                    BattlePassGUI.open(player, manager);
                }
            }
            case "reward" -> {
                Integer tier = pdc.get(BattlePassGUI.TIER_KEY, PersistentDataType.INTEGER);
                String track = pdc.get(BattlePassGUI.TRACK_KEY, PersistentDataType.STRING);
                boolean premium = "PREMIUM".equalsIgnoreCase(track);
                if (tier != null && manager.claim(player, tier, premium)) {
                    BattlePassGUI.open(player, manager);
                }
            }
            default -> {}
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (BattlePassGUI.TITLE.equals(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }
}
