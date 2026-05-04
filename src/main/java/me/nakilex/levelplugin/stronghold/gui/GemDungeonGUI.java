package me.nakilex.levelplugin.stronghold.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.stronghold.run.GemDungeonManager;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TextUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GemDungeonGUI implements Listener {
    private static final String TITLE = TextUtil.centerInventoryTitle("Gem Dungeon");
    private final GemDungeonManager manager;

    public GemDungeonGUI(GemDungeonManager manager) {
        this.manager = manager;
    }

    public void open(Player player) {
        Inventory inv = org.bukkit.Bukkit.createInventory(null, 27, TITLE);
        ItemStack pane = GuiUtil.createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);
        inv.setItem(13, buildEntry());
        player.openInventory(inv);
    }

    private ItemStack buildEntry() {
        ItemStack item = GuiUtil.getNexoItem("purple_orb_icon", ChatColor.LIGHT_PURPLE + "Gem Dungeon");
        if (item.getType() == Material.BARRIER) item = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "DPS check challenge against a training dummy.");
            lore.add(ChatColor.GRAY + "20s timer. Reward scales by stage HP.");
            lore.add("");
            lore.add(ChatColor.GRAY + "Stage HP: " + ChatColor.WHITE + "1000 x stage");
            lore.add(ChatColor.GRAY + "Reward: " + ChatColor.WHITE + "10% HP as gems");
            lore.add("");
            lore.addAll(TooltipUtil.clickInstructions("to challenge next stage", "to sweep highest cleared stage"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!GuiUtil.titleMatches(event.getView().getTitle(), TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getRawSlot() != 13) return;
        if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) manager.sweep(player);
        else manager.challenge(player);
        player.closeInventory();
    }
}
