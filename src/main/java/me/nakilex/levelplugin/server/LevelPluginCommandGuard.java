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
    private final Set<String> buildAllowedCommands;

    public LevelPluginCommandGuard(Main plugin, ServerSelectionManager serverSelectionManager) {
        this.serverSelectionManager = serverSelectionManager;
        this.guardedCommands = buildCommandSet(plugin);
        this.buildAllowedCommands = Set.of("world", "debug", "se");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (serverSelectionManager == null) {
            return;
        }
        Player player = event.getPlayer();
        World world = player.getWorld();
        if (serverSelectionManager.isAlphaWorld(world) || isDungeonInstance(world) || isArenaInstance(world)) {
            return;
        }
        String message = event.getMessage();
        if (message == null || !message.startsWith("/")) {
            return;
        }
        ParsedCommand parsed = parseCommand(message);
        String label = parsed.label();
        int colon = label.indexOf(':');
        if (colon >= 0 && colon < label.length() - 1) {
            label = label.substring(colon + 1);
        }
        if (isExplicitlyAllowedOutsideAlpha(label, parsed.args())) {
            return;
        }
        if ("hub".equals(label)) {
            return;
        }
        if ("world".equals(label)) {
            return;
        }
        if ("se".equals(label)) {
            return;
        }
        if (serverSelectionManager.isBuildWorld(world) && buildAllowedCommands.contains(label)) {
            return;
        }
        if (!guardedCommands.contains(label)) {
            return;
        }
        event.setCancelled(true);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                "You must be in the alpha test server to use that command.");
    }

    private boolean isExplicitlyAllowedOutsideAlpha(String label, List<String> args) {
        if ("dungeon".equals(label) && !args.isEmpty()) {
            return "stronghold".equals(args.get(0));
        }
        return false;
    }

    private ParsedCommand parseCommand(String message) {
        String payload = message.substring(1).trim();
        if (payload.isEmpty()) {
            return new ParsedCommand("", List.of());
        }
        String[] parts = payload.split("\\s+");
        String label = parts[0].toLowerCase(Locale.ROOT);
        if (parts.length <= 1) {
            return new ParsedCommand(label, List.of());
        }
        java.util.ArrayList<String> args = new java.util.ArrayList<>(parts.length - 1);
        for (int i = 1; i < parts.length; i++) {
            args.add(parts[i].toLowerCase(Locale.ROOT));
        }
        return new ParsedCommand(label, List.copyOf(args));
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

    private boolean isDungeonInstance(World world) {
        if (world == null) {
            return false;
        }
        var dungeonManager = Main.getInstance().getDungeonManager();
        return dungeonManager != null && dungeonManager.isInstanceWorld(world);
    }

    private boolean isArenaInstance(World world) {
        if (world == null) {
            return false;
        }
        var arenaManager = Main.getInstance().getArenaInstanceManager();
        return arenaManager != null && arenaManager.isInstanceWorld(world);
    }

    private record ParsedCommand(String label, List<String> args) {}
}
