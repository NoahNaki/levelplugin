package me.nakilex.levelplugin.environment;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

public class TownCommand implements CommandExecutor {
    private final UpgradeGUI gui;
    private final EnvironmentManager manager;

    public TownCommand(UpgradeGUI gui, EnvironmentManager manager) {
        this.gui = gui;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("start")) {
            if (args.length < 2) {
                p.sendMessage(ChatColor.RED + "Usage: /town start <name>");
                return true;
            }
            manager.startTown(p, args[1].toLowerCase());
            return true;
        }
        gui.open(p);
        return true;
    }
}
