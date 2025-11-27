package me.nakilex.levelplugin.catacombs;

import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple entry GUI for the Catacombs flow to keep UX consistent
 * with existing expedition menus.
 */
public class CatacombsGUI implements Listener {
    private static final int SIZE = 27;
    private static final String TITLE = ChatColor.BLACK + "Catacombs";

    private final CatacombsManager manager;

    public CatacombsGUI(CatacombsManager manager) {
        this.manager = manager;
    }

    public void open(Player player) {
        Inventory inv = GuiBuilder.create(SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(true)
                .build();

        inv.setItem(13, entryItem(player));
        player.openInventory(inv);
    }

    private ItemStack entryItem(Player player) {
        ItemStack item = GuiUtil.getNexoItem("portal", ChatColor.GOLD + "Enter the Catacombs");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Fight through an endless chain of rooms.");
            lore.addAll(TooltipUtil.bulletList(
                    "Scale up difficulty as you clear stages",
                    "Timed encounters reward fast clears",
                    "Return to the last waiting room if you fail"
            ));
            lore.add(" ");
            lore.add(ChatColor.GRAY + "Status: " + (manager.isInCatacombs(player.getUniqueId())
                    ? ChatColor.GREEN + "In progress"
                    : ChatColor.YELLOW + "Ready"));
            lore.addAll(TooltipUtil.clickInstructions("to enter the Catacombs", null));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().getTitle() == null || !event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);
        if (event.getRawSlot() == 13) {
            player.closeInventory();
            manager.startRun(player);
        }
    }
}
