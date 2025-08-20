package me.nakilex.levelplugin.auctionhouse;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;

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
            sender.sendMessage(ChatMessageUtil.format(MessageType.ERROR,
                    "Only players may use this command."));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("open")) {
            gui.open(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("sell")) {
            if (args.length < 2) {
                ChatMessageUtil.send(player, MessageType.ERROR,
                        "Usage: /auctionhouse sell <start> [bin] [hours]");
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
                ChatMessageUtil.send(player, MessageType.ERROR, "Invalid number.");
                return true;
            }
            if (hours > AuctionHouseManager.MAX_DURATION_HOURS) {
                hours = AuctionHouseManager.MAX_DURATION_HOURS;
                ChatMessageUtil.send(player, MessageType.WARNING,
                        "Duration capped at " + AuctionHouseManager.MAX_DURATION_HOURS + "h.");
            }
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType().isAir()) {
                ChatMessageUtil.send(player, MessageType.ERROR,
                        "Hold the item you wish to sell in your hand.");
                return true;
            }
            if (me.nakilex.levelplugin.items.listeners.StaticItemListener.isStaticItem(item)) {
                ChatMessageUtil.send(player, MessageType.ERROR, "You cannot list that item.");
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
                ChatMessageUtil.send(player, MessageType.ERROR,
                        "Usage: /auctionhouse bid <index> <amount>");
                return true;
            }
            try {
                int index = Integer.parseInt(args[1]);
                int amount = Integer.parseInt(args[2]);
                if (manager.bid(player, index, amount)) {
                    ChatMessageUtil.send(player, MessageType.SUCCESS, "Bid placed.");
                }
            } catch (NumberFormatException e) {
                ChatMessageUtil.send(player, MessageType.ERROR, "Invalid number.");
            }
            return true;
        }

        ChatMessageUtil.send(player, MessageType.ERROR, "Usage: /auctionhouse [open|sell|bid]");
        return true;
    }
}
