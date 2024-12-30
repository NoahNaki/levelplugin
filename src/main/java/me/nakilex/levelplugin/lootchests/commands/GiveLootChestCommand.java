package me.nakilex.levelplugin.lootchests.commands;

import me.nakilex.levelplugin.lootchests.config.ConfigManager;
import me.nakilex.levelplugin.lootchests.data.ChestData;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class GiveLootChestCommand implements CommandExecutor {

    private final ConfigManager configManager;
    private final LootChestManager lootChestManager;

    public GiveLootChestCommand(ConfigManager configManager, LootChestManager lootChestManager) {
        this.configManager = configManager;
        this.lootChestManager = lootChestManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }
        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " <tier>");
            return true;
        }

        int tier;
        try {
            tier = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Tier must be a number (1-4).");
            return true;
        }

        if (tier < 1 || tier > 4) {
            player.sendMessage(ChatColor.RED + "Tier must be between 1 and 4.");
            return true;
        }

        // We'll take player's current location and store just the coordinates
        Location loc = player.getLocation();
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();

        // Generate a new chest ID
        int newId = getNextChestId();

        // Write to lootchests.yml (just coordinates/tier)
        FileConfiguration lootConfig = configManager.getLootChestsConfig();
        String path = "loot_chests." + newId;
        lootConfig.set(path + ".coordinates", x + ", " + y + ", " + z);
        lootConfig.set(path + ".tier", tier);
        configManager.saveLootChestsConfig();

        // Create ChestData and add to memory
        ChestData newChest = new ChestData(newId, x, y, z, tier);
        lootChestManager.addChestData(newChest);

        // Spawn instantly
        lootChestManager.spawnChest(newChest);

        player.sendMessage(ChatColor.GREEN + "Spawned a Tier " + tier + " chest at your location with ID " + newId + ".");
        return true;
    }

    /**
     * Finds the largest chest ID in config and returns one greater.
     */
    private int getNextChestId() {
        FileConfiguration lootConfig = configManager.getLootChestsConfig();
        if (!lootConfig.contains("loot_chests")) {
            return 1;
        }
        int maxId = 0;
        for (String key : lootConfig.getConfigurationSection("loot_chests").getKeys(false)) {
            try {
                int id = Integer.parseInt(key);
                if (id > maxId) {
                    maxId = id;
                }
            } catch (NumberFormatException ignore) {}
        }
        return maxId + 1;
    }
}
