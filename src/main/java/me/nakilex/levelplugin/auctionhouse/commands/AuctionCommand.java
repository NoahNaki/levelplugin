package me.nakilex.levelplugin.auctionhouse.commands;

import me.nakilex.levelplugin.auctionhouse.AuctionHouseManager;
import me.nakilex.levelplugin.auctionhouse.gui.AuctionGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AuctionCommand implements CommandExecutor {
    private final AuctionGUI gui;

    public AuctionCommand(AuctionHouseManager manager) {
        this.gui = new AuctionGUI(manager);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        gui.open(player, 0);
        return true;
    }
}
