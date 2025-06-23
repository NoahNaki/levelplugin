package me.nakilex.levelplugin.blacksmith.gui;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.blacksmith.managers.ItemRepairManager;
import me.nakilex.levelplugin.blacksmith.managers.ItemUpgradeManager;
import me.nakilex.levelplugin.blacksmith.managers.ItemRerollManager;
import me.nakilex.levelplugin.ego.EgoWeaponManager;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class BlacksmithGUI implements Listener {

    private static final int GUI_SIZE = 27;
    private static final String GUI_TITLE_UPGRADE = ChatColor.DARK_GRAY + "Blacksmith: Upgrade";
    private static final String GUI_TITLE_REPAIR  = ChatColor.DARK_GRAY + "Blacksmith: Repair";
    private static final String GUI_TITLE_REROLL = ChatColor.DARK_GRAY + "Blacksmith: Reroll";
    private static final String GUI_TITLE_EVOLVE = ChatColor.DARK_GRAY + "Blacksmith: Evolve";

    private final EconomyManager economyManager;
    private final ItemUpgradeManager upgradeManager;
    private final ItemRepairManager repairManager;
    private final ItemRerollManager rerollManager;
    private final ItemManager itemManager;
    private final Map<UUID, Inventory> openInventories = new HashMap<>();

    public BlacksmithGUI(EconomyManager economyManager, ItemUpgradeManager upgradeManager,
                         ItemManager itemManager, ItemRepairManager repairManager) {
        this.economyManager = economyManager;
        this.upgradeManager = upgradeManager;
        this.repairManager = repairManager;
        this.itemManager = itemManager;
        this.rerollManager = new ItemRerollManager();
    }

    public void openUpgradeGUI(Player player) {
        Inventory gui = Bukkit.createInventory(player, GUI_SIZE, GUI_TITLE_UPGRADE);
        fillGuiWithFiller(gui);
        gui.setItem(8, createUpgradeInfoItem());
        gui.setItem(9, getNexoItem("arrow_left", ChatColor.GRAY + "Go to Evolve"));
        gui.setItem(17, getNexoItem("arrow_right", ChatColor.GRAY + "Go to Repair"));
        gui.setItem(13, null);
        gui.setItem(22, createUpgradeButton(0, 0));
        openInventories.put(player.getUniqueId(), gui);
        player.openInventory(gui);
    }

    public void openRepairGUI(Player player) {
        Inventory gui = Bukkit.createInventory(player, GUI_SIZE, GUI_TITLE_REPAIR);
        fillGuiWithFiller(gui);
        gui.setItem(8, createRepairInfoItem());
        gui.setItem(9, getNexoItem("arrow_left", ChatColor.GRAY + "Go to Upgrade"));
        gui.setItem(17, getNexoItem("arrow_right", ChatColor.GRAY + "Go to Reroll"));
        gui.setItem(0, createRepairAllButton(calculateTotalRepairCost(player)));
        gui.setItem(13, null);
        gui.setItem(22, createRepairButton(0));
        openInventories.put(player.getUniqueId(), gui);
        player.openInventory(gui);
    }

    public void openRerollGUI(Player player) {
        Inventory gui = Bukkit.createInventory(player, GUI_SIZE, GUI_TITLE_REROLL);
        fillGuiWithFiller(gui);
        gui.setItem(8, createRerollInfoItem());
        gui.setItem(9, getNexoItem("arrow_left", ChatColor.GRAY + "Go to Repair"));
        gui.setItem(17, getNexoItem("arrow_right", ChatColor.GRAY + "Go to Evolve"));
        gui.setItem(11, null); // item slot
        gui.setItem(13, null); // result
        gui.setItem(15, null); // placeholder
        gui.setItem(22, createRerollButton(0));
        openInventories.put(player.getUniqueId(), gui);
        player.openInventory(gui);
    }

    public void openEvolveGUI(Player player) {
        Inventory gui = Bukkit.createInventory(player, GUI_SIZE, GUI_TITLE_EVOLVE);
        fillGuiWithFiller(gui);
        gui.setItem(8, createEvolveInfoItem());
        gui.setItem(9, getNexoItem("arrow_left", ChatColor.GRAY + "Go to Reroll"));
        gui.setItem(17, getNexoItem("arrow_right", ChatColor.GRAY + "Go to Upgrade"));
        gui.setItem(13, null);
        gui.setItem(22, createEvolveButton(false));
        openInventories.put(player.getUniqueId(), gui);
        player.openInventory(gui);
    }

    private void fillGuiWithFiller(Inventory gui) {
        ItemStack filler = createFiller();
        for (int i = 0; i < GUI_SIZE; i++) gui.setItem(i, filler);
    }

    private ItemStack createFiller() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }
        return glass;
    }

    private ItemStack getNexoItem(String id, String name) {
        ItemBuilder builder = NexoItems.itemFromId(id);
        if (builder == null) return new ItemStack(Material.BARRIER);
        ItemStack item = builder.build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createUpgradeInfoItem() {
        ItemStack info = getNexoItem("info", ChatColor.YELLOW + "Information");
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            meta.setLore(Arrays.asList(
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
            info.setItemMeta(meta);
        }
        return info;
    }

    private ItemStack createRepairInfoItem() {
        ItemStack info = getNexoItem("info", ChatColor.YELLOW + "Information");
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "",
                ChatColor.GRAY + "Costs increase with item " + ChatColor.AQUA + "rarity",
                ChatColor.GRAY + "and " + ChatColor.AQUA + "durability" + ChatColor.GRAY + " lost.",
                "",
                ChatColor.GRAY + "Place a damaged item in the center.",
                ChatColor.GRAY + "Use " + ChatColor.GREEN + "Repair Item" + ChatColor.GRAY + " or",
                ChatColor.GREEN + "Repair All Items" + ChatColor.GRAY + " to fix it."
            ));
            info.setItemMeta(meta);
        }
        return info;
    }

    private ItemStack createRerollInfoItem() {
        ItemStack info = getNexoItem("info", ChatColor.YELLOW + "Information");
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "",
                ChatColor.GRAY + "Use the left slot for your item and",
                ChatColor.GRAY + "the right slot for a stat placeholder.",
                ChatColor.GRAY + "Press the check mark to reroll that stat."
            ));
            info.setItemMeta(meta);
        }
        return info;
    }

    private ItemStack createEvolveInfoItem() {
        ItemStack info = getNexoItem("info", ChatColor.YELLOW + "Information");
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "",
                ChatColor.GRAY + "Place a Rank 10 Ego weapon",
                ChatColor.GRAY + "in the center slot to evolve it."
            ));
            info.setItemMeta(meta);
        }
        return info;
    }

    private ItemStack createUpgradeButton(int upgradeCost, int successChance) {
        ItemStack upgrade = new ItemStack(Material.ANVIL);
        ItemMeta meta = upgrade.getItemMeta();
        List<String> lore = new ArrayList<>();

        if (upgradeCost < 0) {
            // Negative cost indicates the item reached the upgrade cap
            meta.setDisplayName("§cMax Level");
            lore.add("§7This item cannot be upgraded further.");
        } else {
            meta.setDisplayName("§aUpgrade");
            if (upgradeCost > 0) {
                lore.add("§7Cost: §6⛃ " + upgradeCost);
                lore.add("§7Success Chance: §6" + successChance + "%");
            } else {
                lore.add("§7Place an item in upgrade slot.");
            }
        }

        meta.setLore(lore);
        upgrade.setItemMeta(meta);
        return upgrade;
    }

    private ItemStack createRepairButton(int cost) {
        ItemStack repair = new ItemStack(Material.ANVIL);
        ItemMeta meta = repair.getItemMeta();
        meta.setDisplayName("§bRepair Item");
        List<String> lore = new ArrayList<>();
        if (cost > 0) {
            lore.add("§7Cost: §6⛃ " + cost);
        } else {
            lore.add("§7Place an item in the repair slot.");
        }
        meta.setLore(lore);
        repair.setItemMeta(meta);
        return repair;
    }

    private ItemStack createRerollButton(int cost) {
        ItemStack reroll = getNexoItem("check", ChatColor.GREEN + "Reroll Stat");
        ItemMeta meta = reroll.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            if (cost > 0) {
                lore.add("§7Cost: §6⛃ " + cost);
            } else {
                lore.add("§7Place item and placeholder.");
            }
            meta.setLore(lore);
            reroll.setItemMeta(meta);
        }
        return reroll;
    }

    private ItemStack createEvolveButton(boolean ready) {
        ItemStack evo = getNexoItem("check", ChatColor.GOLD + "Evolve Weapon");
        ItemMeta meta = evo.getItemMeta();
        if (meta != null) {
            meta.setLore(Collections.singletonList(
                ready ? ChatColor.YELLOW + "Click to evolve" : ChatColor.RED + "Requires Rank 10"));
            evo.setItemMeta(meta);
        }
        return evo;
    }

    private ItemStack createRepairAllButton(int totalCost) {
        ItemStack repairAll = new ItemStack(Material.GRINDSTONE);
        ItemMeta meta = repairAll.getItemMeta();
        meta.setDisplayName("§cRepair All Items");
        List<String> lore = new ArrayList<>();
        if (totalCost > 0) {
            lore.add("§7Total Cost: §6⛃ " + totalCost);
        } else {
            lore.add("§7No damaged items found.");
        }
        meta.setLore(lore);
        repairAll.setItemMeta(meta);
        return repairAll;
    }

    private int calculateTotalRepairCost(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            CustomItem ci = itemManager.getCustomItemFromItemStack(item);
            if (ci != null && ci.getCurrentDurability() < ci.getMaxDurability()) {
                total += repairManager.getRepairCost(ci);
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
        int clickedSlot = event.getSlot();

        String title = event.getView().getTitle();

// Allow placing item/placeholder in reroll slots
        if (title.equals(GUI_TITLE_REROLL) && (rawSlot == 11 || rawSlot == 15)) {
            event.setCancelled(false);
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                Inventory updatedGui = openInventories.get(player.getUniqueId());
                if (updatedGui != null) updateActionButton(player, updatedGui, title);
            }, 1L);
            return;
        }

        if (title.equals(GUI_TITLE_REROLL) && rawSlot == 13) {
            if (event.getCursor() == null || event.getCursor().getType().isAir()) {
                event.setCancelled(false); // allow taking result item
            } else {
                event.setCancelled(true); // prevent placing items here
            }
            return;
        }

// Allow placing into slot 13 only manually on upgrade/repair
        if (event.getRawSlot() == 13 && !title.equals(GUI_TITLE_REROLL)) {
            event.setCancelled(false);
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                Inventory updatedGui = openInventories.get(player.getUniqueId());
                if (updatedGui != null) updateActionButton(player, updatedGui, title);
            }, 1L);
            return;
        }


// Allow dragging items into appropriate slots
        if (event.getAction() == InventoryAction.PLACE_ALL ||
            event.getAction() == InventoryAction.PLACE_SOME ||
            event.getAction() == InventoryAction.PLACE_ONE ||
            event.getAction() == InventoryAction.SWAP_WITH_CURSOR ||
            event.getAction() == InventoryAction.HOTBAR_SWAP) {
            if (title.equals(GUI_TITLE_REROLL) && (event.getSlot() == 11 || event.getSlot() == 15)) {
                event.setCancelled(false);
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                    Inventory updatedGui = openInventories.get(player.getUniqueId());
                    if (updatedGui != null) updateActionButton(player, updatedGui, title);
                }, 1L);
                return;
            } else if (!title.equals(GUI_TITLE_REROLL) && event.getSlot() == 13) {
                event.setCancelled(false);
                return;
            }
        }

// Cancel all interactions outside the GUI
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                    Inventory updatedGui = openInventories.get(player.getUniqueId());
                    if (updatedGui != null) updateActionButton(player, updatedGui, title);
                }, 1L);
            }
            return; // allow interactions with the player's inventory
        }

        event.setCancelled(true);


        if (rawSlot == 9 || rawSlot == 17) {
            ItemStack carriedItem = title.equals(GUI_TITLE_REROLL) ? gui.getItem(11) : gui.getItem(13);
            ItemStack placeholder = title.equals(GUI_TITLE_REROLL) ? gui.getItem(15) : null;
            String newTitle;
            if (title.equals(GUI_TITLE_UPGRADE)) {
                if (rawSlot == 9) {
                    newTitle = GUI_TITLE_EVOLVE;
                    openEvolveGUI(player);
                } else {
                    newTitle = GUI_TITLE_REPAIR;
                    openRepairGUI(player);
                }
            } else if (title.equals(GUI_TITLE_REPAIR)) {
                if (rawSlot == 9) {
                    newTitle = GUI_TITLE_UPGRADE;
                    openUpgradeGUI(player);
                } else {
                    newTitle = GUI_TITLE_REROLL;
                    openRerollGUI(player);
                }
            } else if (title.equals(GUI_TITLE_REROLL)) {
                if (rawSlot == 9) {
                    newTitle = GUI_TITLE_REPAIR;
                    openRepairGUI(player);
                } else {
                    newTitle = GUI_TITLE_EVOLVE;
                    openEvolveGUI(player);
                }
            } else { // EVOLVE
                if (rawSlot == 9) {
                    newTitle = GUI_TITLE_REROLL;
                    openRerollGUI(player);
                } else {
                    newTitle = GUI_TITLE_UPGRADE;
                    openUpgradeGUI(player);
                }
            }

            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                Inventory newGui = openInventories.get(player.getUniqueId());
                if (newGui != null) {
                    if (newTitle.equals(GUI_TITLE_REROLL)) {
                        newGui.setItem(11, carriedItem);
                        newGui.setItem(15, placeholder);
                    } else {
                        newGui.setItem(13, carriedItem);
                        updateActionButton(player, newGui, newTitle);
                    }
                }
            }, 1L);
            return;
        }


        if (rawSlot == 0 && title.equals(GUI_TITLE_REPAIR)) {
            handleRepairAllClick(player);
            gui.setItem(0, createRepairAllButton(calculateTotalRepairCost(player)));
            return;
        }

        if (rawSlot == 22) {
            ItemStack item = title.equals(GUI_TITLE_REROLL) ? gui.getItem(11) : gui.getItem(13);
            if (item == null || item.getType().isAir()) return;
            CustomItem ci = itemManager.getCustomItemFromItemStack(item);
            if (ci == null) return;

            if (title.equals(GUI_TITLE_UPGRADE)) {
                if (ci.getUpgradeLevel() >= 5) {
                    player.sendMessage("§cItem has reached the maximum upgrade level.");
                    return;
                }
                int cost = upgradeManager.getUpgradeCost(ci);
                int chance = upgradeManager.getSuccessChance(ci);
                try {
                    economyManager.deductCoins(player, cost);
                } catch (IllegalArgumentException ex) {
                    player.sendMessage("§cNot enough coins! Upgrade cost: §6⛃ " + cost);
                    return;
                }
                if (upgradeManager.attemptUpgrade(player, item, ci)) {
                    player.sendMessage("§aUpgrade successful!");
                    Main.getInstance().getQuestManager().handleUpgrade(player, String.valueOf(ci.getId()));
                    gui.setItem(13, item);
                } else {
                    player.sendMessage("§cUpgrade failed!");
                }
                if (ci.getUpgradeLevel() >= 5) {
                    gui.setItem(22, createUpgradeButton(-1, 0));
                } else {
                    gui.setItem(22, createUpgradeButton(
                            upgradeManager.getUpgradeCost(ci),
                            upgradeManager.getSuccessChance(ci)));
                }
            } else if (title.equals(GUI_TITLE_REPAIR)) {
                int cost = repairManager.getRepairCost(ci);
                try {
                    economyManager.deductCoins(player, cost);
                } catch (IllegalArgumentException ex) {
                    player.sendMessage("§cNot enough coins to repair! Cost: §6⛃ " + cost);
                    return;
                }
                if (repairManager.repairItem(player, item, ci)) {
                    player.sendMessage("§aItem repaired!");
                    gui.setItem(13, item);
                    ItemUtil.updateTooltip(item, player);
                    Main.getInstance().getQuestManager().handleRepair(player, String.valueOf(ci.getId()));
                }
                gui.setItem(22, createRepairButton(0));
            } else if (title.equals(GUI_TITLE_REROLL)) {
                ItemStack placeholder = gui.getItem(15);
                if (placeholder == null || placeholder.getType().isAir()) {
                    player.sendMessage("§cPlace a stat placeholder on the right.");
                    return;
                }
                StatType stat = materialToStat(placeholder.getType());
                if (stat == null) {
                    player.sendMessage("§cInvalid placeholder item!");
                    return;
                }

                if (!rerollManager.hasStat(ci, stat)) {
                    player.sendMessage("§cThis item does not have " + statDisplayName(stat) + "!");
                    return;
                }

                int cost = rerollManager.getRerollCost(ci);
                try {
                    economyManager.deductCoins(player, cost);
                } catch (IllegalArgumentException ex) {
                    player.sendMessage("§cNot enough coins to reroll! Cost: §6⛃" + cost);
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
                player.sendMessage(message);
            } else if (title.equals(GUI_TITLE_EVOLVE)) {
                EgoWeaponManager manager = EgoWeaponManager.getInstance();
                if (manager.evolveWeapon(player, item)) {
                    gui.setItem(13, item);
                }
                gui.setItem(22, createEvolveButton(
                    item.getItemMeta().getPersistentDataContainer()
                        .getOrDefault(ItemUtil.EGO_RANK_KEY, PersistentDataType.INTEGER,1) >= 10));
            }
        }

        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            updateActionButton(player, gui, title);
        }, 1L);
    }

    private void handleRepairAllClick(Player player) {
        int totalCost = 0;
        List<ItemStack> toRepair = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            CustomItem ci = itemManager.getCustomItemFromItemStack(item);
            if (ci != null && ci.getCurrentDurability() < ci.getMaxDurability()) {
                totalCost += repairManager.getRepairCost(ci);
                toRepair.add(item);
            }
        }
        if (totalCost == 0) {
            player.sendMessage("§7No damaged items found.");
            return;
        }
        try {
            economyManager.deductCoins(player, totalCost);
        } catch (IllegalArgumentException ex) {
            player.sendMessage("§cYou need §6⛃ " + totalCost + " §cto repair all items.");
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
        player.sendMessage("§aAll items repaired! Total cost: §6⛃ " + totalCost);
    }

    private void updateActionButton(Player player, Inventory gui, String title) {
        ItemStack current = title.equals(GUI_TITLE_REROLL) ? gui.getItem(11) : gui.getItem(13);
        if (current == null || current.getType().isAir()) {
            if (title.equals(GUI_TITLE_REROLL)) {
                gui.setItem(22, createRerollButton(0));
            } else if (title.equals(GUI_TITLE_EVOLVE)) {
                gui.setItem(22, createEvolveButton(false));
            } else {
                gui.setItem(22, title.equals(GUI_TITLE_UPGRADE)
                    ? createUpgradeButton(0, 0)
                    : createRepairButton(0));
            }
            return;
        }

        CustomItem ci = itemManager.getCustomItemFromItemStack(current);
        if (ci == null) {
            if (title.equals(GUI_TITLE_REROLL)) {
                gui.setItem(22, createRerollButton(0));
            } else if (title.equals(GUI_TITLE_EVOLVE)) {
                gui.setItem(22, createEvolveButton(false));
            } else {
                gui.setItem(22, title.equals(GUI_TITLE_UPGRADE)
                    ? createUpgradeButton(0, 0)
                    : createRepairButton(0));
            }
            return;
        }

        if (title.equals(GUI_TITLE_UPGRADE)) {
            if (ci.getUpgradeLevel() >= 5) {
                gui.setItem(22, createUpgradeButton(-1, 0));
            } else {
                gui.setItem(22, createUpgradeButton(
                    upgradeManager.getUpgradeCost(ci),
                    upgradeManager.getSuccessChance(ci)));
            }
        } else if (title.equals(GUI_TITLE_REPAIR)) {
            gui.setItem(22, createRepairButton(repairManager.getRepairCost(ci)));
        } else if (title.equals(GUI_TITLE_REROLL)) {
            gui.setItem(22, createRerollButton(rerollManager.getRerollCost(ci)));
        } else {
            ItemMeta meta = current.getItemMeta();
            int rank = 1;
            if (meta != null) {
                rank = meta.getPersistentDataContainer()
                        .getOrDefault(ItemUtil.EGO_RANK_KEY, PersistentDataType.INTEGER, 1);
            }
            gui.setItem(22, createEvolveButton(rank >= 10));
        }
    }

    private StatType materialToStat(Material mat) {
        return switch (mat) {
            case BORDURE_INDENTED_BANNER_PATTERN -> StatType.STR;
            case FLOWER_BANNER_PATTERN -> StatType.INT;
            case FLOW_BANNER_PATTERN -> StatType.AGI;
            case SKULL_BANNER_PATTERN -> StatType.HP;
            case GUSTER_BANNER_PATTERN -> StatType.DEX;
            case GLOBE_BANNER_PATTERN -> StatType.DEF;
            default -> null;
        };
    }

    private String statDisplayName(StatType stat) {
        return switch (stat) {
            case STR -> "Strength";
            case INT -> "Intelligence";
            case AGI -> "Agility";
            case DEX -> "Dexterity";
            case HP  -> "Health";
            case DEF -> "Defence";
        };
    }


    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        Inventory gui = openInventories.get(player.getUniqueId());
        if (gui == null || !event.getInventory().equals(gui)) return;
        String title = event.getView().getTitle();
        if (title.equals(GUI_TITLE_REROLL)) {
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
    }
}
