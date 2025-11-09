package me.nakilex.levelplugin.dungeon;

import me.nakilex.levelplugin.Main;
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
            default -> {
                return false;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtil.simpleSuggestions(args[0], "create", "edit", "undo", "play", "list", "delete", "rate");
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
