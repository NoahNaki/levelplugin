package me.nakilex.levelplugin.duels.commands;

import me.nakilex.levelplugin.duels.listeners.DuelListener;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.duels.managers.DuelRequest;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.nakilex.levelplugin.utils.CommandUtil;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

public class DuelCommand implements TabExecutor {

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

        String sub = args[0];
        DuelManager manager = DuelManager.getInstance();

        if (sub.equalsIgnoreCase("accept")) {
            if (!manager.acceptRequest(player)) {
                send(player, MessageType.ERROR, "You have no valid duel request to accept!");
            }
            return true;
        }

        if (sub.equalsIgnoreCase("decline")) {
            boolean declined = manager.declineRequest(player);
            if (declined) {
                send(player, MessageType.INFO, "You have declined the duel request!");
            } else {
                send(player, MessageType.ERROR, "You have no valid duel request to decline!");
            }
            return true;
        }

        Player target = Bukkit.getPlayerExact(sub);
        if (target == null) {
            send(player, MessageType.ERROR, "Player not found.");
            return true;
        }
        if (target.equals(player)) {
            send(player, MessageType.ERROR, "You cannot duel yourself.");
            return true;
        }

        if (manager.areInAnyDuel(player) || manager.areInAnyDuel(target)) {
            send(player, MessageType.ERROR, "Either you or the target is already in a duel.");
            return true;
        }

        DuelRequest incoming = manager.getRequest(player.getUniqueId());
        if (incoming != null && incoming.getRequester().equals(target.getUniqueId())) {
            manager.acceptRequest(player);
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>(CommandUtil.onlinePlayerNames(args[0]));
            String lower = args[0].toLowerCase();
            if ("accept".startsWith(lower)) suggestions.add("accept");
            if ("decline".startsWith(lower)) suggestions.add("decline");
            return suggestions;
        }
        return Collections.emptyList();
    }
}
