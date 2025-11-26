package me.nakilex.levelplugin.tower;

import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TextUtil;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple launcher menu for the infinite tower.
 */
public class TowerGUI implements Listener {

    private static final int SIZE = 27;
    private static final int ENTER_SLOT = 13;
    private static final int EXIT_SLOT = 15;
    private static final int SUMMARY_SLOT = 11;

    private final TowerManager towerManager;
    private final java.util.Map<UUID, Inventory> open = new ConcurrentHashMap<>();

    public TowerGUI(TowerManager towerManager) {
        this.towerManager = towerManager;
    }

    public void open(Player player) {
        Inventory inv = build(player);
        player.openInventory(inv);
        open.put(player.getUniqueId(), inv);
    }

    private Inventory build(Player player) {
        GuiBuilder builder = GuiBuilder.create(SIZE, TextUtil.centerInventoryTitle(ChatColor.RED + "Infinite Tower"))
                .filler(Material.BLACK_STAINED_GLASS_PANE)
                .border();

        builder.setItem(SUMMARY_SLOT, buildSummary(player));
        builder.setItem(ENTER_SLOT, buildEnter());
        builder.setItem(EXIT_SLOT, buildExit());
        return builder.build();
    }

    private ItemStack buildSummary(Player player) {
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta != null) {
            TowerStatus status = towerManager.getStatus(player.getUniqueId());
            int floor = status != null ? status.stage() : towerManager.getTrackedStage(player.getUniqueId());
            meta.setDisplayName(ChatColor.GOLD + "Floor " + floor);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Each 10th floor is a boss room.");
            lore.add(ChatColor.GRAY + "Time limit scales with your floor.");
            if (status != null) {
                lore.add(" ");
                lore.add(ChatColor.YELLOW + "In run:" + ChatColor.WHITE + " Floor " + status.stage());
                lore.add(ChatColor.GRAY + "Time left: " + ChatColor.WHITE + status.secondsRemaining() + "s");
                lore.add(ChatColor.GRAY + "Mobs remaining: " + ChatColor.WHITE + status.mobsRemaining());
            }
            lore.add(" ");
            lore.addAll(TooltipUtil.bulletList(
                    ChatColor.AQUA + "Soul Sigils" + ChatColor.GRAY + " awarded per clear.",
                    "Mythic mobs rotate from mob_rewards.yml.",
                    "Bosses pull from field_bosses.yml every 10 floors.",
                    "Leave anytime with /tower exit."
            ));
            meta.setLore(lore);
            book.setItemMeta(meta);
        }
        return book;
    }

    private ItemStack buildEnter() {
        ItemStack item = GuiUtil.getNexoItem("sword", ChatColor.GREEN + "Enter Current Floor");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Clear waves before the timer ends.");
            lore.addAll(TooltipUtil.clickInstructions("Enter the tower", null));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildExit() {
        ItemStack item = GuiUtil.getNexoItem("cross", ChatColor.RED + "Exit Tower");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setLore(TooltipUtil.clickInstructions("Leave your current run", null));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory openInv = open.get(player.getUniqueId());
        if (openInv == null || !event.getView().getTitle().contains("Infinite Tower")) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() == ENTER_SLOT) {
            towerManager.enter(player);
            player.closeInventory();
            return;
        }
        if (event.getRawSlot() == EXIT_SLOT) {
            towerManager.exit(player);
            player.closeInventory();
        }
    }
}
