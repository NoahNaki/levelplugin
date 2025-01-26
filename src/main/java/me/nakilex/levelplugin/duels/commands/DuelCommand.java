package me.nakilex.levelplugin.duels.commands;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DuelCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("Usage: /duel <accept|decline>");
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            boolean accepted = DuelManager.getInstance().acceptRequest(player);
            if (accepted) {
                player.sendMessage("§aYou have accepted the duel request!");
            } else {
                player.sendMessage("§cYou have no valid duel request to accept!");
            }
        } else if (args[0].equalsIgnoreCase("decline")) {
            boolean declined = DuelManager.getInstance().declineRequest(player);
            if (declined) {
                player.sendMessage("§cYou have declined the duel request!");
            } else {
                player.sendMessage("§cYou have no valid duel request to decline!");
            }
        } else {
            player.sendMessage("Usage: /duel <accept|decline>");
        }
        return true;
    }
}
