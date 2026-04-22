package me.nakilex.levelplugin.stronghold.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.stronghold.StrongholdQueueManager;
import me.nakilex.levelplugin.stronghold.StrongholdQueueMode;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TextUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class StrongholdQueueGUI implements Listener {
    private static final String TITLE = TextUtil.centerInventoryTitle("Stronghold Queue");
    private final Main plugin;
    private final StrongholdQueueManager queueManager;
    private final Map<UUID, Inventory> openInventories = new HashMap<>();

    public StrongholdQueueGUI(Main plugin, StrongholdQueueManager queueManager) {
        this.plugin = plugin;
        this.queueManager = queueManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        Inventory inv = plugin.getServer().createInventory(player, 27, TITLE);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }
        inv.setItem(11, createModeButton(player, StrongholdQueueMode.SOLO, Material.IRON_SWORD));
        inv.setItem(13, createModeButton(player, StrongholdQueueMode.DUO, Material.DIAMOND_SWORD));
        inv.setItem(15, createModeButton(player, StrongholdQueueMode.SQUAD, Material.NETHERITE_SWORD));
        player.openInventory(inv);
        openInventories.put(player.getUniqueId(), inv);
    }

    public void refresh() {
        for (UUID uuid : new ArrayList<>(openInventories.keySet())) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                openInventories.remove(uuid);
                continue;
            }
            Inventory inv = openInventories.get(uuid);
            if (inv == null || !GuiUtil.titleMatches(player.getOpenInventory().getTitle(), TITLE)) {
                openInventories.remove(uuid);
                continue;
            }
            inv.setItem(11, createModeButton(player, StrongholdQueueMode.SOLO, Material.IRON_SWORD));
            inv.setItem(13, createModeButton(player, StrongholdQueueMode.DUO, Material.DIAMOND_SWORD));
            inv.setItem(15, createModeButton(player, StrongholdQueueMode.SQUAD, Material.NETHERITE_SWORD));
        }
    }

    private ItemStack createModeButton(Player viewer, StrongholdQueueMode mode, Material fallback) {
        boolean queuedThis = queueManager.getMode(viewer.getUniqueId()).map(mode::equals).orElse(false);
        boolean queuedOther = queueManager.isQueued(viewer.getUniqueId()) && !queuedThis;
        ItemStack item = GuiUtil.getNexoItem("swords_icon",
                (queuedThis ? ChatColor.RED + "Leave " : ChatColor.GREEN + "Queue ") + mode.displayName());
        if (item.getType() == Material.BARRIER) {
            item = new ItemStack(fallback);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Party size: " + ChatColor.GOLD + mode.teamSize());
            lore.add(ChatColor.GRAY + "Queued: " + ChatColor.GREEN + queueManager.getQueuePopulation(mode) + ChatColor.GRAY + " players");
            lore.add(" ");
            if (queuedOther) {
                lore.add(ChatColor.RED + "Leave your current queue first.");
            } else if (queuedThis) {
                lore.addAll(TooltipUtil.clickInstructions("to leave this queue", null));
            } else {
                lore.addAll(TooltipUtil.clickInstructions("to queue for this mode", null));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!GuiUtil.titleMatches(event.getView().getTitle(), TITLE)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        event.setCancelled(true);
        StrongholdQueueMode mode = switch (event.getRawSlot()) {
            case 11 -> StrongholdQueueMode.SOLO;
            case 13 -> StrongholdQueueMode.DUO;
            case 15 -> StrongholdQueueMode.SQUAD;
            default -> null;
        };
        if (mode == null) {
            return;
        }
        if (queueManager.getMode(player.getUniqueId()).map(mode::equals).orElse(false)) {
            queueManager.leave(player.getUniqueId());
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, "Left stronghold queue.");
        } else {
            StrongholdQueueManager.QueueJoinResult result = queueManager.join(player, mode);
            if (result != StrongholdQueueManager.QueueJoinResult.JOINED) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                        StrongholdQueueManager.describeJoinFailure(result));
            }
        }
        refresh();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (GuiUtil.titleMatches(event.getView().getTitle(), TITLE)) {
            openInventories.remove(event.getPlayer().getUniqueId());
        }
    }
}
