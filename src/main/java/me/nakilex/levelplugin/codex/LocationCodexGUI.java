package me.nakilex.levelplugin.codex;

import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.codex.CodexGuiUtil;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** GUI listing discovered locations. */
public class LocationCodexGUI implements Listener {
    private static final String TITLE = ChatColor.BLACK + "Codex - Locations";

    private final CodexManager manager;
    private final ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
    private CodexMainGUI mainGui;

    public LocationCodexGUI(CodexManager manager, CodexMainGUI mainGui) {
        this.manager = manager;
        this.mainGui = mainGui;
    }

    public void setMainGui(CodexMainGUI gui) { this.mainGui = gui; }

    public void open(Player player) {
        List<String> list = new ArrayList<>(manager.getDiscoveredLocations(player.getUniqueId()));
        int size = ((list.size() - 1) / 9 + 1) * 9;
        Inventory inv = Bukkit.createInventory(null, Math.max(size, 27), TITLE);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        Map<String, String> lines = new LinkedHashMap<>();
        lines.put("Locations", String.valueOf(list.size()));
        inv.setItem(4, CodexGuiUtil.createInfoBook("Discoveries", lines,
                ChatColor.GRAY + "Category: Locations",
                ChatColor.WHITE + "Each location that you've discovered will be listed here."));

        int slot = 0;
        for (String name : list) {
            if (slot == 4) slot++; // skip info book slot
            ItemStack item = new ItemStack(Material.MAP);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + name);
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
        }
        inv.setItem(inv.getSize() - 1, GuiUtil.getNexoItem("arrow_left", ChatColor.YELLOW + "Back"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(TITLE)) return;
        e.setCancelled(true);
        if (e.getRawSlot() == e.getInventory().getSize() - 1 && e.getWhoClicked() instanceof Player p) {
            mainGui.open(p);
        }
    }
}
