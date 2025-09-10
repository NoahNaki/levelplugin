package me.nakilex.levelplugin.arena;

import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType.INFO;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType.ERROR;

/**
 * Handles /rank and /elo commands for players.
 */
public class RankCommand implements CommandExecutor {
    private final ArenaManager arena = ArenaManager.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ERROR, "Only players can use this command.");
            return true;
        }
        ArenaManager.Rating rating = arena.getRating(player.getUniqueId());
        if (command.getName().equalsIgnoreCase("elo")) {
            ChatMessageUtil.send(player, INFO, "Your MMR: " + rating.mmr);
        } else {
            String rank = arena.getTierName(rating.rankPoints);
            ChatMessageUtil.send(player, INFO, "Rank: " + rank + " (" + rating.rankPoints + " RP)");
        }
        return true;
    }
}
