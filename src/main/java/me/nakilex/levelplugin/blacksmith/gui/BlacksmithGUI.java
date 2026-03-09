package me.nakilex.levelplugin.blacksmith.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.blacksmith.managers.ItemRepairManager;
import me.nakilex.levelplugin.blacksmith.managers.ItemUpgradeManager;
import me.nakilex.levelplugin.blacksmith.managers.ItemRerollManager;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import me.nakilex.levelplugin.guild.GuildManager;
import me.nakilex.levelplugin.guild.TownPerk;
import me.nakilex.levelplugin.guild.TownPerkManager;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.gui.flow.GuiActionOperation;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

public class BlacksmithGUI implements Listener {

    private static final int GUI_SIZE = 27;
    private static final String GUI_TITLE_UPGRADE = "<glyph:anvil_icon> Blacksmith: Upgrade";
    private static final String GUI_TITLE_REPAIR  = "<glyph:anvil_icon> Blacksmith: Repair";
    private static final String GUI_TITLE_REROLL = "<glyph:anvil_icon> Blacksmith: Reroll";

    private final EconomyManager economyManager;
    private final ItemUpgradeManager upgradeManager;
    private final ItemRepairManager repairManager;
    private final ItemRerollManager rerollManager;
    private final ItemManager itemManager;
    private final Map<UUID, Inventory> openInventories = new HashMap<>();
    private final Map<UUID, BlacksmithMode> openModes = new HashMap<>();
    private final List<GuiWidget> widgets;
    private final ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);

    private enum BlacksmithMode {
        UPGRADE,
        REPAIR,
        REROLL
    }

    public BlacksmithGUI(EconomyManager economyManager, ItemUpgradeManager upgradeManager,
                         ItemManager itemManager, ItemRepairManager repairManager) {
        this.economyManager = economyManager;
        this.upgradeManager = upgradeManager;
        this.repairManager = repairManager;
        this.itemManager = itemManager;
        this.rerollManager = new ItemRerollManager();
        this.widgets = buildWidgets();
    }

    public void openUpgradeGUI(Player player) {
        openGui(player, BlacksmithMode.UPGRADE);
    }

    public void openRepairGUI(Player player) {
        openGui(player, BlacksmithMode.REPAIR);
    }

    public void openRerollGUI(Player player) {
        openGui(player, BlacksmithMode.REROLL);
    }

    private void openGui(Player player, BlacksmithMode mode) {
        Inventory gui = GuiBuilder.create(GUI_SIZE, getTitle(mode))
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();
        openInventories.put(player.getUniqueId(), gui);
        openModes.put(player.getUniqueId(), mode);
        renderWidgets(gui, player);
        if (mode == BlacksmithMode.REROLL) {
            gui.setItem(11, null);
            gui.setItem(13, null);
            gui.setItem(15, null);
        } else {
            gui.setItem(13, null);
        }
        updateActionButton(player, gui, mode);
        player.openInventory(gui);
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> widgetList = new ArrayList<>();
        widgetList.add(new ActionWidget(8, context -> createInfoItem(getMode(context.player())), null));
        widgetList.add(new ActionWidget(9, context -> createNavItem(getMode(context.player()), true),
                (click, context) -> handleNavigation(context.player(), getMode(context.player()), true)));
        widgetList.add(new ActionWidget(17, context -> createNavItem(getMode(context.player()), false),
                (click, context) -> handleNavigation(context.player(), getMode(context.player()), false)));
        widgetList.add(new ActionWidget(0, context -> createRepairAllWidget(context), (click, context) -> {
            if (getMode(context.player()) == BlacksmithMode.REPAIR) {
                handleRepairAllClick(context.player());
                context.inventory().setItem(0, createRepairAllButton(calculateTotalRepairCost(context.player())));
            }
        }));
        widgetList.add(new ActionWidget(22, context -> createActionItem(context), (click, context) -> {
            handleActionButtonClick(context.player(), context.inventory(), getMode(context.player()));
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                Inventory updatedGui = openInventories.get(context.player().getUniqueId());
                if (updatedGui != null) updateActionButton(context.player(), updatedGui, getMode(context.player()));
            }, 1L);
        }));
        return widgetList;
    }

    private void renderWidgets(Inventory inventory, Player player) {
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    private String getTitle(BlacksmithMode mode) {
        return switch (mode) {
            case UPGRADE -> GUI_TITLE_UPGRADE;
            case REPAIR -> GUI_TITLE_REPAIR;
            case REROLL -> GUI_TITLE_REROLL;
        };
    }

    private BlacksmithMode getMode(Player player) {
        BlacksmithMode mode = openModes.get(player.getUniqueId());
        if (mode != null) {
            return mode;
        }
        String title = player.getOpenInventory().getTitle();
        if (GuiUtil.titleMatches(title, GUI_TITLE_UPGRADE)) {
            return BlacksmithMode.UPGRADE;
        }
        if (GuiUtil.titleMatches(title, GUI_TITLE_REPAIR)) {
            return BlacksmithMode.REPAIR;
        }
        if (GuiUtil.titleMatches(title, GUI_TITLE_REROLL)) {
            return BlacksmithMode.REROLL;
        }
        return BlacksmithMode.UPGRADE;
    }

    private ItemStack createInfoItem(BlacksmithMode mode) {
        return switch (mode) {
            case UPGRADE -> GuiUtil.getNexoItem("info", ChatColor.YELLOW + "Information", Arrays.asList(
                    ChatColor.GRAY + "",
                    ChatColor.GRAY + "Upgrade Success Rates:",
                    ChatColor.GRAY + "",
                    ChatColor.GRAY + "  +0 ➜ +1: " + ChatColor.WHITE + "33%",
                    ChatColor.GRAY + "  +1 ➜ +2: " + ChatColor.WHITE + "15%",
                    ChatColor.GRAY + "  +2 ➜ +3: " + ChatColor.WHITE + "10%",
                    ChatColor.GRAY + "  +3 ➜ +4: " + ChatColor.WHITE + "5%",
                    ChatColor.GRAY + "  +4 ➜ +5: " + ChatColor.WHITE + "2%",
                    "",
                    ChatColor.GRAY + "Upgrade costs scale with " + ChatColor.AQUA + "rarity" + ChatColor.GRAY + " and",
                    ChatColor.GRAY + "current upgrade " + ChatColor.AQUA + "tier" + ChatColor.GRAY + "."
            ));
            case REPAIR -> GuiUtil.getNexoItem("info", ChatColor.YELLOW + "Information", Arrays.asList(
                    ChatColor.GRAY + "",
                    ChatColor.GRAY + "Costs increase with item " + ChatColor.AQUA + "rarity",
                    ChatColor.GRAY + "and " + ChatColor.AQUA + "durability" + ChatColor.GRAY + " lost.",
                    "",
                    ChatColor.GRAY + "Place a damaged item in the center.",
                    ChatColor.GRAY + "Use " + ChatColor.GREEN + "Repair Item" + ChatColor.GRAY + " or",
                    ChatColor.GREEN + "Repair All Items" + ChatColor.GRAY + " to fix it."
            ));
            case REROLL -> GuiUtil.getNexoItem("info", ChatColor.YELLOW + "Information", Arrays.asList(
                    ChatColor.GRAY + "",
                    ChatColor.GRAY + "Use the left slot for your item and",
                    ChatColor.GRAY + "the right slot for a stat placeholder.",
                    ChatColor.GRAY + "Press the check mark to reroll that stat."
            ));
        };
    }

    private ItemStack createNavItem(BlacksmithMode mode, boolean left) {
        return switch (mode) {
            case UPGRADE -> left
                    ? GuiUtil.getNexoItem("arrow_left", ChatColor.GRAY + "Go to Reroll")
                    : GuiUtil.getNexoItem("arrow_right", ChatColor.GRAY + "Go to Repair");
            case REPAIR -> left
                    ? GuiUtil.getNexoItem("arrow_left", ChatColor.GRAY + "Go to Upgrade")
                    : GuiUtil.getNexoItem("arrow_right", ChatColor.GRAY + "Go to Reroll");
            case REROLL -> left
                    ? GuiUtil.getNexoItem("arrow_left", ChatColor.GRAY + "Go to Repair")
                    : GuiUtil.getNexoItem("arrow_right", ChatColor.GRAY + "Go to Upgrade");
        };
    }

    private ItemStack createRepairAllWidget(GuiContext context) {
        if (getMode(context.player()) != BlacksmithMode.REPAIR) {
            return filler.clone();
        }
        return createRepairAllButton(calculateTotalRepairCost(context.player()));
    }

    private ItemStack createUpgradeButton(int upgradeCost, int successChance) {
        List<String> lore = new ArrayList<>();

        if (upgradeCost < 0) {
            // Negative cost indicates the item reached the upgrade cap
            lore.add("§7This item cannot be upgraded further.");
            return GuiUtil.createGuiItem(Material.ANVIL, "§cMax Level", lore);
        } else {
            if (upgradeCost > 0) {
                lore.add("§7Cost: §6<glyph:coins_icon> " + upgradeCost);
                lore.add("§7Success Chance: §6" + successChance + "%");
            } else {
                lore.add("§7Place an item in upgrade slot.");
            }
            return GuiUtil.createGuiItem(Material.ANVIL, "§aUpgrade", lore);
        }
    }

    private ItemStack createRepairButton(int cost) {
        List<String> lore = new ArrayList<>();
        if (cost > 0) {
            lore.add("§7Cost: §6<glyph:coins_icon> " + cost);
        } else {
            lore.add("§7Place an item in the repair slot.");
        }
        return GuiUtil.createGuiItem(Material.ANVIL, "§bRepair Item", lore);
    }

    private ItemStack createRerollButton(int cost) {
        List<String> lore = new ArrayList<>();
        if (cost > 0) {
            lore.add("§7Cost: §6<glyph:coins_icon> " + cost);
        } else {
            lore.add("§7Place item and placeholder.");
        }
        return GuiUtil.getNexoItem("check", ChatColor.GREEN + "Reroll Stat", lore);
    }


    private ItemStack createRepairAllButton(int totalCost) {
        List<String> lore = new ArrayList<>();
        if (totalCost > 0) {
            lore.add("§7Total Cost: §6<glyph:coins_icon> " + totalCost);
        } else {
            lore.add("§7No damaged items found.");
        }
        return GuiUtil.createGuiItem(Material.GRINDSTONE, "§cRepair All Items", lore);
    }

    private int calculateTotalRepairCost(Player player) {
        int total = 0;
        me.nakilex.levelplugin.guild.Guild g = GuildManager.getInstance().getGuild(player.getUniqueId());
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            CustomItem ci = itemManager.getCustomItemFromItemStack(item);
            if (ci != null && ci.getCurrentDurability() < ci.getMaxDurability()) {
                int base = repairManager.getRepairCost(ci);
                total += TownPerkManager.getInstance().applyDiscount(g, TownPerk.BLACKSMITH_DISCOUNT, base);
            }
        }
        return total;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory gui = openInventories.get(player.getUniqueId());
        if (gui == null || !event.getView().getTopInventory().equals(gui)) return;

        int rawSlot = event.getRawSlot();
        BlacksmithMode mode = getMode(player);

        // Allow placing item/placeholder in reroll slots
        if (mode == BlacksmithMode.REROLL && (rawSlot == 11 || rawSlot == 15)) {
            event.setCancelled(false);
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                Inventory updatedGui = openInventories.get(player.getUniqueId());
                if (updatedGui != null) updateActionButton(player, updatedGui, mode);
            }, 1L);
            return;
        }

        if (mode == BlacksmithMode.REROLL && rawSlot == 13) {
            if (event.getCursor() == null || event.getCursor().getType().isAir()) {
                event.setCancelled(false); // allow taking result item
            } else {
                event.setCancelled(true); // prevent placing items here
            }
            return;
        }

        // Allow placing into slot 13 only manually on upgrade/repair
        if (rawSlot == 13 && mode != BlacksmithMode.REROLL) {
            event.setCancelled(false);
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                Inventory updatedGui = openInventories.get(player.getUniqueId());
                if (updatedGui != null) updateActionButton(player, updatedGui, mode);
            }, 1L);
            return;
        }


// Allow dragging items into appropriate slots
        if (event.getAction() == InventoryAction.PLACE_ALL ||
            event.getAction() == InventoryAction.PLACE_SOME ||
            event.getAction() == InventoryAction.PLACE_ONE ||
            event.getAction() == InventoryAction.SWAP_WITH_CURSOR ||
            event.getAction() == InventoryAction.HOTBAR_SWAP) {
            if (mode == BlacksmithMode.REROLL && (event.getSlot() == 11 || event.getSlot() == 15)) {
                event.setCancelled(false);
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                    Inventory updatedGui = openInventories.get(player.getUniqueId());
                    if (updatedGui != null) updateActionButton(player, updatedGui, mode);
                }, 1L);
                return;
            } else if (mode != BlacksmithMode.REROLL && event.getSlot() == 13) {
                event.setCancelled(false);
                return;
            }
        }

        // Cancel all interactions outside the GUI
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                    Inventory updatedGui = openInventories.get(player.getUniqueId());
                    if (updatedGui != null) updateActionButton(player, updatedGui, mode);
                }, 1L);
            }
            return; // allow interactions with the player's inventory
        }

        event.setCancelled(true);

        handleWidgetClick(event, player);
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
        widget.onClick(slot, event.getClick(), new GuiContext(player, event.getView().getTopInventory()));
        return true;
    }

    private void handleNavigation(Player player, BlacksmithMode mode, boolean left) {
        Inventory gui = openInventories.get(player.getUniqueId());
        if (gui == null) {
            return;
        }
        ItemStack carriedItem = mode == BlacksmithMode.REROLL ? gui.getItem(11) : gui.getItem(13);
        ItemStack placeholder = mode == BlacksmithMode.REROLL ? gui.getItem(15) : null;
        BlacksmithMode targetMode = switch (mode) {
            case UPGRADE -> left ? BlacksmithMode.REROLL : BlacksmithMode.REPAIR;
            case REPAIR -> left ? BlacksmithMode.UPGRADE : BlacksmithMode.REROLL;
            case REROLL -> left ? BlacksmithMode.REPAIR : BlacksmithMode.UPGRADE;
        };
        openGui(player, targetMode);
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            Inventory newGui = openInventories.get(player.getUniqueId());
            if (newGui == null) {
                return;
            }
            if (targetMode == BlacksmithMode.REROLL) {
                newGui.setItem(11, carriedItem);
                newGui.setItem(15, placeholder);
            } else {
                newGui.setItem(13, carriedItem);
                updateActionButton(player, newGui, targetMode);
            }
        }, 1L);
    }

    private void handleActionButtonClick(Player player, Inventory gui, BlacksmithMode mode) {
        resolveOperation(mode).execute(player, gui);
    }

    private GuiActionOperation resolveOperation(BlacksmithMode mode) {
        return switch (mode) {
            case UPGRADE -> new GuiActionOperation() {
                @Override
                public ItemStack createActionButton(Player player, Inventory gui) {
                    ItemStack current = gui.getItem(13);
                    if (current == null || current.getType().isAir()) return createUpgradeButton(0, 0);
                    CustomItem ci = itemManager.getCustomItemFromItemStack(current);
                    if (ci == null) return createUpgradeButton(0, 0);
                    if (ci.getUpgradeLevel() >= 5) return createUpgradeButton(-1, 0);
                    int cost = TownPerkManager.getInstance().applyDiscount(
                            GuildManager.getInstance().getGuild(player.getUniqueId()),
                            TownPerk.BLACKSMITH_DISCOUNT,
                            upgradeManager.getUpgradeCost(ci));
                    return createUpgradeButton(cost, upgradeManager.getSuccessChance(ci));
                }

                @Override
                public void execute(Player player, Inventory gui) {
                    ItemStack item = gui.getItem(13);
                    if (item == null || item.getType().isAir()) return;
                    CustomItem ci = itemManager.getCustomItemFromItemStack(item);
                    if (ci == null) return;
                    if (ci.getUpgradeLevel() >= 5) {
                        send(player, MessageType.ERROR, "Item has reached the maximum upgrade level.");
                        return;
                    }
                    int cost = TownPerkManager.getInstance().applyDiscount(
                            GuildManager.getInstance().getGuild(player.getUniqueId()),
                            TownPerk.BLACKSMITH_DISCOUNT,
                            upgradeManager.getUpgradeCost(ci));
                    try { economyManager.deductCoins(player, cost); }
                    catch (IllegalArgumentException ex) {
                        send(player, MessageType.ERROR, "Not enough coins! Upgrade cost: §6<glyph:coins_icon> " + cost);
                        return;
                    }
                    if (upgradeManager.attemptUpgrade(player, item, ci)) {
                        send(player, MessageType.SUCCESS, "Upgrade successful!");
                        Main.getInstance().getQuestManager().handleBlacksmithUpgrade(player, String.valueOf(ci.getId()));
                        gui.setItem(13, item);
                    } else {
                        send(player, MessageType.ERROR, "Upgrade failed!");
                    }
                }
            };
            case REPAIR -> new GuiActionOperation() {
                @Override
                public ItemStack createActionButton(Player player, Inventory gui) {
                    ItemStack current = gui.getItem(13);
                    if (current == null || current.getType().isAir()) return createRepairButton(0);
                    CustomItem ci = itemManager.getCustomItemFromItemStack(current);
                    if (ci == null) return createRepairButton(0);
                    int cost = TownPerkManager.getInstance().applyDiscount(
                            GuildManager.getInstance().getGuild(player.getUniqueId()),
                            TownPerk.BLACKSMITH_DISCOUNT,
                            repairManager.getRepairCost(ci));
                    return createRepairButton(cost);
                }

                @Override
                public void execute(Player player, Inventory gui) {
                    ItemStack item = gui.getItem(13);
                    if (item == null || item.getType().isAir()) return;
                    CustomItem ci = itemManager.getCustomItemFromItemStack(item);
                    if (ci == null) return;
                    int cost = TownPerkManager.getInstance().applyDiscount(
                            GuildManager.getInstance().getGuild(player.getUniqueId()),
                            TownPerk.BLACKSMITH_DISCOUNT,
                            repairManager.getRepairCost(ci));
                    try { economyManager.deductCoins(player, cost); }
                    catch (IllegalArgumentException ex) {
                        send(player, MessageType.ERROR, "Not enough coins to repair! Cost: §6<glyph:coins_icon> " + cost);
                        return;
                    }
                    if (repairManager.repairItem(player, item, ci)) {
                        send(player, MessageType.SUCCESS, "Item repaired!");
                        gui.setItem(13, item);
                        ItemUtil.updateTooltip(item, player);
                        Main.getInstance().getQuestManager().handleRepair(player, String.valueOf(ci.getId()));
                    }
                }
            };
            case REROLL -> new GuiActionOperation() {
                @Override
                public ItemStack createActionButton(Player player, Inventory gui) {
                    ItemStack current = gui.getItem(11);
                    if (current == null || current.getType().isAir()) return createRerollButton(0);
                    CustomItem ci = itemManager.getCustomItemFromItemStack(current);
                    if (ci == null) return createRerollButton(0);
                    int cost = TownPerkManager.getInstance().applyDiscount(
                            GuildManager.getInstance().getGuild(player.getUniqueId()),
                            TownPerk.BLACKSMITH_DISCOUNT,
                            rerollManager.getRerollCost(ci));
                    return createRerollButton(cost);
                }

                @Override
                public void execute(Player player, Inventory gui) {
                    ItemStack item = gui.getItem(11);
                    if (item == null || item.getType().isAir()) return;
                    CustomItem ci = itemManager.getCustomItemFromItemStack(item);
                    if (ci == null) return;
                    ItemStack placeholder = gui.getItem(15);
                    if (placeholder == null || placeholder.getType().isAir()) {
                        send(player, MessageType.WARNING, "Place a stat placeholder on the right.");
                        return;
                    }
                    StatType stat = materialToStat(placeholder.getType());
                    if (stat == null) {
                        send(player, MessageType.ERROR, "Invalid placeholder item!");
                        return;
                    }
                    if (!rerollManager.hasStat(ci, stat)) {
                        send(player, MessageType.ERROR, "This item does not have " + statDisplayName(stat) + "!");
                        return;
                    }
                    int cost = TownPerkManager.getInstance().applyDiscount(
                            GuildManager.getInstance().getGuild(player.getUniqueId()),
                            TownPerk.BLACKSMITH_DISCOUNT,
                            rerollManager.getRerollCost(ci));
                    try { economyManager.deductCoins(player, cost); }
                    catch (IllegalArgumentException ex) {
                        send(player, MessageType.ERROR, "Not enough coins to reroll! Cost: §6<glyph:coins_icon>" + cost);
                        return;
                    }
                    int diff = rerollManager.rerollStat(player, item, ci, stat);
                    Main.getInstance().getQuestManager().handleReroll(player, String.valueOf(ci.getId()));
                    gui.setItem(13, item.clone());
                    gui.setItem(11, null);
                    placeholder.setAmount(placeholder.getAmount() - 1);
                    if (placeholder.getAmount() <= 0) gui.setItem(15, null);
                    String message = ChatColor.GOLD + "" + ChatColor.BOLD + "STAT REROLLED! "
                            + ChatColor.YELLOW + statDisplayName(stat) + (diff >= 0
                            ? " increased by " + ChatColor.GREEN + "+" + diff
                            : " decreased by " + ChatColor.RED + diff);
                    send(player, MessageType.SUCCESS, message);
                }
            };
        };
    }

    private ItemStack createActionItem(GuiContext context) {
        BlacksmithMode mode = getMode(context.player());
        return resolveOperation(mode).createActionButton(context.player(), context.inventory());
    }

    private void handleRepairAllClick(Player player) {
        int totalCost = 0;
        List<ItemStack> toRepair = new ArrayList<>();
        me.nakilex.levelplugin.guild.Guild g = GuildManager.getInstance().getGuild(player.getUniqueId());
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            CustomItem ci = itemManager.getCustomItemFromItemStack(item);
            if (ci != null && ci.getCurrentDurability() < ci.getMaxDurability()) {
                int base = repairManager.getRepairCost(ci);
                totalCost += TownPerkManager.getInstance().applyDiscount(g, TownPerk.BLACKSMITH_DISCOUNT, base);
                toRepair.add(item);
            }
        }
        if (totalCost == 0) {
            send(player, MessageType.INFO, "No damaged items found.");
            return;
        }
        try {
            economyManager.deductCoins(player, totalCost);
        } catch (IllegalArgumentException ex) {
            send(player, MessageType.ERROR, "You need §6<glyph:coins_icon> " + totalCost + " §cto repair all items.");
            return;
        }
        for (ItemStack item : toRepair) {
            CustomItem ci = itemManager.getCustomItemFromItemStack(item);
            if (ci != null) {
                repairManager.repairItem(player, item, ci);
                ItemUtil.updateTooltip(item, player);
                Main.getInstance().getQuestManager().handleRepair(player, String.valueOf(ci.getId()));
            }
        }
        send(player, MessageType.SUCCESS, "All items repaired! Total cost: §6<glyph:coins_icon> " + totalCost);
    }

    private void updateActionButton(Player player, Inventory gui, BlacksmithMode mode) {
        gui.setItem(22, resolveOperation(mode).createActionButton(player, gui));
    }

    private StatType materialToStat(Material mat) {
        return switch (mat) {
            case BORDURE_INDENTED_BANNER_PATTERN -> StatType.STR;
            case FLOWER_BANNER_PATTERN        -> StatType.INT;
            case FLOW_BANNER_PATTERN          -> StatType.AGI;
            case SKULL_BANNER_PATTERN         -> StatType.VIT;
            case GUSTER_BANNER_PATTERN        -> StatType.DEX;
            case GLOBE_BANNER_PATTERN         -> StatType.WIL;
            default -> null;
        };
    }

    private String statDisplayName(StatType stat) {
        return switch (stat) {
            case STR -> "Strength";
            case INT -> "Intelligence";
            case AGI -> "Agility";
            case DEX -> "Dexterity";
            case VIT -> "Vitality";
            case WIL -> "Will";
            case TEC -> "Technique";
        };
    }


    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        Inventory gui = openInventories.get(player.getUniqueId());
        if (gui == null || !event.getInventory().equals(gui)) return;
        String title = event.getView().getTitle();
        if (GuiUtil.titleMatches(title, GUI_TITLE_REROLL)) {
            ItemStack left = gui.getItem(11);
            ItemStack result = gui.getItem(13);
            ItemStack placeholder = gui.getItem(15);
            if (left != null && !left.getType().isAir()) player.getInventory().addItem(left);
            if (result != null && !result.getType().isAir()) player.getInventory().addItem(result);
            if (placeholder != null && !placeholder.getType().isAir()) player.getInventory().addItem(placeholder);
        } else {
            ItemStack item = gui.getItem(13);
            if (item != null && !item.getType().isAir()) player.getInventory().addItem(item);
        }
        openInventories.remove(player.getUniqueId());
        openModes.remove(player.getUniqueId());
    }
}
