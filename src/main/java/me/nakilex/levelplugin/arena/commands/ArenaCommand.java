package me.nakilex.levelplugin.arena.commands;

import me.nakilex.levelplugin.arena.ArenaMode;
import me.nakilex.levelplugin.arena.ArenaQueueManager;
import me.nakilex.levelplugin.arena.gui.ArenaQueueGUI;
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
            ArenaMode mode = parseMode(args, 1).orElse(ArenaMode.ONE_VS_ONE);
            Optional<ArenaMode> current = queueManager.getMode(id);
            if (current.isPresent() && !current.get().equals(mode)) {
                send(player, MessageType.ERROR, "Leave your current arena queue before joining another.");
                return true;
            }
            if (current.isPresent()) {
                send(player, MessageType.WARNING, "You are already in the " + current.get().displayName() + ChatColor.GRAY + " queue.");
                return true;
            }

            ArenaQueueManager.QueueJoinOutcome outcome = queueManager.join(player, mode);
            if (outcome.result() == ArenaQueueManager.QueueJoinResult.JOINED) {
                send(player, MessageType.SUCCESS, "You joined the " + mode.displayName() + ChatColor.GRAY + " queue.");
                gui.refresh();
            } else {
                String message = outcome.message();
                if (message == null) {
                    switch (outcome.result()) {
                        case ALREADY_QUEUED -> message = ChatColor.RED + "You are already in that queue.";
                        case IN_MATCH -> message = ChatColor.RED + "You cannot queue while an arena match is active.";
                        case PARTY_REQUIRED -> message = ChatColor.RED + "A party of two is required for 2v2.";
                        case PARTY_SIZE_INVALID -> message = ChatColor.RED + "Your party must contain exactly 2 members.";
                        case PARTY_MEMBER_OFFLINE -> message = ChatColor.RED + "All party members must be online.";
                        case TEAM_MEMBER_QUEUED -> message = ChatColor.RED + "A party member is already queued.";
                        case TEAM_MEMBER_IN_MATCH -> message = ChatColor.RED + "A party member is already in a match.";
                        case RANK_GAP_TOO_LARGE -> message = ChatColor.RED + "Party members must be within one arena tier.";
                        default -> message = ChatColor.RED + "Unable to join the queue.";
                    }
                }
                send(player, MessageType.ERROR, message);
            }
            return true;
        }

        if (equalsAny(args[0], "leave", "quit")) {
            ArenaMode mode = parseMode(args, 1).orElse(null);
            Optional<ArenaMode> current = queueManager.getMode(id);
            if (current.isEmpty()) {
                send(player, MessageType.WARNING, "You are not currently in an arena queue.");
                return true;
            }
            if (mode != null && !current.get().equals(mode)) {
                send(player, MessageType.WARNING, "You are not in the " + mode.displayName() + ChatColor.GRAY + " queue.");
                return true;
            }
            if (queueManager.leave(id)) {
                send(player, MessageType.INFO, "You left the " + current.get().displayName() + ChatColor.GRAY + " queue.");
                gui.refresh();
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
        if (args.length == 2 && equalsAny(args[0], "join", "queue", "leave", "quit")) {
            String current = args[1].toLowerCase();
            List<String> options = Arrays.asList("1v1", "2v2");
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

    private Optional<ArenaMode> parseMode(String[] args, int index) {
        if (args.length <= index) {
            return Optional.empty();
        }
        String modeArg = args[index].toLowerCase();
        if (modeArg.contains("2")) {
            return Optional.of(ArenaMode.TWO_VS_TWO);
        }
        if (modeArg.contains("1")) {
            return Optional.of(ArenaMode.ONE_VS_ONE);
        }
        return Optional.empty();
    }
}
