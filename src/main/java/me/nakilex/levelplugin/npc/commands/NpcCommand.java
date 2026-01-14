package me.nakilex.levelplugin.npc.commands;

import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.npc.system.NpcApi;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class NpcCommand implements TabExecutor {
    private static final List<String> SUBCOMMANDS = List.of("create", "remove", "list", "move", "teleport", "spawn", "despawn");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("levelplugin.admin")) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "You do not have permission to do that.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "create" -> handleCreate(sender, label, args);
            case "remove" -> handleRemove(sender, label, args);
            case "list" -> handleList(sender);
            case "move" -> handleMove(sender, label, args);
            case "teleport" -> handleTeleport(sender, label, args);
            case "spawn" -> handleSpawn(sender, label, args);
            case "despawn" -> handleDespawn(sender, label, args);
            default -> {
                sendUsage(sender, label);
                yield true;
            }
        };
    }

    private boolean handleCreate(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "Only players can create NPCs.");
            return true;
        }
        if (args.length < 2) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO, "Usage: /" + label + " create <name> [type]");
            return true;
        }
        String name = args[1];
        EntityType type = EntityType.PLAYER;
        if (args.length > 2) {
            type = parseEntityType(args[2]);
            if (type == null) {
                ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "Unknown entity type.");
                return true;
            }
        }
        NPC npc = NpcApi.getRegistry().createNPC(type, name);
        npc.spawn(player.getLocation());
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS,
                "Created NPC #" + npc.getId() + " (" + npc.getName() + ") as " + npc.getType().name() + ".");
        return true;
    }

    private boolean handleRemove(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO, "Usage: /" + label + " remove <id>");
            return true;
        }
        Integer id = parseId(args[1]);
        if (id == null) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "Invalid NPC id.");
            return true;
        }
        NPC npc = NpcApi.getRegistry().getById(id);
        if (npc == null) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "No NPC found with that id.");
            return true;
        }
        NpcApi.getRegistry().remove(id);
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS, "Removed NPC #" + id + ".");
        return true;
    }

    private boolean handleList(CommandSender sender) {
        List<NPC> npcs = NpcApi.getRegistry().sorted();
        if (npcs.isEmpty()) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO, "No NPCs registered.");
            return true;
        }
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO, "NPCs:");
        for (NPC npc : npcs) {
            String status = npc.isSpawned() ? "spawned" : "despawned";
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                    "- #" + npc.getId() + " " + npc.getName() + " (" + npc.getType().name() + ", " + status + ")");
        }
        return true;
    }

    private boolean handleMove(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "Only players can move NPCs.");
            return true;
        }
        if (args.length < 2) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO, "Usage: /" + label + " move <id>");
            return true;
        }
        NPC npc = getNpcById(sender, args[1]);
        if (npc == null) {
            return true;
        }
        Location location = player.getLocation();
        npc.spawn(location);
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS,
                "Moved NPC #" + npc.getId() + " to your location.");
        return true;
    }

    private boolean handleTeleport(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "Only players can teleport to NPCs.");
            return true;
        }
        if (args.length < 2) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO, "Usage: /" + label + " teleport <id>");
            return true;
        }
        NPC npc = getNpcById(sender, args[1]);
        if (npc == null) {
            return true;
        }
        Location target = npc.isSpawned() && npc.getEntity() != null ? npc.getEntity().getLocation() : npc.getStoredLocation();
        if (target == null) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.WARNING, "NPC has no stored location.");
            return true;
        }
        player.teleport(target);
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS,
                "Teleported to NPC #" + npc.getId() + ".");
        return true;
    }

    private boolean handleSpawn(CommandSender sender, String label, String[] args) {
        NPC npc = getNpcById(sender, args.length > 1 ? args[1] : null);
        if (npc == null) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO, "Usage: /" + label + " spawn <id>");
            return true;
        }
        Location location = npc.getStoredLocation();
        if (location == null && sender instanceof Player player) {
            location = player.getLocation();
        }
        if (location == null) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "NPC has no stored location.");
            return true;
        }
        npc.spawn(location);
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS, "Spawned NPC #" + npc.getId() + ".");
        return true;
    }

    private boolean handleDespawn(CommandSender sender, String label, String[] args) {
        NPC npc = getNpcById(sender, args.length > 1 ? args[1] : null);
        if (npc == null) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO, "Usage: /" + label + " despawn <id>");
            return true;
        }
        npc.despawn();
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS, "Despawned NPC #" + npc.getId() + ".");
        return true;
    }

    private NPC getNpcById(CommandSender sender, String idValue) {
        Integer id = parseId(idValue);
        if (id == null) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "Invalid NPC id.");
            return null;
        }
        NPC npc = NpcApi.getRegistry().getById(id);
        if (npc == null) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "No NPC found with that id.");
            return null;
        }
        return npc;
    }

    private Integer parseId(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private EntityType parseEntityType(String value) {
        if (value == null) {
            return null;
        }
        try {
            return EntityType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void sendUsage(CommandSender sender, String label) {
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "Usage: /" + label + " <create|remove|list|move|teleport|spawn|despawn>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream()
                    .filter(opt -> opt.startsWith(input))
                    .toList();
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (List.of("remove", "move", "teleport", "spawn", "despawn").contains(sub)) {
                return NpcApi.getRegistry().sorted().stream()
                        .map(npc -> String.valueOf(npc.getId()))
                        .filter(id -> id.startsWith(args[1]))
                        .toList();
            }
            if ("create".equals(sub)) {
                return Collections.emptyList();
            }
        }
        if (args.length == 3 && "create".equalsIgnoreCase(args[0])) {
            String input = args[2].toLowerCase(Locale.ROOT);
            return Arrays.stream(EntityType.values())
                    .map(type -> type.name().toLowerCase(Locale.ROOT))
                    .filter(name -> name.startsWith(input))
                    .sorted()
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
