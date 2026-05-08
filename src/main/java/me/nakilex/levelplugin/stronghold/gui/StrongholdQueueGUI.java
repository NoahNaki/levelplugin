package me.nakilex.levelplugin.stronghold.gui;

import me.nakilex.levelplugin.stronghold.StrongholdGearBand;
import me.nakilex.levelplugin.stronghold.StrongholdQueueManager;
import me.nakilex.levelplugin.stronghold.StrongholdQueueMode;
import me.nakilex.levelplugin.stronghold.StrongholdStartupProfiler;
import me.nakilex.levelplugin.stronghold.run.StrongholdHeat;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TextUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

public class StrongholdQueueGUI implements Listener {
    private static final int GUI_SIZE = 27;
    private static final String TITLE = TextUtil.centerInventoryTitle("Stronghold Queue");

    private final StrongholdQueueManager queueManager;
    private final Map<UUID, Inventory> openInventories = new HashMap<>();
    private final List<GuiWidget> widgets;

    public StrongholdQueueGUI(StrongholdQueueManager queueManager) {
        this.queueManager = queueManager;
        this.widgets = buildWidgets();
    }

    public void open(Player player) {
        StrongholdStartupProfiler profiler = StrongholdStartupProfiler.startOrContinue(Main.getInstance(), player);
        if (profiler != null) {
            long step = profiler.stepStarted("Open stronghold queue GUI");
            profiler.stepFinished("Open stronghold queue GUI", step);
        }
        Inventory inv = GuiBuilder.create(GUI_SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();
        renderWidgets(inv, player);
        openInventories.put(player.getUniqueId(), inv);
        player.openInventory(inv);
    }

    public void refresh() {
        refreshOpenInventories();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!GuiUtil.titleMatches(event.getView().getTitle(), TITLE)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getRawSlot() == 11 && (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT)) {
            event.setCancelled(true);
            new StrongholdStageSelectGUI().open(player);
            return;
        }
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
        if (queueManager.leave(id, StrongholdQueueManager.LeaveReason.DISCONNECT)) {
            refreshOpenInventories();
        }
        openInventories.remove(id);
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> list = new ArrayList<>();
        list.add(widget(11, StrongholdQueueMode.SOLO, "solo_swords_icon", Material.IRON_SWORD));
        list.add(widget(13, StrongholdQueueMode.DUO, "group_swords_icon", Material.DIAMOND_SWORD));
        list.add(widget(15, StrongholdQueueMode.SQUAD, "party_banner_icon", Material.NETHERITE_AXE));
        list.add(new ActionWidget(22, ctx -> createHeatButton(ctx.player()), (click, ctx) -> handleHeatClick(ctx.player())));
        return list;
    }

    private GuiWidget widget(int slot, StrongholdQueueMode mode, String nexoId, Material fallback) {
        return new ActionWidget(slot,
                context -> createQueueButton(context.player(), mode, nexoId, fallback),
                (click, context) -> handleQueueClick(context.player(), mode));
    }

    private ItemStack createHeatButton(Player viewer) {
        var runManager = Main.getInstance() == null ? null : Main.getInstance().getStrongholdRunManager();
        StrongholdHeat heat = runManager == null ? StrongholdHeat.NONE : runManager.getQueuedHeat(viewer);
        ItemStack item = new ItemStack(heat == StrongholdHeat.NONE ? Material.CAMPFIRE : Material.SOUL_CAMPFIRE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(heat.color() + "" + ChatColor.BOLD + "Stronghold Heat: " + heat.displayName());
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Optional risk/reward modifier for your next run.");
            lore.add("");
            for (String line : heat.description()) {
                lore.add(TooltipUtil.bulletLine(ChatColor.GRAY + line));
            }
            lore.addAll(TooltipUtil.rewardList(heat == StrongholdHeat.NONE ? "Standard rewards" : "Higher score potential"));
            lore.add("");
            lore.addAll(TooltipUtil.clickInstructions("to cycle heat", null));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void handleHeatClick(Player player) {
        var runManager = Main.getInstance() == null ? null : Main.getInstance().getStrongholdRunManager();
        if (runManager == null) {
            send(player, MessageType.ERROR, "Stronghold run manager unavailable.");
            return;
        }
        StrongholdHeat heat = runManager.cycleQueuedHeat(player);
        send(player, heat == StrongholdHeat.NONE ? MessageType.INFO : MessageType.WARNING,
                "Stronghold heat set to " + heat.coloredName() + ChatColor.GRAY + ".");
        refreshOpenInventories();
    }

    private ItemStack createQueueButton(Player viewer, StrongholdQueueMode mode, String icon, Material fallback) {
        UUID viewerId = viewer.getUniqueId();
        Optional<StrongholdQueueMode> current = queueManager.getMode(viewerId);
        boolean queuedThis = current.map(mode::equals).orElse(false);
        boolean queuedOther = current.isPresent() && !queuedThis;

        String action = queuedThis ? "Leave" : "Join";
        ItemStack item = GuiUtil.getNexoItem(icon,
                (queuedThis ? ChatColor.RED : ChatColor.GREEN) + "" + ChatColor.BOLD + action + " " + mode.displayName());
        if (item.getType() == Material.BARRIER) {
            item = new ItemStack(fallback);
            ItemMeta fallbackMeta = item.getItemMeta();
            if (fallbackMeta != null) {
                fallbackMeta.setDisplayName((queuedThis ? ChatColor.RED : ChatColor.GREEN)
                        + "" + ChatColor.BOLD + action + " " + mode.displayName());
                item.setItemMeta(fallbackMeta);
            }
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Party size: " + ChatColor.WHITE + mode.teamSize());
            queueManager.getAverageGear(viewerId).ifPresent(avg -> {
                StrongholdGearBand band = StrongholdGearBand.fromAverageGear(avg);
                lore.add(ChatColor.GRAY + "Avg Gear: " + ChatColor.WHITE + avg);
                lore.add(ChatColor.GRAY + "Band: " + band.display());
            });
            var runManager = Main.getInstance() == null ? null : Main.getInstance().getStrongholdRunManager();
            if (runManager != null) {
                int unlockedStage = Math.max(1, runManager.getHighestUnlockedStage(viewerId));
                lore.add(ChatColor.GRAY + "Best Checkpoint: " + ChatColor.WHITE + unlockedStage + "-1");
            }
            lore.add(" ");
            lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + queueManager.getQueuePopulation(mode)
                    + ChatColor.GRAY + " players in queue!");
            lore.add(" ");
            if (queuedOther) {
                lore.add(ChatColor.RED + "Leave your active queue first.");
            } else if (queuedThis) {
                lore.addAll(TooltipUtil.clickInstructions("to leave this queue", null));
            } else {
                lore.addAll(TooltipUtil.clickInstructions("to join this queue", "to open stage selection"));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void handleQueueClick(Player player, StrongholdQueueMode mode) {
        UUID id = player.getUniqueId();
        Optional<StrongholdQueueMode> current = queueManager.getMode(id);
        if (current.isPresent() && !current.get().equals(mode)) {
            send(player, MessageType.ERROR, "Leave your current Stronghold queue before joining another.");
            refreshOpenInventories();
            return;
        }

        if (current.isPresent()) {
            queueManager.leave(id);
            send(player, MessageType.INFO, "You left the " + current.get().displayName() + ChatColor.GRAY + " queue.");
            refreshOpenInventories();
            return;
        }

        StrongholdStartupProfiler profiler = StrongholdStartupProfiler.startOrContinue(Main.getInstance(), player);
        long queueStep = profiler == null ? 0L : profiler.stepStarted("Queue join request (" + mode.name() + ")");
        StrongholdQueueManager.QueueJoinOutcome outcome = queueManager.join(player, mode);
        if (outcome.result() == StrongholdQueueManager.QueueJoinResult.JOINED
                || outcome.result() == StrongholdQueueManager.QueueJoinResult.STARTED) {
            String success = outcome.result() == StrongholdQueueManager.QueueJoinResult.STARTED
                    ? "Generating your solo Stronghold run."
                    : "You joined the " + mode.displayName() + ChatColor.GRAY + " queue.";
            send(player, MessageType.SUCCESS, success);
        } else {
            send(player, MessageType.ERROR, outcome.message() == null
                    ? ChatColor.RED + "Unable to join Stronghold queue."
                    : outcome.message());
        }
        if (profiler != null) {
            profiler.stepFinished("Queue join request (" + mode.name() + ")", queueStep);
        }
        refreshOpenInventories();
    }

    private void renderWidgets(Inventory inv, Player player) {
        GuiLayout layout = new GuiLayout(inv);
        GuiContext context = new GuiContext(player, inv);
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

    private void refreshOpenInventories() {
        Iterator<Map.Entry<UUID, Inventory>> iterator = openInventories.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Inventory> entry = iterator.next();
            UUID viewerId = entry.getKey();
            Inventory inv = entry.getValue();

            boolean stillOpen = false;
            Player player = null;
            for (HumanEntity viewer : inv.getViewers()) {
                if (viewer.getUniqueId().equals(viewerId) && viewer instanceof Player p) {
                    player = p;
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
}
