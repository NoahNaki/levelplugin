package me.nakilex.levelplugin.economy.commands;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class AddCoinsCommand implements CommandExecutor {

    private EconomyManager economy;

    public AddCoinsCommand(EconomyManager economy) {
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)) {
            sender.sendMessage("Players only!");
            return true;
        }
        Player player = (Player)sender;
        if(args.length < 1) {
            player.sendMessage("Usage: /addcoins <amount>");
            return true;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[0]);
        } catch(NumberFormatException e) {
            player.sendMessage("Invalid amount: " + args[0]);
            return true;
        }
        economy.addCoins(player, amount);
        player.sendMessage("Added " + amount + " coins to your balance!");
        return true;
    }
}
