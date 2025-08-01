package me.nakilex.levelplugin.dungeon.gui;

import me.nakilex.levelplugin.dungeon.DungeonManager;
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
import me.nakilex.levelplugin.utils.GuiUtil;

import java.util.List;
import java.text.DecimalFormat;

/** GUI listing playable dungeons. */
public class DungeonListGUI implements Listener {
    private static final String TITLE = ChatColor.BLACK + "Dungeons";
    private static final int SIZE = 54;

    private final DungeonManager manager;
    private final ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);

    public DungeonListGUI(DungeonManager manager) {
        this.manager = manager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        for (int i = 0; i < SIZE; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) inv.setItem(i, filler);
        }
        int slot = 10;
        DecimalFormat df = new DecimalFormat("#.#");
        for (var entry : manager.getLayoutEntries()) {
            if (slot >= 44) break;
            String key = entry.getKey();
            String display = entry.getValue();
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.AQUA + display);
                int threat = manager.getThreatLevel(key);
                double rating = me.nakilex.levelplugin.Main.getInstance().getDungeonRatingManager().getAverage(key);
                String stars = GuiUtil.glyphStars((int) Math.floor(rating));
                String ratingLine = rating > 0 ? ChatColor.GOLD + "Rating: " + df.format(rating) + " " + stars : ChatColor.GOLD + "Rating: N/A";
                meta.setLore(List.of(ChatColor.GRAY + "Left-click to play",
                        ChatColor.DARK_RED + "Threat Level: " + threat,
                        ratingLine));
                meta.setLocalizedName(key);
                item.setItemMeta(meta);
            }
            inv.setItem(slot, item);
            slot++;
            if (slot % 9 == 8) slot += 2;
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        if (!event.getClick().isLeftClick()) return;
        ItemMeta meta = item.getItemMeta();
        String key = meta != null && meta.getLocalizedName() != null ? meta.getLocalizedName() : ChatColor.stripColor(meta.getDisplayName());
        player.closeInventory();
        manager.startInstance(player, key);
    }
}
