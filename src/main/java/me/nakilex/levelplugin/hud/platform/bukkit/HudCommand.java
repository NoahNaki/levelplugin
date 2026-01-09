package me.nakilex.levelplugin.hud.platform.bukkit;

import me.nakilex.levelplugin.hud.core.HudManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public class HudCommand implements CommandExecutor, TabCompleter {
    private final HudManager hudManager;

    public HudCommand(HudManager hudManager) {
        this.hudManager = hudManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (hudManager == null) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "HUD system is not available.");
            return true;
        }
        if (args.length == 0) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO, "/hud reload | /hud debug | /hud toggle");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                hudManager.reload();
                ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS, "HUD reloaded.");
            }
            case "debug" -> {
                if (!(sender instanceof Player player)) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "Only players can use /hud debug.");
                    return true;
                }
                hudManager.debug(player);
            }
            case "toggle" -> {
                if (!(sender instanceof Player player)) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "Only players can use /hud toggle.");
                    return true;
                }
                hudManager.toggle(player);
            }
            default -> ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.WARNING, "Unknown subcommand.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "debug", "toggle");
        }
        return List.of();
    }
}
