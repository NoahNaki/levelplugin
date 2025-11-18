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
        int used = manager.getDailyRewardsUsed(player.getUniqueId());
        int limit = manager.getDailyRewardLimit();
        int remaining = Math.max(0, limit - used);
        ItemStack status = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta statusMeta = status.getItemMeta();
        if (statusMeta != null) {
            statusMeta.setDisplayName(ChatColor.GOLD + "Daily Dungeon Rewards");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Claim rewards from up to " + ChatColor.YELLOW + limit
                    + ChatColor.GRAY + " dungeons each day.");
            lore.add(ChatColor.GRAY + "Used: " + ChatColor.YELLOW + used + ChatColor.GRAY + " / " + ChatColor.YELLOW + limit);
            lore.add(ChatColor.GRAY + "Remaining: "
                    + (remaining > 0 ? ChatColor.GREEN : ChatColor.RED) + remaining);
            lore.add(" ");
            lore.addAll(TooltipUtil.bulletList(
                    "Rewards scale with performance and party size.",
                    "Ratings grant a small XP bonus."));
            statusMeta.setLore(lore);
            status.setItemMeta(statusMeta);
        }
        inv.setItem(4, status);
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
                List<String> lore = new ArrayList<>();
                lore.addAll(TooltipUtil.clickInstructions("to play", null));
                lore.add(ChatColor.DARK_RED + "Threat Level: " + threat);
                lore.add(ChatColor.GRAY + "Attempts left today: "
                        + (remaining > 0 ? ChatColor.GREEN : ChatColor.RED)
                        + remaining + ChatColor.GRAY + "/" + limit);
                lore.add(ratingLine);
                meta.setLore(lore);
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
