package me.nakilex.levelplugin.auction.commands;

import me.nakilex.levelplugin.auction.AuctionItem;
import me.nakilex.levelplugin.auction.gui.AuctionHouseGUI;
import me.nakilex.levelplugin.auction.managers.AuctionManager;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class AuctionHouseCommand implements CommandExecutor {
    private final AuctionManager manager;
    private final AuctionHouseGUI gui;
    private final EconomyManager economy;

    public AuctionHouseCommand(AuctionManager manager, AuctionHouseGUI gui, EconomyManager economy) {
        this.manager = manager;
        this.gui = gui;
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only!");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0 || args[0].equalsIgnoreCase("browse")) {
            gui.open(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("sell")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /ah sell <price>");
                return true;
            }
            int price;
            try {
                price = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                player.sendMessage(ChatColor.RED + "Invalid price: " + args[1]);
                return true;
            }
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) {
                player.sendMessage(ChatColor.RED + "Hold an item to sell.");
                return true;
            }
            ItemStack toSell = hand.clone();
            player.getInventory().setItemInMainHand(null);
            AuctionItem item = new AuctionItem(UUID.randomUUID(), player.getUniqueId(), player.getName(), toSell, price, System.currentTimeMillis());
            manager.addAuction(item);
            player.sendMessage(ChatColor.GREEN + "Listed item for " + price + " coins.");
            gui.refresh();
            return true;
        }
        return false;
    }
}
