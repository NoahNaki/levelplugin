package me.nakilex.levelplugin.economy.commands;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BalanceCommand implements CommandExecutor {

    private EconomyManager economy;

    public BalanceCommand(EconomyManager economy) {
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(!(sender instanceof Player)) {
            sender.sendMessage("Players only!");
            return true;
        }
        Player player = (Player)sender;
        int balance = economy.getBalance(player);
        player.sendMessage("Your balance: " + balance + " coins.");
        return true;
    }
}
