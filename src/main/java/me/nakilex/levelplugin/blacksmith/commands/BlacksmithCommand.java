package me.nakilex.levelplugin.blacksmith.commands;

import me.nakilex.levelplugin.blacksmith.gui.BlacksmithGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BlacksmithCommand implements CommandExecutor {

    private final BlacksmithGUI blacksmithGUI;

    public BlacksmithCommand(BlacksmithGUI blacksmithGUI) {
        this.blacksmithGUI = blacksmithGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        // Open the Blacksmith GUI for the player
        blacksmithGUI.open(player);
        //player.sendMessage("§aBlacksmith GUI opened!");

        return true;
    }
}
