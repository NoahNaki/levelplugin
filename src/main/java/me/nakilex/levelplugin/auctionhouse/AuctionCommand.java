package me.nakilex.levelplugin.auctionhouse;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AuctionCommand implements CommandExecutor {

    private final AuctionHouseManager manager;
    private final AuctionHouseGUI gui;

    public AuctionCommand(AuctionHouseManager manager, AuctionHouseGUI gui) {
        this.manager = manager;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players may use this command.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("open")) {
            gui.open(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("sell")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /auctionhouse sell <price>");
                return true;
            }
            int price;
            try {
                price = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid price.");
                return true;
            }
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType().isAir()) {
                player.sendMessage(ChatColor.RED + "Hold the item you wish to sell in your hand.");
                return true;
            }
            player.getInventory().setItemInMainHand(null);
            manager.listItem(player, item, price);
            player.sendMessage(ChatColor.GREEN + "Item listed for " + price + " coins.");
            return true;
        }

        player.sendMessage(ChatColor.RED + "Usage: /auctionhouse [open|sell <price>]");
        return true;
    }
}
