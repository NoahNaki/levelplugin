package me.nakilex.levelplugin.server;

import me.nakilex.levelplugin.utils.ChatMessageUtil;
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

public class ServerSelectorGUI implements Listener {
    private static final String TITLE = "Server Selector";
    private static final int ALPHA_SLOT = 11;
    private static final int BUILD_SLOT = 15;

    private final ServerSelectionManager manager;

    public ServerSelectorGUI(ServerSelectionManager manager) {
        this.manager = manager;
    }

    public void open(Player player) {
        if (player == null) {
            return;
        }
        Inventory gui = GuiBuilder.create(27, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .fillEmptySlots(true)
                .setItem(ALPHA_SLOT, createAlphaItem(player))
                .setItem(BUILD_SLOT, createBuildItem(player))
                .build();
        player.openInventory(gui);
    }

    private ItemStack createAlphaItem(Player player) {
        ItemStack item = new ItemStack(Material.DIAMOND);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Alpha Test");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.addAll(TooltipUtil.bulletList(
                    "Play the full MMORPG experience.",
                    "Create or select a profile to begin."
            ));
            lore.add("");
            lore.addAll(TooltipUtil.clickInstructions("to connect", null));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBuildItem(Player player) {
        boolean allowed = manager.canAccessBuild(player);
        Material icon = allowed ? Material.BRICKS : Material.BARRIER;
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "Build Server");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.addAll(TooltipUtil.bulletList(
                    "Access the flatland build world.",
                    "LevelPlugin features are disabled here."
            ));
            lore.add("");
            if (allowed) {
                lore.addAll(TooltipUtil.clickInstructions("to connect", null));
            } else {
                lore.add(ChatColor.RED + "Staff-only access.");
                lore.add(ChatColor.GRAY + "Requires permission or weight "
                        + manager.getBuildMinWeight() + "+.");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String title = GuiUtil.normalizeTitle(event.getView().getTitle());
        if (!TITLE.equalsIgnoreCase(title)) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == ALPHA_SLOT) {
            player.closeInventory();
            manager.sendToAlpha(player);
            return;
        }
        if (slot == BUILD_SLOT) {
            if (!manager.canAccessBuild(player)) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                        "You do not have access to the build server.");
                return;
            }
            player.closeInventory();
            manager.sendToBuild(player);
        }
    }
}
