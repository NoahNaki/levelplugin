package me.nakilex.levelplugin.animatedlb;

import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public class AnimatedLeaderboardPlugin implements CommandExecutor, TabCompleter {
    private final LeaderboardManager manager;

    public AnimatedLeaderboardPlugin(LeaderboardManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO, "Usage: /animatedlb <spawn|remove|reload|next>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "spawn" -> {
                boolean spawned = manager.spawn();
                ChatMessageUtil.send(sender, spawned ? ChatMessageUtil.MessageType.SUCCESS : ChatMessageUtil.MessageType.ERROR,
                        spawned ? "Animated leaderboard spawned." : "Could not spawn leaderboard. Check world config.");
            }
            case "remove" -> { manager.remove(); ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS, "Animated leaderboard removed."); }
            case "reload" -> { manager.reload(); ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS, "Animated leaderboard config reloaded."); }
            case "next" -> { manager.next(); ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO, "Switched leaderboard phase."); }
            default -> ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "Unknown subcommand.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? List.of("spawn", "remove", "reload", "next") : List.of();
    }
}
