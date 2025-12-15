package me.nakilex.levelplugin.dungeon.gui;

import me.nakilex.levelplugin.dungeon.DungeonManager;
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

/** Confirmation GUI shown when a player attempts to leave a dungeon instance. */
public class DungeonLeaveGUI implements Listener {
    private static final String TITLE = ChatColor.BLACK + "Leave Dungeon?";
    private static final int SIZE = 27;
    private static final int CONFIRM_SLOT = 11;
    private static final int CANCEL_SLOT = 15;

    private final DungeonManager dungeonManager;

    public DungeonLeaveGUI(DungeonManager dungeonManager) {
        this.dungeonManager = dungeonManager;
    }

    public void open(Player player) {
        if (player == null) return;

        if (!dungeonManager.isInstanceWorld(player.getWorld())) {
            dungeonManager.getPlugin().getDungeonListGUI().open(player);
            return;
        }

        Inventory inv = GuiBuilder.create(SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();

        inv.setItem(CONFIRM_SLOT, createConfirmItem());
        inv.setItem(CANCEL_SLOT, createCancelItem());

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;

        int slot = event.getRawSlot();
        if (slot == CONFIRM_SLOT) {
            player.closeInventory();
            dungeonManager.handleInstanceExit(player.getWorld(), player, true);
        } else if (slot == CANCEL_SLOT) {
            player.closeInventory();
        }
    }

    private ItemStack createConfirmItem() {
        ItemStack item = GuiUtil.getNexoItem("check", ChatColor.GREEN + "Confirm Exit");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Return to your last location.");
            lore.addAll(TooltipUtil.clickInstructions("to leave the dungeon", null));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCancelItem() {
        ItemStack item = GuiUtil.getNexoItem("cross", ChatColor.RED + "Stay Here");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Keep exploring and collect your loot.");
            lore.addAll(TooltipUtil.clickInstructions("to close", null));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
