package me.nakilex.levelplugin.player.farming.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class FarmingRewardsGUI implements Listener, CommandExecutor {

    private static final String TITLE = "Farming Rewards";
    private final EconomyManager economyManager;
    private final Main plugin;

    public FarmingRewardsGUI(Main plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        plugin.getCommand("farmrewards").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private ItemStack tradeItem(String name, int wheatCost, String rewardText) {
        ItemStack stack = new ItemStack(Material.WHEAT);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(
                    "§7Cost: §f" + wheatCost + " Wheat",
                    "§7Rewards: §e" + rewardText,
                    "",
                    "§eClick to trade"
            ));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        inv.setItem(11, tradeItem("§eFarmhands' Stipend", 16, "+75 Coins"));
        inv.setItem(13, tradeItem("§eSeed Fund", 32, "+150 Coins & 4 Seeds"));
        inv.setItem(15, tradeItem("§eBarn Booster", 64, "+350 Coins & 1 Bone Meal"));
        player.openInventory(inv);
    }

    private int countWheat(Player player) {
        return Arrays.stream(player.getInventory().getContents())
                .filter(it -> it != null && it.getType() == Material.WHEAT)
                .mapToInt(ItemStack::getAmount)
                .sum();
    }

    private boolean takeWheat(Player player, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack != null && stack.getType() == Material.WHEAT) {
                if (stack.getAmount() <= remaining) {
                    remaining -= stack.getAmount();
                    contents[i] = null;
                } else {
                    stack.setAmount(stack.getAmount() - remaining);
                    remaining = 0;
                }
            }
        }
        player.getInventory().setContents(contents);
        return remaining <= 0;
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
        if (!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.WHEAT) return;

        int slot = event.getRawSlot();
        int cost;
        Runnable reward;
        if (slot == 11) {
            cost = 16;
            reward = () -> economyManager.addCoins(player, 75, false);
        } else if (slot == 13) {
            cost = 32;
            reward = () -> {
                economyManager.addCoins(player, 150, false);
                player.getInventory().addItem(new ItemStack(Material.WHEAT_SEEDS, 4));
            };
        } else if (slot == 15) {
            cost = 64;
            reward = () -> {
                economyManager.addCoins(player, 350, false);
                player.getInventory().addItem(new ItemStack(Material.BONE_MEAL, 1));
            };
        } else {
            return;
        }

        int owned = countWheat(player);
        if (owned < cost) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "You need " + cost + " wheat to purchase this reward.");
            return;
        }

        if (takeWheat(player, cost)) {
            reward.run();
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Trade complete! Enjoy your rewards.");
            player.updateInventory();
        }
    }
}
