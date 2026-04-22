package me.nakilex.levelplugin.stronghold.commands;

import me.nakilex.levelplugin.stronghold.StrongholdQueueManager;
import me.nakilex.levelplugin.stronghold.StrongholdQueueMode;
import me.nakilex.levelplugin.stronghold.gui.StrongholdQueueGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

public class StrongholdCommand implements TabExecutor {
    private final StrongholdQueueGUI gui;
    private final StrongholdQueueManager queueManager;

    public StrongholdCommand(StrongholdQueueGUI gui, StrongholdQueueManager queueManager) {
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
            StrongholdQueueMode mode = parseMode(args, 1).orElse(StrongholdQueueMode.SOLO);
            Optional<StrongholdQueueMode> current = queueManager.getMode(id);
            if (current.isPresent() && !current.get().equals(mode)) {
                send(player, MessageType.ERROR, "Leave your current Stronghold queue before joining another.");
                return true;
            }
            if (current.isPresent()) {
                send(player, MessageType.WARNING, "You are already in the " + current.get().displayName() + ChatColor.GRAY + " queue.");
                return true;
            }
            StrongholdQueueManager.QueueJoinOutcome outcome = queueManager.join(player, mode);
            if (outcome.result() == StrongholdQueueManager.QueueJoinResult.JOINED) {
                send(player, MessageType.SUCCESS, "You joined the " + mode.displayName() + ChatColor.GRAY + " queue.");
                gui.refresh();
            } else {
                send(player, MessageType.ERROR, outcome.message() == null
                        ? ChatColor.RED + "Unable to join Stronghold queue."
                        : outcome.message());
            }
            return true;
        }

        if (equalsAny(args[0], "leave", "quit")) {
            Optional<StrongholdQueueMode> current = queueManager.getMode(id);
            if (current.isEmpty()) {
                send(player, MessageType.WARNING, "You are not in a Stronghold queue.");
                return true;
            }
            queueManager.leave(id);
            send(player, MessageType.INFO, "You left the " + current.get().displayName() + ChatColor.GRAY + " queue.");
            gui.refresh();
            return true;
        }

        send(player, MessageType.INFO, "Usage: /" + label + " [join|leave|open] [solo|duo|squad]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return partial(args[0], List.of("join", "leave", "open"));
        }
        if (args.length == 2 && equalsAny(args[0], "join", "queue")) {
            return partial(args[1], Arrays.asList("solo", "duo", "squad"));
        }
        return Collections.emptyList();
    }

    private List<String> partial(String input, List<String> options) {
        String current = input.toLowerCase();
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(current)) {
                matches.add(option);
            }
        }
        return matches;
    }

    private boolean equalsAny(String input, String... values) {
        for (String value : values) {
            if (input.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private Optional<StrongholdQueueMode> parseMode(String[] args, int index) {
        if (args.length <= index) {
            return Optional.empty();
        }
        return switch (args[index].toLowerCase()) {
            case "solo", "s" -> Optional.of(StrongholdQueueMode.SOLO);
            case "duo", "d", "2" -> Optional.of(StrongholdQueueMode.DUO);
            case "squad", "q", "4" -> Optional.of(StrongholdQueueMode.SQUAD);
            default -> Optional.empty();
        };
    }
}
