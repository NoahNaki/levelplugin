package me.nakilex.levelplugin.duels.commands;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.duels.utils.DuelMessageUtil;
import me.nakilex.levelplugin.Main;
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

        DuelManager manager = DuelManager.getInstance();

        if (args[0].equalsIgnoreCase("accept")) {
            boolean accepted = manager.acceptRequest(player);
            if (accepted) {
                send(player, MessageType.SUCCESS, "You have accepted the duel request!");
            } else {
                send(player, MessageType.ERROR, "You have no valid duel request to accept!");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("decline")) {
            boolean declined = manager.declineRequest(player);
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
        if (Main.getInstance().getIgnoreManager().isIgnoring(target.getUniqueId(), player.getUniqueId())
                || Main.getInstance().getIgnoreManager().isIgnoring(player.getUniqueId(), target.getUniqueId())) {
            send(player, MessageType.ERROR, "Cannot duel that player.");
            return true;
        }
        if (manager.areInAnyDuel(player) || manager.areInAnyDuel(target)) {
            send(player, MessageType.ERROR, "One of you is already in a duel.");
            return true;
        }
        if (manager.getRequest(target.getUniqueId()) != null) {
            send(player, MessageType.ERROR, "That player already has a pending duel request.");
            return true;
        }

        manager.createRequest(player, target);
        DuelMessageUtil.sendRequest(player, target);
        return true;
    }
}
