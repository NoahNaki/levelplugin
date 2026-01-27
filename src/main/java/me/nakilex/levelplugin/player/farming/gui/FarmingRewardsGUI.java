package me.nakilex.levelplugin.player.farming.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.player.farming.data.FarmingCrop;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import me.nakilex.levelplugin.utils.gui.widgets.NexoButtonWidget;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class FarmingRewardsGUI implements Listener, CommandExecutor {

    private static final String TITLE = "Farming Rewards";
    private static final int INFO_SLOT = 8;
    private static final int CANCEL_SLOT = 45;
    private static final int WITHDRAW_SLOT = 46;
    private static final int DEPOSIT_SLOT = 52;
    private static final int CONFIRM_SLOT = 53;
    private final EconomyManager economyManager;
    private final Main plugin;
    private final List<GuiWidget> widgets;

    public FarmingRewardsGUI(Main plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.widgets = buildWidgets();
        plugin.getCommand("farmrewards").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void open(Player player) {
        Inventory inv = GuiBuilder.create(54, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();

        renderWidgets(inv, player);
        player.openInventory(inv);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        open(player);
        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!GuiUtil.titleMatches(event.getView().getTitle(), TITLE)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory top = event.getView().getTopInventory();
        int rawSlot = event.getRawSlot();
        ItemStack current = event.getCurrentItem();

        if (handleWidgetClick(event, player)) {
            return;
        }

        if (rawSlot < top.getSize()) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR && FarmingCrop.fromItem(cursor.getType()) == null) {
                event.setCancelled(true);
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "Only crops can be sold here.");
                return;
            }
            if (current != null && current.getType() != Material.AIR
                    && FarmingCrop.fromItem(current.getType()) == null) {
                event.setCancelled(true);
                return;
            }
        } else if (event.isShiftClick() && current != null && current.getType() != Material.AIR
                && FarmingCrop.fromItem(current.getType()) == null) {
            event.setCancelled(true);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "Only crops can be sold here.");
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!GuiUtil.titleMatches(event.getView().getTitle(), TITLE)) return;
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < event.getView().getTopInventory().getSize() && isControlSlot(rawSlot)) {
                event.setCancelled(true);
                return;
            }
        }
        ItemStack item = event.getOldCursor();
        if (item == null || item.getType() == Material.AIR) return;
        if (FarmingCrop.fromItem(item.getType()) == null) {
            event.setCancelled(true);
        }
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> widgetList = new ArrayList<>();
        widgetList.add(new NexoButtonWidget(INFO_SLOT, "info", ChatColor.YELLOW + "Information",
                context -> buildInfoLore(), null));
        widgetList.add(new NexoButtonWidget(CANCEL_SLOT, "cross", ChatColor.RED + "Cancel",
                null, (click, context) -> context.player().closeInventory()));
        widgetList.add(new NexoButtonWidget(WITHDRAW_SLOT, "arrow_down", ChatColor.YELLOW + "Return All",
                null, (click, context) -> withdrawAll(context.player(), context.inventory())));
        widgetList.add(new NexoButtonWidget(DEPOSIT_SLOT, "arrow_up", ChatColor.YELLOW + "Deposit All",
                null, (click, context) -> depositAll(context.player(), context.inventory())));
        widgetList.add(new NexoButtonWidget(CONFIRM_SLOT, "check", ChatColor.GREEN + "Confirm Sale",
                null, (click, context) -> handleSell(context.player(), context.inventory())));
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

    private void handleSell(Player player, Inventory top) {
        int total = 0;
        for (int slot = 0; slot < top.getSize(); slot++) {
            if (isControlSlot(slot)) continue;
            ItemStack stack = top.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) continue;
            FarmingCrop crop = FarmingCrop.fromItem(stack.getType());
            if (crop == null) continue;
            total += crop.getSellValue() * stack.getAmount();
            top.setItem(slot, null);
        }

        if (total <= 0) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "Place crops in the menu to sell them.");
            return;
        }

        economyManager.addCoins(player, total, false);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Sold your harvest for " + ChatColor.YELLOW + total + " <glyph:coins_icon>.");
        player.updateInventory();
    }

    private void depositAll(Player player, Inventory top) {
        ItemStack[] contents = player.getInventory().getContents();
        boolean moved = false;
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() == Material.AIR) continue;
            if (FarmingCrop.fromItem(stack.getType()) == null) continue;
            int slot = findEmptySlot(top);
            if (slot == -1) {
                break;
            }
            top.setItem(slot, stack);
            contents[i] = null;
            moved = true;
        }
        player.getInventory().setContents(contents);
        if (!moved) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "You have no crops to deposit.");
        }
    }

    private void withdrawAll(Player player, Inventory top) {
        boolean moved = false;
        for (int slot = 0; slot < top.getSize(); slot++) {
            if (isControlSlot(slot)) continue;
            ItemStack stack = top.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) continue;
            if (FarmingCrop.fromItem(stack.getType()) == null) continue;
            java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
            if (leftover.isEmpty()) {
                top.setItem(slot, null);
                moved = true;
            } else {
                top.setItem(slot, leftover.values().iterator().next());
                break;
            }
        }
        if (!moved) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "No crops to withdraw.");
        }
        player.updateInventory();
    }

    private int findEmptySlot(Inventory top) {
        for (int slot = 0; slot < top.getSize(); slot++) {
            if (isControlSlot(slot)) continue;
            ItemStack stack = top.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) {
                return slot;
            }
        }
        return -1;
    }

    private boolean isControlSlot(int slot) {
        return widgets.stream().anyMatch(widget -> widget.handlesSlot(slot));
    }

    private List<String> buildInfoLore() {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Place harvested crops in the center.");
        lore.add(ChatColor.GRAY + "Confirm to sell them for coins.");
        lore.add("");
        lore.add(ChatColor.GOLD + "Crop Value:");
        for (FarmingCrop crop : FarmingCrop.values()) {
            lore.add(ChatColor.GRAY + "  " + formatCropName(crop) + ": "
                    + ChatColor.YELLOW + crop.getSellValue() + " <glyph:coins_icon>");
        }
        lore.add("");
        lore.addAll(TooltipUtil.clickInstructions("to confirm sale", null));
        return lore;
    }

    private String formatCropName(FarmingCrop crop) {
        String raw = crop.getItemMaterial().name().toLowerCase().replace('_', ' ');
        return raw.substring(0, 1).toUpperCase() + raw.substring(1);
    }
}
