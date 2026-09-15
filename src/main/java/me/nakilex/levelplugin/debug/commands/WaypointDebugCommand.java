package me.nakilex.levelplugin.debug.commands;

import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.waypoints.bukkit.DirectionalWaypointService;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** In-game editor for exercising directional text waypoints. */
public final class WaypointDebugCommand implements TabExecutor {
    private final DirectionalWaypointService waypointService;
    private final Map<UUID, Map<String, DebugWaypoint>> waypoints = new HashMap<>();
    private final Map<UUID, String> selected = new HashMap<>();

    public WaypointDebugCommand(DirectionalWaypointService waypointService) {
        this.waypointService = waypointService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("levelplugin.admin")) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "You do not have permission to use waypoint debugging.");
            return true;
        }
        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "set" -> set(player, args, false);
            case "move" -> set(player, args, true);
            case "show", "select" -> show(player, args);
            case "hide" -> hide(player);
            case "delete", "remove" -> delete(player, args);
            case "clear" -> clear(player);
            case "list" -> list(player);
            default -> {
                sendUsage(player);
                yield true;
            }
        };
    }

    private boolean set(Player player, String[] args, boolean mustExist) {
        if (args.length != 2 && args.length != 5) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Usage: /waypointdebug " + (mustExist ? "move" : "set") + " <name> [x y z]");
            return true;
        }
        String key = key(args[1]);
        Map<String, DebugWaypoint> playerWaypoints = waypoints.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>());
        DebugWaypoint existing = playerWaypoints.get(key);
        if (mustExist && existing == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "No waypoint named '" + args[1] + "'.");
            return true;
        }

        Location location = parseLocation(player, args);
        if (location == null) {
            return true;
        }
        String displayName = existing == null ? displayName(args[1]) : existing.displayName();
        playerWaypoints.put(key, new DebugWaypoint(displayName, location));
        selected.put(player.getUniqueId(), key);
        waypointService.show(player, displayName, ignored -> location.clone());
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                (mustExist ? "Moved" : existing == null ? "Created" : "Updated") + " waypoint '"
                        + displayName + "' to " + formatLocation(location) + ".");
        return true;
    }

    private boolean show(Player player, String[] args) {
        if (args.length != 2) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Usage: /waypointdebug show <name>");
            return true;
        }
        String key = key(args[1]);
        DebugWaypoint waypoint = get(player, key);
        if (waypoint == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "No waypoint named '" + args[1] + "'.");
            return true;
        }
        selected.put(player.getUniqueId(), key);
        waypointService.show(player, waypoint.displayName(), ignored -> waypoint.location().clone());
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Now guiding you to '" + waypoint.displayName() + "'.");
        return true;
    }

    private boolean hide(Player player) {
        waypointService.remove(player);
        selected.remove(player.getUniqueId());
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Directional waypoint hidden.");
        return true;
    }

    private boolean delete(Player player, String[] args) {
        if (args.length != 2) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Usage: /waypointdebug delete <name>");
            return true;
        }
        String key = key(args[1]);
        Map<String, DebugWaypoint> playerWaypoints = waypoints.get(player.getUniqueId());
        DebugWaypoint removed = playerWaypoints == null ? null : playerWaypoints.remove(key);
        if (removed == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "No waypoint named '" + args[1] + "'.");
            return true;
        }
        if (key.equals(selected.get(player.getUniqueId()))) {
            hide(player);
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Deleted waypoint '" + removed.displayName() + "'.");
        return true;
    }

    private boolean clear(Player player) {
        waypoints.remove(player.getUniqueId());
        waypointService.remove(player);
        selected.remove(player.getUniqueId());
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Deleted all of your debug waypoints.");
        return true;
    }

    private boolean list(Player player) {
        Map<String, DebugWaypoint> playerWaypoints = waypoints.get(player.getUniqueId());
        if (playerWaypoints == null || playerWaypoints.isEmpty()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, "You have no debug waypoints.");
            return true;
        }
        String active = selected.get(player.getUniqueId());
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, "Debug waypoints:");
        playerWaypoints.forEach((key, waypoint) -> ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                (key.equals(active) ? ChatColor.GREEN + "* " : ChatColor.GRAY + "- ")
                        + waypoint.displayName() + ChatColor.GRAY + " at " + formatLocation(waypoint.location())));
        return true;
    }

    private Location parseLocation(Player player, String[] args) {
        if (args.length == 2) {
            return player.getLocation().clone();
        }
        try {
            return new Location(player.getWorld(), Double.parseDouble(args[2]), Double.parseDouble(args[3]), Double.parseDouble(args[4]));
        } catch (NumberFormatException ex) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Coordinates must be numbers.");
            return null;
        }
    }

    private DebugWaypoint get(Player player, String key) {
        Map<String, DebugWaypoint> playerWaypoints = waypoints.get(player.getUniqueId());
        return playerWaypoints == null ? null : playerWaypoints.get(key);
    }

    private String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private String displayName(String name) {
        String clean = ChatColor.stripColor(name).trim();
        return clean.length() > 32 ? clean.substring(0, 32) : clean;
    }

    private String formatLocation(Location location) {
        return location.getWorld().getName() + " " + String.format(Locale.ROOT, "%.1f %.1f %.1f",
                location.getX(), location.getY(), location.getZ());
    }

    private void sendUsage(Player player) {
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Waypoint debug: set <name> [x y z], move <name> [x y z], show <name>, hide, delete <name>, list, clear.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(List.of("set", "move", "show", "hide", "delete", "list", "clear"), args[0]);
        }
        if (args.length == 2 && List.of("move", "show", "select", "delete", "remove").contains(args[0].toLowerCase(Locale.ROOT))) {
            Map<String, DebugWaypoint> stored = waypoints.get(player.getUniqueId());
            return filter(stored == null ? List.of() : new ArrayList<>(stored.keySet()), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted().toList();
    }

    private record DebugWaypoint(String displayName, Location location) {}
}
