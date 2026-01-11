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

public class HudDebugCommand implements CommandExecutor, TabCompleter {
    private final HudManager hudManager;

    public HudDebugCommand(HudManager hudManager) {
        this.hudManager = hudManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (hudManager == null) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "HUD system is not available.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "Only players can use /huddebug.");
            return true;
        }
        String mode = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "toggle";
        switch (mode) {
            case "on" -> hudManager.setDebugMode(player, true);
            case "off" -> hudManager.setDebugMode(player, false);
            case "toggle" -> hudManager.toggleDebugMode(player);
            default -> ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                    "Usage: /huddebug [on|off|toggle]");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("on", "off", "toggle");
        }
        return List.of();
    }
}
