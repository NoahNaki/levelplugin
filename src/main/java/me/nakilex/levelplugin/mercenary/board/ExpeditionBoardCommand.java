package me.nakilex.levelplugin.mercenary.board;

import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Command for distributing the expedition board wand and reloading board data.
 */
public class ExpeditionBoardCommand implements CommandExecutor, TabCompleter {

    private final ExpeditionBoardManager boardManager;

    public ExpeditionBoardCommand(ExpeditionBoardManager boardManager) {
        this.boardManager = boardManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "wand" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can receive the wand.");
                    return true;
                }
                ItemStack wand = boardManager.createWand();
                player.getInventory().addItem(wand);
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "You received an expedition board wand.");
                return true;
            }
            case "reload" -> {
                boardManager.reload();
                sender.sendMessage(ChatColor.GREEN + "Reloaded expedition boards (" + boardManager.getBoards().size() + " loaded).");
                return true;
            }
            default -> {
                sendUsage(sender);
                return true;
            }
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "/expeditionboard wand" + ChatColor.GRAY + " - Get the placement wand.");
        sender.sendMessage(ChatColor.YELLOW + "/expeditionboard reload" + ChatColor.GRAY + " - Reload saved boards.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("wand", "reload");
        }
        return List.of();
    }
}
