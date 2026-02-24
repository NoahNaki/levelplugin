package me.nakilex.levelplugin.enchanting.gui;

import me.nakilex.levelplugin.enchanting.managers.EnchantManager;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.tools.FarmingToolEnchant;
import me.nakilex.levelplugin.items.tools.ToolDiscipline;
import me.nakilex.levelplugin.items.tools.WoodcuttingToolEnchant;
import me.nakilex.levelplugin.items.tools.ToolManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.quests.def.SharpestSecretQuest;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import me.nakilex.levelplugin.utils.gui.widgets.NexoButtonWidget;
import me.nakilex.levelplugin.guild.GuildManager;
import me.nakilex.levelplugin.guild.TownPerk;
import me.nakilex.levelplugin.guild.TownPerkManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class EnchantGUI implements Listener {
    private static final int SIZE = 27;
    private static final int INFO_SLOT = 8;
    private static final int ACTION_SLOT = 22;
    private static final String TITLE = "Enchant";

    private final EnchantManager manager;
    private final EconomyManager economy;
    private final Map<UUID, Inventory> open = new HashMap<>();
    private final List<GuiWidget> widgets;

    public EnchantGUI(EnchantManager manager, EconomyManager economy) {
        this.manager = manager;
        this.economy = economy;
        this.widgets = buildWidgets();
    }

    public void open(Player player) {
        Inventory gui = GuiBuilder.create(SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();
        gui.setItem(13, null);
        renderWidgets(gui, player);
        open.put(player.getUniqueId(), gui);
        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        Inventory gui = open.get(p.getUniqueId());
        if (gui == null || !e.getView().getTopInventory().equals(gui)) return;
        int rawSlot = e.getRawSlot();
        if (rawSlot == 13) {
            e.setCancelled(false);
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> update(p, gui), 1L);
            return;
        }

        if (rawSlot >= gui.getSize()) {
            if (e.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY
                    && (gui.getItem(13) == null || gui.getItem(13).getType().isAir())) {
                e.setCancelled(false);
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> update(p, gui), 1L);
            }
            return;
        }

        if (handleWidgetClick(e, p)) {
            return;
        }

        // Prevent taking filler/placeholder items from GUI slots.
        e.setCancelled(true);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory gui = open.get(player.getUniqueId());
        if (gui == null || !event.getView().getTopInventory().equals(gui)) {
            return;
        }
        boolean touchesTopInventory = event.getRawSlots().stream().anyMatch(slot -> slot < gui.getSize() && slot != 13);
        if (touchesTopInventory) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Inventory gui = open.get(e.getPlayer().getUniqueId());
        if (gui == null || !e.getInventory().equals(gui)) return;
        ItemStack it = gui.getItem(13);
        if (it != null && !it.getType().isAir()) {
            ((Player)e.getPlayer()).getInventory().addItem(it);
        }
        open.remove(e.getPlayer().getUniqueId());
    }

    private void update(Player p, Inventory gui) {
        renderWidgets(gui, p);
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> widgetList = new ArrayList<>();
        widgetList.add(new NexoButtonWidget(INFO_SLOT, "info", ChatColor.YELLOW + "Information",
                context -> buildInfoLore(), null));
        widgetList.add(new ActionWidget(ACTION_SLOT,
                context -> createEnchantButton(context.player(), context.inventory()),
                (click, context) -> handleEnchantClick(context.player(), context.inventory(), click.isShiftClick())));
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

    private List<String> buildInfoLore() {
        return Arrays.asList(
                ChatColor.GRAY + "Place a custom item or lifeskill tool in the center.",
                ChatColor.GRAY + "Click " + ChatColor.LIGHT_PURPLE + "Enchant" + ChatColor.GRAY + " to add",
                ChatColor.GRAY + "a random prefix or tool enchant.",
                ChatColor.GRAY + "Cost doubles every enchant."
        );
    }

    private ItemStack createEnchantButton(Player player, Inventory inventory) {
        boolean freeEnchant = SharpestSecretQuest.shouldReceiveFreeEnchant(player.getUniqueId());
        EnchantButtonState state = resolveEnchantButtonState(player, inventory, freeEnchant);
        ItemStack item = new ItemStack(Material.ENCHANTING_TABLE);
        var meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Enchant");
        List<String> lore = new ArrayList<>();
        if (freeEnchant) {
            lore.add(ChatColor.GRAY + "Cost: " + ChatColor.AQUA + "Covered (quest reward)");
        } else if (state.cost() > 0) {
            lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GOLD + "<glyph:coins_icon> " + state.cost());
        } else {
            lore.add(ChatColor.GRAY + "Place item to enchant");
        }
        if (!freeEnchant && state.cost() > 0 && SharpestSecretQuest.hasEnchantToken(player)) {
            lore.add(ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + "Shift-click to spend an Enchant Token.");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private EnchantButtonState resolveEnchantButtonState(Player player, Inventory inventory, boolean freeEnchant) {
        ItemStack stack = inventory.getItem(13);
        if (stack == null || stack.getType().isAir()) {
            return new EnchantButtonState(0);
        }
        CustomItem ci = ItemManager.getInstance().getCustomItemFromItemStack(stack);
        me.nakilex.levelplugin.items.tools.CustomTool tool = ToolManager.getInstance().getTool(stack);
        boolean isEnchantableTool = tool != null
                && (tool.getDiscipline() == ToolDiscipline.FARMING || tool.getDiscipline() == ToolDiscipline.WOODCUTTING);
        if (ci == null && !isEnchantableTool) {
            return new EnchantButtonState(0);
        }
        int baseCost = ci != null ? manager.getEnchantCost(ci) : manager.getEnchantCost(stack);
        int cost = freeEnchant ? 0 : TownPerkManager.getInstance().applyDiscount(
                GuildManager.getInstance().getGuild(player.getUniqueId()),
                TownPerk.ENCHANTING_DISCOUNT,
                baseCost);
        return new EnchantButtonState(cost);
    }

    private void handleEnchantClick(Player player, Inventory inventory, boolean shiftClick) {
        ItemStack item = inventory.getItem(13);
        if (item == null || item.getType().isAir()) return;
        CustomItem ci = ItemManager.getInstance().getCustomItemFromItemStack(item);
        me.nakilex.levelplugin.items.tools.CustomTool tool = ToolManager.getInstance().getTool(item);
        boolean isFarmingTool = tool != null && tool.getDiscipline() == ToolDiscipline.FARMING;
        boolean isWoodcuttingTool = tool != null && tool.getDiscipline() == ToolDiscipline.WOODCUTTING;
        if (ci == null && !isFarmingTool && !isWoodcuttingTool) return;
        boolean freeEnchant = SharpestSecretQuest.shouldReceiveFreeEnchant(player.getUniqueId());
        int baseCost = ci != null ? manager.getEnchantCost(ci) : manager.getEnchantCost(item);
        int discountedCost = TownPerkManager.getInstance().applyDiscount(
                GuildManager.getInstance().getGuild(player.getUniqueId()),
                TownPerk.ENCHANTING_DISCOUNT,
                baseCost);

        boolean usingToken = false;
        if (!freeEnchant && shiftClick) {
            usingToken = SharpestSecretQuest.consumeEnchantToken(player);
            if (!usingToken) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "You don't have an Enchant Token to spend.");
            }
        }

        if (!freeEnchant && !usingToken) {
            try {
                economy.deductCoins(player, discountedCost);
            } catch (IllegalArgumentException ex) {
                player.sendMessage(ChatColor.RED + "Not enough coins! Cost: " + discountedCost);
                return;
            }
        }
        if (ci != null) {
            String prefix = manager.enchant(player, item, ci);
            inventory.setItem(13, item);
            ItemUtil.updateTooltip(item, player);
            player.sendMessage(ChatColor.GREEN + "Item enchanted with " + ChatColor.LIGHT_PURPLE + prefix + ChatColor.GREEN + "!");
        } else if (isFarmingTool) {
            FarmingToolEnchant enchant = manager.enchantFarmingTool(player, item);
            inventory.setItem(13, item);
            ItemUtil.updateCustomToolTooltip(item, player);
            if (enchant != null) {
                player.sendMessage(ChatColor.GREEN + "Tool enchanted with " + ChatColor.LIGHT_PURPLE
                        + enchant.getDisplayName() + ChatColor.GREEN + "!");
            }
        } else if (isWoodcuttingTool) {
            WoodcuttingToolEnchant enchant = manager.enchantWoodcuttingTool(player, item);
            inventory.setItem(13, item);
            ItemUtil.updateCustomToolTooltip(item, player);
            if (enchant != null) {
                player.sendMessage(ChatColor.GREEN + "Tool enchanted with " + ChatColor.LIGHT_PURPLE
                        + enchant.getDisplayName() + ChatColor.GREEN + "!");
            }
        }
        if (freeEnchant) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "Osiris covers your first enchant.");
        } else if (usingToken) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "Your Enchant Token dissolves into violet sparks.");
        }
        me.nakilex.levelplugin.Main.getInstance().getQuestManager().handleEnchant(player);
        update(player, inventory);
    }

    private record EnchantButtonState(int cost) {}
}
