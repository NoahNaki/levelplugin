package me.nakilex.levelplugin.arena.gui;

import me.nakilex.levelplugin.arena.ArenaMode;
import me.nakilex.levelplugin.arena.ArenaQueueManager;
import me.nakilex.levelplugin.arena.rating.ArenaRatingManager;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TextUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

/**
 * Lightweight queue menu mirroring the modern GUI style used across the
 * plugin. Players can join/leave the arena queue and see the live count
 * in the tooltip formatted as "&a&lN &7players in queue!" as requested.
 */
public class ArenaQueueGUI implements Listener {
    private static final int GUI_SIZE = 27;
    private static final String TITLE = TextUtil.centerInventoryTitle(ChatColor.BLACK + "Arena Queue");
    private static final int TWO_VS_TWO_SLOT = 12;
    private static final int ONE_VS_ONE_SLOT = 14;

    private final ArenaQueueManager queueManager;
    private final ArenaRatingManager ratingManager;
    private final Map<UUID, Inventory> openInventories = new HashMap<>();

    public ArenaQueueGUI(ArenaQueueManager queueManager, ArenaRatingManager ratingManager) {
        this.queueManager = queueManager;
        this.ratingManager = ratingManager;
    }

    /**
     * Open the arena queue menu for the player.
     */
    public void open(Player player) {
        Inventory inv = GuiBuilder.create(GUI_SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();
        inv.setItem(ONE_VS_ONE_SLOT, createQueueButton(player.getUniqueId(), ArenaMode.ONE_VS_ONE));
        inv.setItem(TWO_VS_TWO_SLOT, createQueueButton(player.getUniqueId(), ArenaMode.TWO_VS_TWO));
        openInventories.put(player.getUniqueId(), inv);
        player.openInventory(inv);
    }

    /**
     * Update all currently open inventories. Invoked after queue actions or
     * by external callers (commands) to refresh the live count.
     */
    public void refresh() {
        refreshOpenInventories();
    }

    private ItemStack createQueueButton(UUID viewerId, ArenaMode mode) {
        boolean queued = viewerId != null && queueManager.getMode(viewerId)
                .map(mode::equals)
                .orElse(false);
        boolean inOtherQueue = viewerId != null && queueManager.isQueued(viewerId) && !queued;
        String action = queued ? "Leave" : "Join";
        String name = (queued ? ChatColor.RED : ChatColor.GREEN) + "" + ChatColor.BOLD
                + action + " " + mode.displayName() + ChatColor.RESET;

        String icon = mode == ArenaMode.TWO_VS_TWO ? "group_swords_icon" : "swords_icon";
        ItemStack item = GuiUtil.getNexoItem(icon, name);
        if (item.getType() == Material.BARRIER) {
            item = new ItemStack(mode == ArenaMode.TWO_VS_TWO ? Material.DIAMOND_SWORD : Material.IRON_SWORD);
            ItemMeta fallback = item.getItemMeta();
            if (fallback != null) {
                fallback.setDisplayName(name);
                item.setItemMeta(fallback);
            }
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add("");
            if (mode == ArenaMode.ONE_VS_ONE) {
                lore.add(ChatColor.GRAY + "Queue up to battle challengers.");
            } else {
                lore.add(ChatColor.GRAY + "Queue with your party of two.");
            }

            if (viewerId != null) {
                ArenaRatingManager.RatingSnapshot snapshot = ratingManager.getSnapshot(viewerId, mode.ratingCategory());
                int rating = snapshot.rating();
                int window = snapshot.matchWindow(Duration.ZERO);
                int stability = (int) Math.round(snapshot.deviation());
                lore.add(" ");
                lore.add(ChatColor.GOLD + "Your ELO: " + ChatColor.WHITE + rating + ChatColor.GRAY + " ("
                        + ratingManager.formatTier(rating) + ChatColor.GRAY + ")");
                lore.add(ChatColor.DARK_GRAY + "Confidence: ±" + stability);
                lore.add(ChatColor.DARK_GRAY + "Search window: ±" + window);
                lore.add(ChatColor.DARK_GRAY + "Ranked matches: " + snapshot.matches());
            }

            lore.add(" ");
            lore.add(formatQueueStatusLine(mode));
            lore.add(" ");
            if (inOtherQueue) {
                lore.add(ChatColor.RED + "Leave your current queue first.");
            } else if (queued) {
                lore.addAll(TooltipUtil.clickInstructions("to leave the queue", null));
            } else {
                lore.addAll(TooltipUtil.clickInstructions("to join the queue", null));
            }
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatQueueStatusLine(ArenaMode mode) {
        int size = queueManager.getQueuePopulation(mode);
        return ChatColor.GREEN + "" + ChatColor.BOLD + size + ChatColor.GRAY + " players in queue!";
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();
        ArenaMode mode;
        if (slot == ONE_VS_ONE_SLOT) {
            mode = ArenaMode.ONE_VS_ONE;
        } else if (slot == TWO_VS_TWO_SLOT) {
            mode = ArenaMode.TWO_VS_TWO;
        } else {
            return;
        }

        UUID id = player.getUniqueId();
        Optional<ArenaMode> current = queueManager.getMode(id);
        if (current.isPresent() && !current.get().equals(mode)) {
            send(player, MessageType.ERROR, "Leave your current arena queue before joining another." );
            refreshOpenInventories();
            return;
        }

        if (current.isPresent()) {
            queueManager.leave(id);
            send(player, MessageType.INFO, "You left the " + current.get().displayName() + ChatColor.GRAY + " queue.");
        } else {
            ArenaQueueManager.QueueJoinOutcome outcome = queueManager.join(player, mode);
            if (outcome.result() == ArenaQueueManager.QueueJoinResult.JOINED) {
                send(player, MessageType.SUCCESS, "You joined the " + mode.displayName() + ChatColor.GRAY + " queue.");
            } else {
                String message = outcome.message();
                if (message == null) {
                    switch (outcome.result()) {
                        case ALREADY_QUEUED -> message = ChatColor.RED + "You are already in this queue.";
                        case IN_MATCH -> message = ChatColor.RED + "You cannot queue while an arena match is active.";
                        case RANK_GAP_TOO_LARGE -> message = ChatColor.RED + "Your party's arena tiers are too far apart.";
                        default -> message = ChatColor.RED + "Unable to join the queue.";
                    }
                }
                send(player, MessageType.ERROR, message);
            }
        }
        refreshOpenInventories();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        openInventories.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (queueManager.leave(id, ArenaQueueManager.LeaveReason.DISCONNECT)) {
            refreshOpenInventories();
        }
        openInventories.remove(id);
    }

    private void refreshOpenInventories() {
        Iterator<Map.Entry<UUID, Inventory>> iterator = openInventories.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Inventory> entry = iterator.next();
            UUID viewerId = entry.getKey();
            Inventory inv = entry.getValue();
            boolean stillOpen = false;
            for (HumanEntity viewer : inv.getViewers()) {
                if (viewer.getUniqueId().equals(viewerId)) {
                    stillOpen = true;
                    break;
                }
            }
            if (!stillOpen) {
                iterator.remove();
                continue;
            }
            inv.setItem(ONE_VS_ONE_SLOT, createQueueButton(viewerId, ArenaMode.ONE_VS_ONE));
            inv.setItem(TWO_VS_TWO_SLOT, createQueueButton(viewerId, ArenaMode.TWO_VS_TWO));
        }
    }
}
