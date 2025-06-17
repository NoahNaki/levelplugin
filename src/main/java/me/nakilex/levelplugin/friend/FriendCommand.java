package me.nakilex.levelplugin.friend;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;

import java.util.UUID;

/**
 * Command to manage a player's friends.
 */
public class FriendCommand implements CommandExecutor {
    private final FriendManager manager;

    public FriendCommand(FriendManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        UUID id = player.getUniqueId();

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /friend <add|remove|list|accept|deny> [player]");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "add" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /friend add <player>");
                    return true;
                }
                OfflinePlayer off = Bukkit.getOfflinePlayer(args[1]);
                if (off.getName() == null) {
                    player.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                UUID targetId = off.getUniqueId();
                if (manager.areFriends(id, targetId)) {
                    player.sendMessage(ChatColor.RED + "You are already friends.");
                    return true;
                }
                if (me.nakilex.levelplugin.Main.getInstance().getIgnoreManager().isIgnoring(targetId, id)
                        || me.nakilex.levelplugin.Main.getInstance().getIgnoreManager().isIgnoring(id, targetId)) {
                    player.sendMessage(ChatColor.RED + "Cannot send request to that player.");
                    return true;
                }
                // If they already requested you, accept instead
                if (manager.getRequest(id) != null && manager.getRequest(id).equals(targetId)) {
                    manager.acceptRequest(id);
                    player.sendMessage(ChatColor.GREEN + "You accepted " + off.getName() + "'s friend request.");
                    Player online = Bukkit.getPlayer(targetId);
                    if (online != null) online.sendMessage(ChatColor.GREEN + player.getName() + " accepted your friend request.");
                    return true;
                }
                if (manager.sendRequest(id, targetId)) {
                    player.sendMessage(ChatColor.GREEN + "Friend request sent to " + off.getName() + ".");
                    Player online = Bukkit.getPlayer(targetId);
                    if (online != null) {
                        online.sendMessage(ChatColor.YELLOW + player.getName() + " has sent you a friend request.");
                        online.sendMessage(ChatColor.YELLOW + "Type /friend accept to add or /friend deny to decline.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Request already pending or invalid.");
                }
            }
            case "remove" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /friend remove <player>");
                    return true;
                }
                OfflinePlayer off = Bukkit.getOfflinePlayer(args[1]);
                if (off.getName() == null) {
                    player.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                if (manager.removeFriend(id, off.getUniqueId())) {
                    player.sendMessage(ChatColor.GREEN + "Removed " + off.getName() + " from friends.");
                } else {
                    player.sendMessage(ChatColor.RED + "That player is not your friend.");
                }
            }
            case "accept" -> {
                UUID inviter = manager.getRequest(id);
                if (manager.acceptRequest(id)) {
                    String name = inviter != null ? Bukkit.getOfflinePlayer(inviter).getName() : "Someone";
                    player.sendMessage(ChatColor.GREEN + "Friend request accepted from " + name + ".");
                    Player on = inviter != null ? Bukkit.getPlayer(inviter) : null;
                    if (on != null) on.sendMessage(ChatColor.GREEN + player.getName() + " accepted your friend request.");
                } else {
                    player.sendMessage(ChatColor.RED + "No pending friend request.");
                }
            }
            case "deny" -> {
                UUID inviter = manager.getRequest(id);
                if (manager.denyRequest(id)) {
                    player.sendMessage(ChatColor.YELLOW + "Friend request denied.");
                    if (inviter != null) {
                        Player on = Bukkit.getPlayer(inviter);
                        if (on != null) on.sendMessage(ChatColor.RED + player.getName() + " denied your friend request.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "No pending friend request.");
                }
            }
            case "list" -> {
                int page = 1;
                if (args.length >= 2) {
                    try {
                        page = Integer.parseInt(args[1]);
                        if (page < 1) page = 1;
                    } catch (NumberFormatException ignore) {
                    }
                }

                java.util.List<UUID> all = new java.util.ArrayList<>(manager.getFriends(id));
                if (all.isEmpty()) {
                    player.sendMessage(ChatColor.YELLOW + "You have no friends.");
                    return true;
                }
                all.sort(java.util.Comparator.comparing(u -> {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(u);
                    String n = op.getName();
                    return n == null ? u.toString() : n.toLowerCase();
                }));
                int maxPage = (all.size() - 1) / 10 + 1;
                if (page > maxPage) page = maxPage;
                int start = (page - 1) * 10;
                int end = Math.min(start + 10, all.size());

                player.sendMessage(ChatColor.AQUA + "Friends - Page " + page + "/" + maxPage);
                long now = System.currentTimeMillis();
                for (int i = start; i < end; i++) {
                    UUID f = all.get(i);
                    OfflinePlayer op = Bukkit.getOfflinePlayer(f);
                    String name = op.getName() != null ? op.getName() : f.toString();
                    PlayerClass pc = StatsManager.getInstance().getPlayerStats(f).playerClass;
                    String className = pc.name().substring(0,1) + pc.name().substring(1).toLowerCase();
                    int lvl = LevelManager.getInstance().getLevel(f);
                    String seen;
                    if (op.isOnline()) {
                        seen = ChatColor.GREEN + "Online";
                    } else {
                        long days = op.getLastPlayed() > 0 ? (now - op.getLastPlayed()) / 86400000L : -1;
                        if (days < 0) {
                            seen = ChatColor.GRAY + "(Last seen ?d)";
                        } else {
                            seen = ChatColor.GRAY + "(Last seen " + days + "d)";
                        }
                    }

                    player.sendMessage(ChatColor.GRAY + "- "
                            + ChatColor.DARK_GRAY + "[" + ChatColor.GRAY + "Lv. " + lvl + " " + className
                            + ChatColor.DARK_GRAY + "] "
                            + ChatColor.GREEN + name + ChatColor.GRAY + " - "
                            + ChatColor.YELLOW + name + " " + seen);
                }

                if (maxPage > 1) {
                    TextComponent nav = new TextComponent("");
                    if (page > 1) {
                        TextComponent prev = new TextComponent(ChatColor.GREEN + "[Previous]");
                        prev.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/friend list " + (page - 1)));
                        nav.addExtra(prev);
                    }
                    if (page < maxPage) {
                        if (page > 1) nav.addExtra(" ");
                        TextComponent next = new TextComponent(ChatColor.GREEN + "[Next]");
                        next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/friend list " + (page + 1)));
                        nav.addExtra(next);
                    }
                    player.spigot().sendMessage(nav);
                }
            }
            default -> player.sendMessage(ChatColor.RED + "Unknown subcommand.");
        }
        return true;
    }
}
