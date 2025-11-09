package me.nakilex.levelplugin.horse.commands;

import me.nakilex.levelplugin.horse.managers.HorseManager;
import me.nakilex.levelplugin.horse.gui.HorseGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

import me.nakilex.levelplugin.utils.CommandUtil;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

public class HorseCommand implements TabExecutor {

    private final HorseManager horseManager;
    private final HorseGUI horseGUI;

    public HorseCommand(HorseManager horseManager, HorseGUI horseGUI) {
        this.horseManager = horseManager;
        this.horseGUI = horseGUI;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            send(sender, MessageType.ERROR, "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            send(player, MessageType.INFO, "Usage: /horse [spawn|reroll]");
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

        send(player, MessageType.INFO, "Usage: /horse [spawn|reroll]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtil.simpleSuggestions(args[0], "spawn", "reroll");
        }
        return Collections.emptyList();
    }
}
