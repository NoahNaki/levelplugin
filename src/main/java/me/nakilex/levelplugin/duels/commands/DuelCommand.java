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
        } else if (args.length == 1) {
            Player target = player.getServer().getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                send(player, MessageType.ERROR, "Player not found.");
            } else if (target.equals(player)) {
                send(player, MessageType.ERROR, "You cannot duel yourself.");
            } else {
                DuelManager.getInstance().sendDuelRequest(player, target);
            }
        } else {
            send(player, MessageType.INFO, "Usage: /duel <player|accept|decline>");
        }
        return true;
    }
}
