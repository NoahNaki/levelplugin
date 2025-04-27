package me.nakilex.levelplugin.economy.commands;

import me.nakilex.levelplugin.economy.managers.GemsManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GemsBalanceCommand implements CommandExecutor {
    private final GemsManager gemsManager;

    public GemsBalanceCommand(GemsManager gemsManager) {
        this.gemsManager = gemsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only!");
            return true;
        }
        Player p = (Player) sender;
        int bal = gemsManager.getTotalUnits(p);
        p.sendMessage("Your gems: " + bal);
        return true;
    }
}
