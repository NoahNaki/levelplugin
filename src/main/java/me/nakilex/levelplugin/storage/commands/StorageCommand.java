package me.nakilex.levelplugin.storage.commands;

import me.nakilex.levelplugin.storage.StorageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StorageCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage("Usage: /ps <create|open>");
            return true;
        }

        StorageManager storageManager = StorageManager.getInstance();

        switch (args[0].toLowerCase()) {
            case "create":
                storageManager.createStorage(player);
                break;

            case "open":
                storageManager.openStorage(player);
                break;

            default:
                player.sendMessage("Unknown command. Use /ps <create|open>");
                break;
        }

        return true;
    }
}
