package me.nakilex.levelplugin.commands;

import me.nakilex.levelplugin.managers.HorseManager;
import me.nakilex.levelplugin.ui.HorseGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HorseCommand implements CommandExecutor {

    private final HorseManager horseManager;
    private final HorseGUI horseGUI;

    public HorseCommand(HorseManager horseManager, HorseGUI horseGUI) {
        this.horseManager = horseManager;
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
            player.sendMessage("Usage: /horse [spawn|reroll]");
            return true;
        }

        // Handle 'spawn' command
        if (args[0].equalsIgnoreCase("spawn")) {
            horseManager.spawnHorse(player);
            return true;
        }

        // Handle 'reroll' command
        if (args[0].equalsIgnoreCase("reroll")) {
            // Dismount any existing horse before rerolling
            horseManager.dismountHorse(player);
            horseGUI.openHorseMenu(player); // Open GUI after dismount
            return true;
        }

        player.sendMessage("Usage: /horse [spawn|reroll]");
        return true;
    }
}
