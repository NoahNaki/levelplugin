package me.nakilex.levelplugin.fishing.core.command;

import me.nakilex.levelplugin.fishing.api.FishingMechanism;
import me.nakilex.levelplugin.fishing.core.CustomFishingPlugin;
import me.nakilex.levelplugin.utils.ChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public class CustomFishingCommand implements CommandExecutor {
    private final CustomFishingPlugin fishing;

    public CustomFishingCommand(CustomFishingPlugin fishing) {
        this.fishing = fishing;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /customfishing <reload|debug|simulate>");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                if (!sender.hasPermission("customfishing.reload")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }
                fishing.reload();
                sender.sendMessage(ChatColor.GREEN + "Custom fishing reloaded.");
            }
            case "debug" -> {
                if (!sender.hasPermission("customfishing.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /customfishing debug <on|off>");
                    return true;
                }
                boolean enabled = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true");
                fishing.getConfigManager().setDebug(enabled);
                sender.sendMessage(ChatColor.GREEN + "Custom fishing debug set to " + enabled + ".");
            }
            case "simulate" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can simulate fishing.");
                    return true;
                }
                if (!sender.hasPermission("customfishing.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /customfishing simulate <water|lava|void>");
                    return true;
                }
                FishingMechanism mechanism;
                try {
                    mechanism = FishingMechanism.valueOf(args[1].toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage(ChatColor.RED + "Unknown mechanism: " + args[1]);
                    return true;
                }
                fishing.simulate(player, mechanism);
                sender.sendMessage(ChatColor.GREEN + ChatUtil.applyEmojis("Simulating " + mechanism.name().toLowerCase(Locale.ROOT) + " fishing."));
            }
            default -> sender.sendMessage(ChatColor.YELLOW + "Usage: /customfishing <reload|debug|simulate>");
        }
        return true;
    }
}
