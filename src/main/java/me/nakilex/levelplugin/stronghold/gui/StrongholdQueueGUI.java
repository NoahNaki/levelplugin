package me.nakilex.levelplugin.stronghold.gui;

import me.nakilex.levelplugin.stronghold.StrongholdQueueManager;
import me.nakilex.levelplugin.stronghold.StrongholdQueueMode;
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

public class StrongholdQueueGUI implements Listener {
    private static final String TITLE = "Stronghold Queue";
    private final StrongholdQueueManager queueManager;

    public StrongholdQueueGUI(StrongholdQueueManager queueManager) {
        this.queueManager = queueManager;
    }

    public void open(Player player) {
        Inventory inv = GuiBuilder.create(27, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(true)
                .build();
        inv.setItem(11, modeItem(StrongholdQueueMode.SOLO));
        inv.setItem(13, modeItem(StrongholdQueueMode.DUO));
        inv.setItem(15, modeItem(StrongholdQueueMode.SQUAD));
        inv.setItem(22, leaveItem());
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!GuiUtil.titleMatches(event.getView().getTitle(), TITLE)) return;
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        event.setCancelled(true);

        StrongholdQueueMode mode = switch (event.getRawSlot()) {
            case 11 -> StrongholdQueueMode.SOLO;
            case 13 -> StrongholdQueueMode.DUO;
            case 15 -> StrongholdQueueMode.SQUAD;
            default -> null;
        };
        if (mode != null) {
            player.closeInventory();
            String error = queueManager.join(player, mode);
            if (error != null) {
                me.nakilex.levelplugin.utils.ChatMessageUtil.send(player, me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType.ERROR, error);
            }
            return;
        }
        if (event.getRawSlot() == 22) {
            player.closeInventory();
            if (!queueManager.leave(player.getUniqueId())) {
                me.nakilex.levelplugin.utils.ChatMessageUtil.send(player, me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType.WARNING,
                        "You are not in the Stronghold queue.");
            }
        }
    }

    private ItemStack modeItem(StrongholdQueueMode mode) {
        Material mat = switch (mode) {
            case SOLO -> Material.IRON_SWORD;
            case DUO -> Material.SHIELD;
            case SQUAD -> Material.NETHERITE_CHESTPLATE;
        };
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + mode.displayName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Team size: " + ChatColor.WHITE + mode.teamSize());
            lore.add(ChatColor.GRAY + "Queued players: " + ChatColor.WHITE + queueManager.queuePopulation(mode));
            lore.addAll(TooltipUtil.clickInstructions("to join " + mode.displayName() + " queue", null));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack leaveItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Leave Queue");
            meta.setLore(TooltipUtil.clickInstructions("to leave the Stronghold queue", null));
            item.setItemMeta(meta);
        }
        return item;
    }
}
