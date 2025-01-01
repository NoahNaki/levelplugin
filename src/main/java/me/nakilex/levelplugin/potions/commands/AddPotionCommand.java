package me.nakilex.levelplugin.potions.commands;

import me.nakilex.levelplugin.potions.managers.PotionManager;
import me.nakilex.levelplugin.potions.data.PotionInstance;
import me.nakilex.levelplugin.potions.data.PotionTemplate;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;

public class AddPotionCommand implements TabExecutor {

    private final PotionManager potionManager;
    private final JavaPlugin plugin;

    public AddPotionCommand(PotionManager potionManager, JavaPlugin plugin) {
        this.potionManager = potionManager;
        this.plugin = plugin;
    }



    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("Usage: /addpotion <player> <template_id>");
            return true;
        }

        Player player = Bukkit.getPlayer(args[0]);
        String templateId = args[1];

        if (player == null) {
            sender.sendMessage("Player not found.");
            return true;
        }

        PotionTemplate template = potionManager.getTemplate(templateId);
        if (template == null) {
            sender.sendMessage("Potion ID not found: " + templateId);
            return true;
        }

        PotionInstance instance = potionManager.createInstance(template);
        player.getInventory().addItem(instance.toItemStack(plugin));
        sender.sendMessage("Potion added to " + player.getName());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
