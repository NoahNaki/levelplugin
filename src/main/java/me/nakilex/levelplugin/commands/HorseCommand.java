package me.nakilex.levelplugin.commands;

import me.nakilex.levelplugin.ui.HorseGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HorseCommand implements CommandExecutor {

    private final HorseGUI horseGUI;

    public HorseCommand(HorseGUI horseGUI) {
        this.horseGUI = horseGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Open the horse GUI
            horseGUI.openHorseMenu(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("reroll")) {
            // Handle reroll command directly
            horseGUI.handleSaddleClick(null); // Simulate click for direct reroll
            return true;
        }

        player.sendMessage("Usage: /horse [reroll]");
        return true;
    }
}
