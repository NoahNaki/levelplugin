package me.nakilex.levelplugin.economy.commands;

import me.nakilex.levelplugin.economy.managers.GemsManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AddGemsCommand implements CommandExecutor {
    private final GemsManager gemsManager;

    public AddGemsCommand(GemsManager gemsManager) {
        this.gemsManager = gemsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only!");
            return true;
        }
        Player p = (Player) sender;
        if (args.length < 1) {
            p.sendMessage("Usage: /addgems <amount>");
            return true;
        }
        int amt;
        try {
            amt = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            p.sendMessage("Invalid amount: " + args[0]);
            return true;
        }
        gemsManager.addUnits(p, amt);
        p.sendMessage("Added " + amt + " gems!");
        return true;
    }
}
