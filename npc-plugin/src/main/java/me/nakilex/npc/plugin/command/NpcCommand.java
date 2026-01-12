package me.nakilex.npc.plugin.command;

import me.nakilex.npc.core.model.Npc;
import me.nakilex.npc.core.model.NpcPosition;
import me.nakilex.npc.core.model.SkinRef;
import me.nakilex.npc.core.model.SkinSource;
import me.nakilex.npc.core.registry.NpcRegistry;
import me.nakilex.npc.plugin.service.NpcService;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class NpcCommand implements CommandExecutor, TabCompleter {
    private static final String PREFIX = ChatColor.DARK_AQUA + "[NPC] " + ChatColor.GRAY;
    private final NpcService service;

    public NpcCommand(NpcService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("npc.admin")) {
            sender.sendMessage(PREFIX + "You do not have permission to use NPC commands.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(PREFIX + "Usage: /npc <create|delete|list|select|rename|clone|spawn|despawn|tp|tphere|move|here|home|skin|export|import|reload>");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "list" -> handleList(sender);
            case "select" -> handleSelect(sender, args);
            case "rename" -> handleRename(sender, args);
            case "clone" -> handleClone(sender, args);
            case "spawn" -> handleSpawn(sender, args);
            case "despawn" -> handleDespawn(sender, args);
            case "tp" -> handleTeleport(sender, args);
            case "tphere" -> handleTeleportHere(sender, args);
            case "move" -> handleMove(sender, args);
            case "here" -> handleHere(sender, args);
            case "home" -> handleHome(sender, args);
            case "skin" -> handleSkin(sender, args);
            case "export" -> handleExport(sender, args);
            case "import" -> handleImport(sender, args);
            case "reload" -> handleReload(sender);
            default -> {
                sender.sendMessage(PREFIX + "Unknown subcommand.");
                yield true;
            }
        };
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + "Only players can create NPCs.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(PREFIX + "Usage: /npc create <name> [--type <entity>]");
            return true;
        }
        String name = args[1];
        EntityType type = EntityType.VILLAGER;
        if (args.length >= 4 && args[2].equalsIgnoreCase("--type")) {
            try {
                type = EntityType.valueOf(args[3].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(PREFIX + "Unknown entity type: " + args[3]);
                return true;
            }
        }
        int max = service.getPlugin().getConfig().getInt("maxNpcs", 200);
        if (service.getRegistry().list().size() >= max) {
            sender.sendMessage(PREFIX + "NPC limit reached (" + max + ").");
            return true;
        }
        Location location = player.getLocation();
        Npc npc = service.getRegistry().create(name, type, location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch(), location.getWorld().getName());
        npc.setViewRange(service.getPlugin().getConfig().getDouble("viewRange", 32.0));
        service.getLifecycle().spawn(npc);
        service.getRegistry().setSelectedNpc(player.getUniqueId(), npc.getId());
        sender.sendMessage(PREFIX + "Created NPC " + npc.getId() + " (" + npc.getName() + ").");
        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        Optional<Npc> target = resolveNpc(sender, args.length > 1 ? args[1] : null);
        if (target.isEmpty()) {
            sender.sendMessage(PREFIX + "NPC not found.");
            return true;
        }
        Npc npc = target.get();
        service.getLifecycle().despawn(npc);
        service.getRegistry().delete(npc.getId());
        sender.sendMessage(PREFIX + "Deleted NPC " + npc.getId() + ".");
        return true;
    }

    private boolean handleList(CommandSender sender) {
        List<String> entries = service.getRegistry().list().stream()
                .map(npc -> npc.getId() + ":" + npc.getName())
                .sorted()
                .collect(Collectors.toList());
        sender.sendMessage(PREFIX + "NPCs (" + entries.size() + "): " + String.join(", ", entries));
        return true;
    }

    private boolean handleSelect(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + "Only players can select NPCs.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(PREFIX + "Usage: /npc select <id>");
            return true;
        }
        Optional<Npc> target = resolveNpc(sender, args[1]);
        if (target.isEmpty()) {
            sender.sendMessage(PREFIX + "NPC not found.");
            return true;
        }
        service.getRegistry().setSelectedNpc(player.getUniqueId(), target.get().getId());
        sender.sendMessage(PREFIX + "Selected NPC " + target.get().getId() + ".");
        return true;
    }

    private boolean handleRename(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(PREFIX + "Usage: /npc rename <id|selected> <name>");
            return true;
        }
        Optional<Npc> target = resolveNpc(sender, args[1]);
        if (target.isEmpty()) {
            sender.sendMessage(PREFIX + "NPC not found.");
            return true;
        }
        target.get().setName(args[2]);
        service.getLifecycle().respawn(target.get());
        sender.sendMessage(PREFIX + "Renamed NPC " + target.get().getId() + " to " + args[2] + ".");
        return true;
    }

    private boolean handleClone(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(PREFIX + "Usage: /npc clone <id> [newName]");
            return true;
        }
        Optional<Npc> target = resolveNpc(sender, args[1]);
        if (target.isEmpty()) {
            sender.sendMessage(PREFIX + "NPC not found.");
            return true;
        }
        String newName = args.length >= 3 ? args[2] : null;
        Npc clone = service.getRegistry().cloneNpc(target.get().getId(), newName);
        service.getLifecycle().spawn(clone);
        sender.sendMessage(PREFIX + "Cloned NPC " + target.get().getId() + " to " + clone.getId() + ".");
        return true;
    }

    private boolean handleSpawn(CommandSender sender, String[] args) {
        Optional<Npc> target = resolveNpc(sender, args.length > 1 ? args[1] : null);
        if (target.isEmpty()) {
            sender.sendMessage(PREFIX + "NPC not found.");
            return true;
        }
        service.getLifecycle().spawn(target.get());
        sender.sendMessage(PREFIX + "Spawned NPC " + target.get().getId() + ".");
        return true;
    }

    private boolean handleDespawn(CommandSender sender, String[] args) {
        Optional<Npc> target = resolveNpc(sender, args.length > 1 ? args[1] : null);
        if (target.isEmpty()) {
            sender.sendMessage(PREFIX + "NPC not found.");
            return true;
        }
        service.getLifecycle().despawn(target.get());
        sender.sendMessage(PREFIX + "Despawned NPC " + target.get().getId() + ".");
        return true;
    }

    private boolean handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + "Only players can teleport to NPCs.");
            return true;
        }
        Optional<Npc> target = resolveNpc(sender, args.length > 1 ? args[1] : null);
        if (target.isEmpty()) {
            sender.sendMessage(PREFIX + "NPC not found.");
            return true;
        }
        Location location = target.get().getPosition() == null ? null : target.get().getPosition().toLocation();
        if (location == null) {
            sender.sendMessage(PREFIX + "NPC has no location.");
            return true;
        }
        player.teleport(location);
        sender.sendMessage(PREFIX + "Teleported to NPC " + target.get().getId() + ".");
        return true;
    }

    private boolean handleTeleportHere(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + "Only players can teleport NPCs.");
            return true;
        }
        Optional<Npc> target = resolveNpc(sender, args.length > 1 ? args[1] : null);
        if (target.isEmpty()) {
            sender.sendMessage(PREFIX + "NPC not found.");
            return true;
        }
        Location location = player.getLocation();
        service.updateNpcPosition(target.get(), location);
        service.getLifecycle().respawn(target.get());
        sender.sendMessage(PREFIX + "Teleported NPC " + target.get().getId() + " to you.");
        return true;
    }

    private boolean handleMove(CommandSender sender, String[] args) {
        return handleTeleportHere(sender, args);
    }

    private boolean handleHere(CommandSender sender, String[] args) {
        return handleTeleportHere(sender, args);
    }

    private boolean handleHome(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(PREFIX + "Usage: /npc home <set|clear|go> [id]");
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        Optional<Npc> target = resolveNpc(sender, args.length > 2 ? args[2] : null);
        if (target.isEmpty()) {
            sender.sendMessage(PREFIX + "NPC not found.");
            return true;
        }
        Npc npc = target.get();
        return switch (action) {
            case "set" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(PREFIX + "Only players can set home.");
                    yield true;
                }
                npc.setHomePosition(NpcPosition.fromLocation(player.getLocation()));
                sender.sendMessage(PREFIX + "Home set for NPC " + npc.getId() + ".");
                yield true;
            }
            case "clear" -> {
                npc.setHomePosition(null);
                sender.sendMessage(PREFIX + "Home cleared for NPC " + npc.getId() + ".");
                yield true;
            }
            case "go" -> {
                if (npc.getHomePosition() == null) {
                    sender.sendMessage(PREFIX + "NPC has no home location.");
                    yield true;
                }
                service.updateNpcPosition(npc, npc.getHomePosition().toLocation());
                service.getLifecycle().respawn(npc);
                sender.sendMessage(PREFIX + "NPC returned home.");
                yield true;
            }
            default -> {
                sender.sendMessage(PREFIX + "Usage: /npc home <set|clear|go> [id]");
                yield true;
            }
        };
    }

    private boolean handleSkin(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(PREFIX + "Usage: /npc skin set <id|selected> --player <name>|--uuid <uuid>|--url <url>|--tex <value> --sig <signature>");
            return true;
        }
        if (!args[1].equalsIgnoreCase("set")) {
            sender.sendMessage(PREFIX + "Only skin set is supported right now.");
            return true;
        }
        Optional<Npc> target = resolveNpc(sender, args[2]);
        if (target.isEmpty()) {
            sender.sendMessage(PREFIX + "NPC not found.");
            return true;
        }
        SkinRef ref = parseSkinRef(args, 3);
        if (ref == null) {
            sender.sendMessage(PREFIX + "Invalid skin arguments.");
            return true;
        }
        Npc npc = target.get();
        npc.setSkinRef(ref);
        service.applySkin(npc);
        sender.sendMessage(PREFIX + "Updated NPC skin.");
        return true;
    }

    private boolean handleExport(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(PREFIX + "Usage: /npc export <file>");
            return true;
        }
        File file = new File(service.getPlugin().getDataFolder(), args[1]);
        service.exportData(file);
        sender.sendMessage(PREFIX + "Exported NPC data to " + file.getName() + ".");
        return true;
    }

    private boolean handleImport(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(PREFIX + "Usage: /npc import <file>");
            return true;
        }
        File file = new File(service.getPlugin().getDataFolder(), args[1]);
        if (!file.exists()) {
            sender.sendMessage(PREFIX + "File not found: " + file.getName());
            return true;
        }
        service.importData(file);
        service.getLifecycle().spawnAll();
        sender.sendMessage(PREFIX + "Imported NPC data.");
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        service.getPlugin().reloadConfig();
        sender.sendMessage(PREFIX + "NPC config reloaded.");
        return true;
    }

    private Optional<Npc> resolveNpc(CommandSender sender, String token) {
        NpcRegistry registry = service.getRegistry();
        if (token == null || token.equalsIgnoreCase("selected")) {
            if (sender instanceof Player player) {
                return registry.getSelectedNpc(player.getUniqueId());
            }
            return Optional.empty();
        }
        try {
            int id = Integer.parseInt(token);
            return registry.get(id);
        } catch (NumberFormatException ex) {
            return registry.getByName(token);
        }
    }

    private SkinRef parseSkinRef(String[] args, int startIndex) {
        SkinSource source = null;
        String value = null;
        String signature = null;
        for (int i = startIndex; i < args.length; i++) {
            String arg = args[i];
            if (arg.equalsIgnoreCase("--player") && i + 1 < args.length) {
                source = SkinSource.PLAYER_NAME;
                value = args[++i];
            } else if (arg.equalsIgnoreCase("--uuid") && i + 1 < args.length) {
                source = SkinSource.PLAYER_UUID;
                value = args[++i];
            } else if (arg.equalsIgnoreCase("--url") && i + 1 < args.length) {
                source = SkinSource.URL;
                value = args[++i];
            } else if (arg.equalsIgnoreCase("--tex") && i + 1 < args.length) {
                source = SkinSource.TEXTURES;
                value = args[++i];
            } else if (arg.equalsIgnoreCase("--sig") && i + 1 < args.length) {
                signature = args[++i];
            }
        }
        if (source == null || value == null) {
            return null;
        }
        if (source == SkinSource.TEXTURES && signature == null) {
            return null;
        }
        if (source == SkinSource.PLAYER_UUID) {
            try {
                UUID.fromString(value);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
        return new SkinRef(source, value, signature);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("create", "delete", "list", "select", "rename", "clone", "spawn", "despawn", "tp",
                    "tphere", "move", "here", "home", "skin", "export", "import", "reload"), args[0]);
        }
        if (args.length == 2 && List.of("delete", "select", "rename", "clone", "spawn", "despawn", "tp", "tphere",
                "move", "here").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(getNpcTokens(), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("home")) {
            return filter(List.of("set", "clear", "go"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("home")) {
            return filter(getNpcTokens(), args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("skin")) {
            return filter(List.of("set"), args[1]);
        }
        if (args.length >= 3 && args[0].equalsIgnoreCase("skin") && args[1].equalsIgnoreCase("set")) {
            List<String> options = new ArrayList<>();
            if (args.length == 3) {
                options.addAll(getNpcTokens());
            } else {
                options.addAll(List.of("--player", "--uuid", "--url", "--tex", "--sig"));
            }
            return filter(options, args[args.length - 1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("create") && args[1] != null) {
            return List.of();
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("create") && args[2].equalsIgnoreCase("--type")) {
            return filter(entityTypeNames(), args[3]);
        }
        return List.of();
    }

    private List<String> getNpcTokens() {
        List<String> tokens = service.getRegistry().list().stream()
                .map(npc -> String.valueOf(npc.getId()))
                .collect(Collectors.toCollection(ArrayList::new));
        tokens.add("selected");
        return tokens;
    }

    private List<String> entityTypeNames() {
        List<String> names = new ArrayList<>();
        for (EntityType type : EntityType.values()) {
            names.add(type.name().toLowerCase(Locale.ROOT));
        }
        return names;
    }

    private List<String> filter(List<String> values, String token) {
        if (token == null || token.isEmpty()) {
            return values;
        }
        String lower = token.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower))
                .collect(Collectors.toList());
    }
}
