package me.nakilex.levelplugin.dungeon;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.stronghold.StrongholdEnums;
import me.nakilex.levelplugin.dungeon.stronghold.StrongholdPlacement;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import me.nakilex.levelplugin.dungeon.DungeonLayout;
import org.bukkit.conversations.ConversationFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import me.nakilex.levelplugin.utils.CommandUtil;

public class DungeonCommand implements CommandExecutor, TabCompleter {
    private final DungeonManager manager;

    public DungeonCommand(Main plugin) {
        this.manager = plugin.getDungeonManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length < 1) return false;
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create" -> {
                manager.getBuilder().start(player);
                player.sendMessage(ChatColor.YELLOW + "Entered dungeon edit mode.");
                return true;
            }
            case "edit" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /dungeon edit <name>");
                    return true;
                }
                String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                DungeonLayout layout = manager.getLayout(name);
                if (layout == null) {
                    player.sendMessage(ChatColor.RED + "Layout not found.");
                    return true;
                }
                manager.getBuilder().edit(player, layout);
                player.sendMessage(ChatColor.YELLOW + "Editing dungeon '" + name + "'.");
                return true;
            }
            case "undo" -> {
                manager.getBuilder().undo(player);
                return true;
            }
            case "play" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /dungeon play <name>");
                    return true;
                }
                String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                boolean ok = manager.playDungeon(player, name);
                if (ok) player.sendMessage(ChatColor.YELLOW + "Dungeon spawned.");
                else player.sendMessage(ChatColor.RED + "Layout not found.");
                return true;
            }
            case "list" -> {
                Main.getInstance().getDungeonListGUI().open(player);
                return true;
            }
            case "leave" -> {
                manager.getPlugin().getDungeonLeaveGUI().open(player);
                return true;
            }
            case "delete" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /dungeon delete <name>");
                    return true;
                }
                String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                boolean ok = manager.deleteDungeon(name);
                if (ok) player.sendMessage(ChatColor.GREEN + "Dungeon removed.");
                else player.sendMessage(ChatColor.RED + "Dungeon not found.");
                return true;
            }
            case "rate" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /dungeon rate <name>");
                    return true;
                }
                String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                String key = DungeonManager.normalizeKey(name);
                String pending = manager.getPendingRating(player.getUniqueId());
                if (pending == null || !pending.equalsIgnoreCase(key)) {
                    player.sendMessage(ChatColor.RED + "You cannot rate this dungeon right now.");
                    return true;
                }

                org.bukkit.conversations.ConversationFactory factory = new org.bukkit.conversations.ConversationFactory(Main.getInstance())
                        .withFirstPrompt(new me.nakilex.levelplugin.dungeon.rating.DungeonRatingPrompt(key, player))
                        .withLocalEcho(false)
                        .addConversationAbandonedListener(event -> { });
                factory.buildConversation(player).begin();
                return true;
            }
            case "stronghold" -> {
                return handleStrongholdCommand(player, args);
            }
            default -> {
                return false;
            }
        }
    }

    private boolean handleStrongholdCommand(Player player, String[] args) {
        if (args.length < 2) {
            ChatMessageUtil.send(player, MessageType.INFO,
                    "Usage: /dungeon stronghold <debug|generate> ...");
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "debug" -> {
                if (args.length < 3) {
                    ChatMessageUtil.send(player, MessageType.INFO,
                            "Usage: /dungeon stronghold debug <on|off>");
                    return true;
                }
                boolean enabled = args[2].equalsIgnoreCase("on");
                if (!enabled && !args[2].equalsIgnoreCase("off")) {
                    ChatMessageUtil.send(player, MessageType.ERROR,
                            "Usage: /dungeon stronghold debug <on|off>");
                    return true;
                }
                manager.getStrongholdDebug().setEnabled(enabled);
                ChatMessageUtil.send(player, MessageType.SUCCESS,
                        "Stronghold debug logging is now " + (enabled ? "enabled" : "disabled") + ".");
                return true;
            }
            case "generate" -> {
                if (args.length < 5) {
                    ChatMessageUtil.send(player, MessageType.INFO,
                            "Usage: /dungeon stronghold generate <snake|branching|test> <rooms> <seed> [attempts]");
                    return true;
                }
                StrongholdEnums.GraphMode mode = parseMode(args[2]);
                if (mode == null) {
                    ChatMessageUtil.send(player, MessageType.ERROR,
                            "Invalid mode. Use: snake, branching, or test.");
                    return true;
                }
                Integer rooms = parseInt(args[3]);
                Long seed = parseLong(args[4]);
                Integer attempts = args.length >= 6 ? parseInt(args[5]) : 5;
                if (rooms == null || rooms <= 0 || seed == null || attempts == null || attempts <= 0) {
                    ChatMessageUtil.send(player, MessageType.ERROR,
                            "rooms/attempts must be > 0 and seed must be a valid number.");
                    return true;
                }
                StrongholdPlacement.PlacementResult result =
                        manager.runStrongholdDebugGeneration(mode, rooms, seed, attempts);
                if (!result.success()) {
                    ChatMessageUtil.send(player, MessageType.ERROR,
                            "Stronghold generation failed. See latest debug lines below.");
                } else {
                    ChatMessageUtil.send(player, MessageType.SUCCESS,
                            "Stronghold generation succeeded with " + result.rooms().size() + " placed rooms.");
                }
                int maxLines = Math.min(8, result.logs().size());
                for (int i = Math.max(0, result.logs().size() - maxLines); i < result.logs().size(); i++) {
                    ChatMessageUtil.send(player, MessageType.INFO, ChatColor.GRAY + result.logs().get(i));
                }
                return true;
            }
            default -> {
                ChatMessageUtil.send(player, MessageType.ERROR,
                        "Unknown stronghold action. Use debug or generate.");
                return true;
            }
        }
    }

    private StrongholdEnums.GraphMode parseMode(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "snake" -> StrongholdEnums.GraphMode.SNAKE;
            case "branching" -> StrongholdEnums.GraphMode.BRANCHING;
            case "test" -> StrongholdEnums.GraphMode.TEST;
            default -> null;
        };
    }

    private Integer parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtil.simpleSuggestions(args[0], "create", "edit", "undo", "play", "list", "leave", "delete", "rate", "stronghold");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("stronghold")) {
            return CommandUtil.simpleSuggestions(args[1], "debug", "generate");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("stronghold")) {
            if (args[1].equalsIgnoreCase("debug")) {
                return CommandUtil.simpleSuggestions(args[2], "on", "off");
            }
            if (args[1].equalsIgnoreCase("generate")) {
                return CommandUtil.simpleSuggestions(args[2], "snake", "branching", "test");
            }
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("stronghold") && args[1].equalsIgnoreCase("generate")) {
            return CommandUtil.numberRange(args[3], 2, 128);
        }
        if (args.length == 6 && args[0].equalsIgnoreCase("stronghold") && args[1].equalsIgnoreCase("generate")) {
            return CommandUtil.numberRange(args[5], 1, 25);
        }
        if (args.length >= 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("edit") || sub.equals("play") || sub.equals("delete") || sub.equals("rate")) {
                Set<String> names = new LinkedHashSet<>();
                for (var entry : manager.getLayoutEntries()) {
                    names.add(entry.getKey());
                    names.add(entry.getValue());
                }
                return CommandUtil.filterStartingWith(names, args[1]);
            }
        }
        return Collections.emptyList();
    }
}
