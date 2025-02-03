package me.nakilex.levelplugin.storage.commands;

import me.nakilex.levelplugin.storage.StorageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StorageCommand implements CommandExecutor {

    private final StorageManager storageManager;

    public StorageCommand(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        // If no arguments, show basic usage
        if (args.length == 0) {
            player.sendMessage("Usage: /ps <create|open>");
            return true;
        }

        // Determine which subcommand was used
        switch (args[0].toLowerCase()) {
            case "create":
                if (storageManager.hasStorage(player.getUniqueId())) {
                    player.sendMessage("You already have a personal storage!");
                } else {
                    storageManager.createStorage(player.getUniqueId());
                    player.sendMessage("Your personal storage has been created.");
                }
                break;

            case "open":
                if (storageManager.hasStorage(player.getUniqueId())) {
                    storageManager.openStorage(player);
                } else {
                    player.sendMessage("You don't have a storage yet. Use /ps create first.");
                }
                break;

            default:
                player.sendMessage("Unknown subcommand. Try /ps <create|open>.");
                break;
        }

        return true;
    }
}
