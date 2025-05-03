package me.nakilex.levelplugin.lootchests.commands;

import me.nakilex.levelplugin.lootchests.config.ConfigManager;
import me.nakilex.levelplugin.lootchests.data.ChestData;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class LootChestCommand implements CommandExecutor {

    private final ConfigManager configManager;
    private final LootChestManager lootChestManager;


    public LootChestCommand(ConfigManager configManager, LootChestManager lootChestManager) {
        this.configManager = configManager;
        this.lootChestManager = lootChestManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender);
                return true;

            case "list":
                handleList(sender);
                return true;

            case "clear":
                handleClear(sender, args);
                return true;

            default:
                sendHelp(sender);
                return true;
        }
    }

    /**
     * Reload the lootchests.yml config file from disk.
     */
    private void handleReload(CommandSender sender) {
        configManager.reloadLootChestsConfig();
        sender.sendMessage(ChatColor.GREEN + "Loot chest configuration reloaded.");

        // If you also want to re-load or respawn chests in memory,
        // you could do something like:
        // lootChestManager.reloadChestsFromConfig();
        // (But that method must be written in LootChestManager.)
    }

    /**
     * List all loaded chest data (ID, Tier, Coordinates).
     * We assume they all spawn in the same world (rpgworld).
     */
    private void handleList(CommandSender sender) {
        Iterable<ChestData> allChests = lootChestManager.getAllChestData();
        if (!allChests.iterator().hasNext()) {
            sender.sendMessage(ChatColor.YELLOW + "No loot chests found.");
            return;
        }

        sender.sendMessage(ChatColor.GREEN + "Loaded Loot Chests (world: rpgworld):");
        for (ChestData chestData : allChests) {
            sender.sendMessage(ChatColor.DARK_GREEN + "- Chest ID: " + chestData.getChestId()
                + ", Tier: " + chestData.getTier()
                + ", Coordinates: (" + chestData.getX() + ", " + chestData.getY() + ", " + chestData.getZ() + ")");
        }
    }

    private void handleClear(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /lootchest clear <id|all>");
            return;
        }

        if (args[1].equalsIgnoreCase("all")) {
            boolean any = false;
            for (ChestData data : lootChestManager.getAllChestData()) {
                int id = data.getChestId();
                if (lootChestManager.removeChest(id)) {
                    any = true;
                }
            }
            if (any) {
                sender.sendMessage(ChatColor.GREEN + "Removed all loot chests.");
            } else {
                sender.sendMessage(ChatColor.RED + "No spawned loot chests to remove.");
            }
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "'" + args[1] + "' is not a valid chest ID.");
            return;
        }

        boolean removed = lootChestManager.removeChest(id);
        if (removed) {
            sender.sendMessage(ChatColor.GREEN + "Removed loot chest with ID " + id + ".");
        } else {
            sender.sendMessage(ChatColor.RED + "No spawned loot chest found with ID " + id + ".");
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "===== LootChest Command Help =====");
        sender.sendMessage(ChatColor.YELLOW + "/lootchest reload         - Reload the loot chest configuration.");
        sender.sendMessage(ChatColor.YELLOW + "/lootchest list           - List all loaded loot chests.");
        sender.sendMessage(ChatColor.YELLOW + "/lootchest clear <id>     - Clear the contents of the chest with the given ID.");
        sender.sendMessage(ChatColor.YELLOW + "/lootchest clear all      - Clear the contents of *all* loot chests.");
    }
}
