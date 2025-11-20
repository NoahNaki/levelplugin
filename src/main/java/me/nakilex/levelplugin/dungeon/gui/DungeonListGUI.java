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
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;

import java.util.ArrayList;
import java.util.List;
import java.text.DecimalFormat;

/** GUI listing playable dungeons. */
public class DungeonListGUI implements Listener {
    private static final String TITLE = ChatColor.BLACK + "Dungeons";
    private static final int SIZE = 54;

    private final DungeonManager manager;

    public DungeonListGUI(DungeonManager manager) {
        this.manager = manager;
    }

    public void open(Player player) {
        Inventory inv = GuiBuilder.create(SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();
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
                String ratingLine = rating > 0
                        ? ChatColor.GRAY + "Rating " + ChatColor.WHITE + df.format(rating) + " " + ChatColor.GOLD + stars
                        : ChatColor.GRAY + "Rating " + ChatColor.WHITE + "N/A";
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.DARK_GRAY + "────────────");
                lore.add(ChatColor.GRAY + "Threat Level " + ChatColor.WHITE + threat);
                lore.add(ratingLine);
                lore.add(ChatColor.DARK_GRAY + "────────────");
                lore.add(ChatColor.GRAY + "Explore fan-favorite dungeons with");
                lore.add(ChatColor.GRAY + "consistent rewards and pacing.");
                lore.add("");
                lore.addAll(TooltipUtil.clickInstructions("to play", null));
                meta.setLore(lore);
                meta.setLocalizedName(key);
                item.setItemMeta(meta);
                me.nakilex.levelplugin.utils.TextUtil.centerItemTooltip(item, true, false);
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
