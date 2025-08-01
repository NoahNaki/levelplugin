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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** GUI listing discovered NPCs. */
public class NpcCodexGUI implements Listener {
    private static final String TITLE = ChatColor.BLACK + "Codex - NPCs";

    private final CodexManager manager;
    private final ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
    private CodexMainGUI mainGui;

    public NpcCodexGUI(CodexManager manager, CodexMainGUI mainGui) {
        this.manager = manager;
        this.mainGui = mainGui;
    }

    public void setMainGui(CodexMainGUI gui) { this.mainGui = gui; }

    public void open(Player player) {
        List<String> list = new ArrayList<>(manager.getDiscoveredNpcs(player.getUniqueId()));
        int size = ((list.size() - 1) / 9 + 1) * 9;
        Inventory inv = Bukkit.createInventory(null, Math.max(size, 27), TITLE);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
        int slot = 0;
        for (String name : list) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = head.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + name);
                head.setItemMeta(meta);
            }
            inv.setItem(slot++, head);
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
