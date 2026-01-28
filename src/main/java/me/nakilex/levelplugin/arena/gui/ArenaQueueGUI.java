package me.nakilex.levelplugin.arena.gui;

import me.nakilex.levelplugin.arena.ArenaMode;
import me.nakilex.levelplugin.arena.ArenaQueueManager;
import me.nakilex.levelplugin.arena.ArenaUnlockUtil;
import me.nakilex.levelplugin.arena.rating.ArenaRatingManager;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TextUtil;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
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
    private static final String TITLE = TextUtil.centerInventoryTitle("Arena Queue");
    private static final int TWO_VS_TWO_SLOT = 12;
    private static final int ONE_VS_ONE_SLOT = 14;

    private final ArenaQueueManager queueManager;
    private final ArenaRatingManager ratingManager;
    private final Map<UUID, Inventory> openInventories = new HashMap<>();
    private final List<GuiWidget> widgets;

    public ArenaQueueGUI(ArenaQueueManager queueManager, ArenaRatingManager ratingManager) {
        this.queueManager = queueManager;
        this.ratingManager = ratingManager;
        this.widgets = buildWidgets();
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
        renderWidgets(inv, player);
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

    private ItemStack createQueueButton(Player viewer, ArenaMode mode) {
        UUID viewerId = viewer != null ? viewer.getUniqueId() : null;
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

            if (viewer != null) {
                ArenaRatingManager.RatingSnapshot snapshot = ratingManager.getSnapshot(viewer.getUniqueId(), mode.ratingCategory());
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
        if (!GuiUtil.titleMatches(event.getView().getTitle(), TITLE)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (handleWidgetClick(event, player)) {
            return;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!GuiUtil.titleMatches(event.getView().getTitle(), TITLE)) return;
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
            Player player = null;
            for (HumanEntity viewer : inv.getViewers()) {
                if (viewer.getUniqueId().equals(viewerId) && viewer instanceof Player match) {
                    player = match;
                    stillOpen = true;
                    break;
                }
            }
            if (!stillOpen) {
                iterator.remove();
                continue;
            }
            if (player != null) {
                renderWidgets(inv, player);
            }
        }
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> widgetList = new ArrayList<>();
        widgetList.add(new ActionWidget(ONE_VS_ONE_SLOT,
                context -> createQueueButton(context.player(), ArenaMode.ONE_VS_ONE),
                (click, context) -> handleQueueClick(context.player(), ArenaMode.ONE_VS_ONE)));
        widgetList.add(new ActionWidget(TWO_VS_TWO_SLOT,
                context -> createQueueButton(context.player(), ArenaMode.TWO_VS_TWO),
                (click, context) -> handleQueueClick(context.player(), ArenaMode.TWO_VS_TWO)));
        return widgetList;
    }

    private void renderWidgets(Inventory inventory, Player player) {
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    private boolean handleWidgetClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return false;
        }
        GuiWidget widget = widgets.stream()
                .filter(w -> w.handlesSlot(slot))
                .findFirst()
                .orElse(null);
        if (widget == null) {
            return false;
        }
        event.setCancelled(true);
        widget.onClick(slot, event.getClick(), new GuiContext(player, event.getView().getTopInventory()));
        return true;
    }

    private void handleQueueClick(Player player, ArenaMode mode) {
        if (ArenaUnlockUtil.warnIfLocked(player)) {
            return;
        }

        UUID id = player.getUniqueId();
        Optional<ArenaMode> current = queueManager.getMode(id);
        if (current.isPresent() && !current.get().equals(mode)) {
            send(player, MessageType.ERROR, "Leave your current arena queue before joining another.");
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
}
