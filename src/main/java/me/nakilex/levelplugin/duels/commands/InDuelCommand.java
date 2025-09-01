package me.nakilex.levelplugin.duels.commands;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.utils.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

public class InDuelCommand implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            send(sender, MessageType.INFO, "Usage: /induel <player>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            send(sender, MessageType.ERROR, "Player not found.");
            return true;
        }
        boolean inDuel = DuelManager.getInstance().areInAnyDuel(target);
        send(sender, MessageType.INFO, target.getName() + " in duel: " + inDuel);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtil.onlinePlayerNames(args[0]);
        }
        return Collections.emptyList();
    }
}
