package me.nakilex.levelplugin.codex;

import me.nakilex.levelplugin.utils.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

/** GUI showing discovered locations. */
public class LocationCodexGUI implements Listener {
    private static final String TITLE = ChatColor.DARK_GREEN + "Location Codex";
    private static final int SIZE = 54;

    private final CodexManager manager;
    private CodexMainGUI mainGUI;
    private final ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);

    public LocationCodexGUI(CodexManager manager, CodexMainGUI mainGUI) {
        this.manager = manager;
        this.mainGUI = mainGUI;
    }

    public void setMainGUI(CodexMainGUI mainGUI) { this.mainGUI = mainGUI; }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        GuiUtil.fillBorder(inv, filler);
        int slot = 10;
        for (String name : manager.getAllLocationKeys()) {
            inv.setItem(slot++, createIcon(player.getUniqueId(), name));
            if (slot % 9 == 8) slot += 2;
        }
        inv.setItem(49, GuiUtil.getNexoItem("arrow_left", ChatColor.YELLOW + "Back"));
        player.openInventory(inv);
    }

    private ItemStack createIcon(UUID id, String key) {
        boolean discovered = manager.hasDiscoveredLocation(id, key);
        ItemStack item = new ItemStack(Material.MAP);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(discovered ? ChatColor.GREEN + key : ChatColor.DARK_GRAY + "???");
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(TITLE)) return;
        e.setCancelled(true);
        ItemStack item = e.getCurrentItem();
        if (item != null && item.hasItemMeta()) {
            String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            if (name.equalsIgnoreCase("Back")) {
                mainGUI.open((Player) e.getWhoClicked());
            }
        }
    }
}
