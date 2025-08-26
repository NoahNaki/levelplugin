package me.nakilex.levelplugin.duels.commands;

import me.nakilex.levelplugin.duels.listeners.DuelListener;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import org.bukkit.Bukkit;
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
            send(player, MessageType.INFO, "Usage: /duel <player|accept|decline>");
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            boolean accepted = DuelManager.getInstance().acceptRequest(player);
            if (accepted) {
                send(player, MessageType.SUCCESS, "You have accepted the duel request!");
            } else {
                send(player, MessageType.ERROR, "You have no valid duel request to accept!");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("decline")) {
            boolean declined = DuelManager.getInstance().declineRequest(player);
            if (declined) {
                send(player, MessageType.ERROR, "You have declined the duel request!");
            } else {
                send(player, MessageType.ERROR, "You have no valid duel request to decline!");
            }
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            send(player, MessageType.ERROR, "Player not found.");
            return true;
        }
        if (target.equals(player)) {
            send(player, MessageType.ERROR, "You cannot duel yourself.");
            return true;
        }

        DuelManager manager = DuelManager.getInstance();
        if (manager.areInAnyDuel(player)) {
            send(player, MessageType.ERROR, "You are already in a duel.");
            return true;
        }
        if (manager.areInAnyDuel(target)) {
            send(player, MessageType.ERROR, "That player is already in a duel.");
            return true;
        }
        if (manager.getRequest(target.getUniqueId()) != null) {
            send(player, MessageType.ERROR, "That player already has a pending duel request.");
            return true;
        }

        manager.createRequest(player, target);
        send(player, MessageType.SUCCESS, "Duel request sent to " + target.getName() + "!");
        DuelListener.sendDuelRequestMessage(target, player.getName());
        return true;
    }
}
