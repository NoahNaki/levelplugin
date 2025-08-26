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
        int roll = ThreadLocalRandom.current().nextInt(1, 101);
        Component msg = Component.text()
                .append(player.displayName())
                .append(Component.text(" rolled " + roll + " (1-100)", NamedTextColor.YELLOW))
                .build();
        ChatManager.sendChannelMessage(player, msg);
        return true;
    }
}
