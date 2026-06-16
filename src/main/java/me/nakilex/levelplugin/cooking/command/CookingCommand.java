package me.nakilex.levelplugin.cooking.command;

import me.nakilex.levelplugin.codex.FoodCodexGUI;
import me.nakilex.levelplugin.cooking.service.CookingSessionService;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/** Player cooking utility commands. */
public class CookingCommand implements CommandExecutor, TabCompleter {
    private final FoodCodexGUI foodCodexGUI;
    private final CookingSessionService sessionService;

    public CookingCommand(FoodCodexGUI foodCodexGUI, CookingSessionService sessionService) {
        this.foodCodexGUI = foodCodexGUI;
        this.sessionService = sessionService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        String subcommand = args.length == 0 ? "codex" : args[0].toLowerCase(java.util.Locale.ROOT);
        switch (subcommand) {
            case "codex", "foods", "catalog" -> foodCodexGUI.open(player);
            case "cancel", "stop" -> cancel(player);
            default -> {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                        "Usage: /" + label + " <codex|cancel>");
            }
        }
        return true;
    }

    private void cancel(Player player) {
        boolean cancelled = sessionService.cancelSessionByPlayer(player.getUniqueId(), true, "Player cancelled cooking");
        if (cancelled) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Cancelled your current cooking session and returned inserted ingredients.");
            return;
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "You do not have an active cooking session.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("codex", "cancel").stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase(java.util.Locale.ROOT)))
                    .toList();
        }
        return List.of();
    }
}
