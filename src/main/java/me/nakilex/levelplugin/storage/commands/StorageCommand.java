package me.nakilex.levelplugin.storage.commands;

import me.nakilex.levelplugin.storage.StorageManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import org.bukkit.ChatColor;
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
            ChatMessageUtil.send(player, MessageType.INFO, "Usage: /ps <create|open|reload|info>");
            return true;
        }

        // Determine which subcommand was used
        switch (args[0].toLowerCase()) {
            case "create":
                if (storageManager.hasStorage(player.getUniqueId())) {
                    ChatMessageUtil.send(player, MessageType.INFO, "You already have a personal storage!");
                } else {
                    storageManager.createStorage(player.getUniqueId());
                    ChatMessageUtil.send(player, MessageType.SUCCESS, "Your personal storage has been created.");
                }
                break;

            case "open":
                if (storageManager.hasStorage(player.getUniqueId())) {
                    storageManager.openStorage(player);
                } else {
                    ChatMessageUtil.send(player, MessageType.ERROR,
                            "You don't have a storage yet. Speak to a Storage Manager to register one.");
                }
                break;
            case "reload":
                if (!storageManager.hasStorage(player.getUniqueId())) {
                    ChatMessageUtil.send(player, MessageType.ERROR, "You don't have a storage yet.");
                    break;
                }
                storageManager.getStorage(player.getUniqueId()).load();
                ChatMessageUtil.send(player, MessageType.SUCCESS, "Storage reloaded from disk.");
                storageManager.openStorage(player);
                break;
            case "info":
                boolean hasStorage = storageManager.hasStorage(player.getUniqueId());
                ChatMessageUtil.send(player, MessageType.INFO,
                        "Personal Storage: " + (hasStorage ? ChatColor.GREEN + "Registered" : ChatColor.RED + "Not registered"));
                if (hasStorage) {
                    ChatMessageUtil.send(player, MessageType.INFO, "Use /ps open to access and /ps reload to refresh.");
                }
                break;

            default:
                ChatMessageUtil.send(player, MessageType.ERROR, "Unknown subcommand. Try /ps <create|open|reload|info>.");
                break;
        }

        return true;
    }
}
