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
                player.sendMessage(ChatColor.RED + "Usage: /auctionhouse sell <start> [bin] [hours]");
                return true;
            }
            int start;
            int bin = 0;
            long hours = 6;
            try {
                start = Integer.parseInt(args[1]);
                if (args.length > 2) bin = Integer.parseInt(args[2]);
                if (args.length > 3) hours = Long.parseLong(args[3]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid number.");
                return true;
            }
            if (hours > AuctionHouseManager.MAX_DURATION_HOURS) {
                hours = AuctionHouseManager.MAX_DURATION_HOURS;
                player.sendMessage(ChatColor.YELLOW + "Duration capped at " + AuctionHouseManager.MAX_DURATION_HOURS + "h.");
            }
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType().isAir()) {
                player.sendMessage(ChatColor.RED + "Hold the item you wish to sell in your hand.");
                return true;
            }
            if (me.nakilex.levelplugin.items.listeners.StaticItemListener.isStaticItem(item)) {
                player.sendMessage(ChatColor.RED + "You cannot list that item.");
                return true;
            }
            player.getInventory().setItemInMainHand(null);
            if (!manager.listItem(player, item, start, bin, hours)) {
                player.getInventory().setItemInMainHand(item);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("bid")) {
            if (args.length < 3) {
                player.sendMessage(ChatColor.RED + "Usage: /auctionhouse bid <index> <amount>");
                return true;
            }
            try {
                int index = Integer.parseInt(args[1]);
                int amount = Integer.parseInt(args[2]);
                if (manager.bid(player, index, amount)) {
                    player.sendMessage(ChatColor.GREEN + "Bid placed.");
                }
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid number.");
            }
            return true;
        }

        player.sendMessage(ChatColor.RED + "Usage: /auctionhouse [open|sell|bid]");
        return true;
    }
}
