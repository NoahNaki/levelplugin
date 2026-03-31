package me.nakilex.levelplugin.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Simple /roll command that sends the result to the player's active chat channel.
 */
public class RollCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        int maxRoll = 100;
        if (args.length >= 1) {
            try {
                maxRoll = Math.max(2, Math.min(1000, Integer.parseInt(args[0])));
            } catch (NumberFormatException ignored) {
                player.sendMessage(ChatColor.RED + "Usage: /roll [max 2-1000]");
                return true;
            }
        }
        int roll = ThreadLocalRandom.current().nextInt(1, maxRoll + 1);
        NamedTextColor rollColor = roll == maxRoll
                ? NamedTextColor.GREEN
                : (roll == 1 ? NamedTextColor.RED : NamedTextColor.YELLOW);
        Component msg = Component.text()
                .append(player.displayName())
                .append(Component.text(" rolled " + roll + " (1-" + maxRoll + ")", rollColor))
                .build();
        ChatManager.sendChannelMessage(player, msg);
        if (roll == maxRoll) {
            player.sendMessage(ChatColor.GOLD + "Critical max roll!");
        } else if (roll == 1) {
            player.sendMessage(ChatColor.DARK_RED + "Oof... critical low roll.");
        }
        return true;
    }
}
