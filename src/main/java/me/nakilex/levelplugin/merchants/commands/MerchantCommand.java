package me.nakilex.levelplugin.merchants.commands;

import me.nakilex.levelplugin.merchants.gui.MerchantGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

public class MerchantCommand implements CommandExecutor {
    private final Plugin plugin;
    private final FileConfiguration merchantConfig;

    public MerchantCommand(Plugin plugin) {
        this.plugin = plugin;
        // Load or create the merchants.yml file
        File file = new File(plugin.getDataFolder(), "merchants.yml");
        if (!file.exists()) {
            plugin.saveResource("merchants.yml", false);
        }
        this.merchantConfig = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            send(sender, MessageType.ERROR, "Only players can use this command.");
            return true;
        }
        if (args.length < 1) {
            send(sender, MessageType.ERROR, "Usage: /merchant <name>");
            return true;
        }
        String merchantName = args[0];
        if (!merchantConfig.contains("merchants." + merchantName)) {
            send(sender, MessageType.ERROR, "Merchant not found!");
            return true;
        }
        // Create and open the merchant GUI
        MerchantGUI merchantGUI = new MerchantGUI(plugin, merchantConfig, merchantName);
        ((Player) sender).openInventory(merchantGUI.getInventory());
        return true;
    }
}
