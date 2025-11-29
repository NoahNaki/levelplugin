package me.nakilex.levelplugin.merchants.commands;

import me.nakilex.levelplugin.merchants.gui.MerchantGUI;
import me.nakilex.levelplugin.merchants.gui.PotionMerchantGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;

import me.nakilex.levelplugin.utils.CommandUtil;

import java.io.File;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

public class MerchantCommand implements TabExecutor {
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
        if ("potion_merchant".equalsIgnoreCase(merchantName)) {
            PotionMerchantGUI potionGUI = new PotionMerchantGUI(plugin, merchantConfig);
            ((Player) sender).openInventory(potionGUI.getInventory());
            return true;
        }
        // Create and open the merchant GUI
        MerchantGUI merchantGUI = new MerchantGUI(plugin, merchantConfig, merchantName);
        ((Player) sender).openInventory(merchantGUI.getInventory());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            var section = merchantConfig.getConfigurationSection("merchants");
            if (section == null) {
                return Collections.emptyList();
            }
            return CommandUtil.filterStartingWith(section.getKeys(false), args[0]);
        }
        return Collections.emptyList();
    }
}
