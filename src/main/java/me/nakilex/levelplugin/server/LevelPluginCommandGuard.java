package me.nakilex.levelplugin.server;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class LevelPluginCommandGuard implements Listener {

    private final ServerSelectionManager serverSelectionManager;
    private final Set<String> guardedCommands;

    public LevelPluginCommandGuard(Main plugin, ServerSelectionManager serverSelectionManager) {
        this.serverSelectionManager = serverSelectionManager;
        this.guardedCommands = buildCommandSet(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (serverSelectionManager == null) {
            return;
        }
        Player player = event.getPlayer();
        World world = player.getWorld();
        if (serverSelectionManager.isAlphaWorld(world)) {
            return;
        }
        String message = event.getMessage();
        if (message == null || !message.startsWith("/")) {
            return;
        }
        String label = message.substring(1).split(" ")[0].toLowerCase(Locale.ROOT);
        int colon = label.indexOf(':');
        if (colon >= 0 && colon < label.length() - 1) {
            label = label.substring(colon + 1);
        }
        if (!guardedCommands.contains(label)) {
            return;
        }
        event.setCancelled(true);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                "You must be in the alpha test server to use that command.");
    }

    private Set<String> buildCommandSet(Main plugin) {
        Map<String, Map<String, Object>> commandMap = plugin.getDescription().getCommands();
        Set<String> commands = new HashSet<>();
        if (commandMap == null) {
            return commands;
        }
        for (Map.Entry<String, Map<String, Object>> entry : commandMap.entrySet()) {
            String key = entry.getKey();
            if (key != null && !key.isBlank()) {
                commands.add(key.toLowerCase(Locale.ROOT));
            }
            Map<String, Object> info = entry.getValue();
            if (info == null) {
                continue;
            }
            Object aliases = info.get("aliases");
            if (aliases instanceof List<?> list) {
                for (Object alias : list) {
                    if (alias != null) {
                        commands.add(alias.toString().toLowerCase(Locale.ROOT));
                    }
                }
            } else if (aliases instanceof String alias) {
                commands.add(alias.toLowerCase(Locale.ROOT));
            }
        }
        return commands;
    }
}
