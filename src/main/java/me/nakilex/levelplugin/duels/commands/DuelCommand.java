package me.nakilex.levelplugin.duels.commands;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

public class DuelCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            send(sender, MessageType.ERROR, "Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            send(player, MessageType.INFO, "Usage: /duel <accept|decline>");
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            boolean accepted = DuelManager.getInstance().acceptRequest(player);
            if (accepted) {
                send(player, MessageType.SUCCESS, "You have accepted the duel request!");
            } else {
                send(player, MessageType.ERROR, "You have no valid duel request to accept!");
            }
        } else if (args[0].equalsIgnoreCase("decline")) {
            boolean declined = DuelManager.getInstance().declineRequest(player);
            if (declined) {
                send(player, MessageType.ERROR, "You have declined the duel request!");
            } else {
                send(player, MessageType.ERROR, "You have no valid duel request to decline!");
            }
        } else {
            send(player, MessageType.INFO, "Usage: /duel <accept|decline>");
        }
        return true;
    }
}
