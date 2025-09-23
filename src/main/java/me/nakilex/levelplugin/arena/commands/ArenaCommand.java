package me.nakilex.levelplugin.arena.commands;

import me.nakilex.levelplugin.arena.ArenaQueueManager;
import me.nakilex.levelplugin.arena.gui.ArenaQueueGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

/**
 * Command entry point for interacting with the arena queue. Players can join,
 * leave or open the GUI menu using a consistent command syntax.
 */
public class ArenaCommand implements TabExecutor {
    private final ArenaQueueGUI gui;
    private final ArenaQueueManager queueManager;

    public ArenaCommand(ArenaQueueGUI gui, ArenaQueueManager queueManager) {
        this.gui = gui;
        this.queueManager = queueManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, MessageType.ERROR, "Only players can use this command.");
            return true;
        }

        if (args.length == 0 || equalsAny(args[0], "open", "menu", "gui")) {
            gui.open(player);
            return true;
        }

        UUID id = player.getUniqueId();
        if (equalsAny(args[0], "join", "queue")) {
            if (queueManager.isQueued(id)) {
                send(player, MessageType.WARNING, "You are already in the arena queue.");
            } else {
                queueManager.join(player);
                send(player, MessageType.SUCCESS, "You joined the arena queue.");
                gui.refresh();
            }
            return true;
        }

        if (equalsAny(args[0], "leave", "quit")) {
            if (queueManager.leave(id)) {
                send(player, MessageType.INFO, "You left the arena queue.");
                gui.refresh();
            } else {
                send(player, MessageType.WARNING, "You are not currently in the arena queue.");
            }
            return true;
        }

        send(player, MessageType.INFO, "Usage: /" + label + " [join|leave|open]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String current = args[0].toLowerCase();
            List<String> options = Arrays.asList("join", "leave", "open");
            List<String> matches = new ArrayList<>();
            for (String option : options) {
                if (option.startsWith(current)) {
                    matches.add(option);
                }
            }
            return matches;
        }
        return Collections.emptyList();
    }

    private boolean equalsAny(String input, String... values) {
        for (String value : values) {
            if (input.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}
