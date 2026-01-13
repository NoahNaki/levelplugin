package me.nakilex.levelplugin.npc.commands;

import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.npc.system.NpcRegistry;
import me.nakilex.levelplugin.npc.system.trait.LookCloseTrait;
import me.nakilex.levelplugin.npc.system.trait.SkinTrait;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.CommandUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class NpcCommand implements CommandExecutor, TabCompleter {
    private static final List<String> ROOT_SUBCOMMANDS = List.of("create", "remove", "list", "select", "sel");
    private static final List<String> SELECT_SUBCOMMANDS = List.of("target", "clear", "skin", "lookclose");
    private static final List<String> LOOKCLOSE_SUBCOMMANDS = List.of("on", "off", "remove");
    private static final List<String> SKIN_SUBCOMMANDS = List.of("set", "clear");

    private final NpcRegistry registry;
    private final Map<UUID, Integer> selections = new HashMap<>();

    public NpcCommand(NpcRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "create" -> handleCreate(sender, label, args);
            case "remove" -> handleRemove(sender, label, args);
            case "list" -> handleList(sender);
            case "select", "sel" -> handleSelect(sender, label, args);
            default -> {
                sendUsage(sender, label);
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtil.filterStartingWith(ROOT_SUBCOMMANDS, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> {
                if (args.length >= 3) {
                    return CommandUtil.filterStartingWith(entityTypeNames(), args[args.length - 1]);
                }
            }
            case "remove" -> {
                if (args.length == 2) {
                    List<String> options = new ArrayList<>(npcIdOptions());
                    options.add("sel");
                    options.add("target");
                    return CommandUtil.filterStartingWith(options, args[1]);
                }
            }
            case "select", "sel" -> {
                if (args.length == 2) {
                    List<String> options = new ArrayList<>(SELECT_SUBCOMMANDS);
                    options.addAll(npcIdOptions());
                    return CommandUtil.filterStartingWith(options, args[1]);
                }
                if (args.length == 3 && args[1].equalsIgnoreCase("skin")) {
                    return CommandUtil.filterStartingWith(SKIN_SUBCOMMANDS, args[2]);
                }
                if (args.length == 3 && args[1].equalsIgnoreCase("lookclose")) {
                    return CommandUtil.filterStartingWith(LOOKCLOSE_SUBCOMMANDS, args[2]);
                }
            }
            default -> {
            }
        }
        return List.of();
    }

    private boolean handleCreate(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "Only players can create NPCs.");
            return true;
        }
        if (args.length < 2) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                    "Usage: /" + label + " create <name> [entityType]");
            return true;
        }
        EntityType type = EntityType.VILLAGER;
        int nameEnd = args.length;
        EntityType parsed = parseEntityType(args[args.length - 1]);
        if (parsed != null && args.length >= 3) {
            type = parsed;
            nameEnd = args.length - 1;
        } else if (args.length >= 3 && args[args.length - 1].equalsIgnoreCase("player")) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.WARNING,
                    "PLAYER NPCs render as a name-only hologram. Use a spawnable entity type instead.");
        }
        String name = joinArgs(args, 1, nameEnd).trim();
        if (name.isBlank()) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "NPC name cannot be blank.");
            return true;
        }
        NPC npc = registry.createNpc(type, name);
        npc.spawn(player.getLocation());
        selections.put(player.getUniqueId(), npc.getId());
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS,
                "Created NPC #" + npc.getId() + " (" + type.name().toLowerCase(Locale.ROOT)
                        + ") named " + ChatColor.YELLOW + name + ChatColor.GREEN + ". Selected it for editing.");
        return true;
    }

    private boolean handleRemove(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                    "Usage: /" + label + " remove <npcId|sel|target>");
            return true;
        }
        NPC npc = resolveNpc(sender, args[1]);
        if (npc == null) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                    "No NPC found for " + args[1] + ".");
            return true;
        }
        registry.remove(npc.getId());
        selections.entrySet().removeIf(entry -> entry.getValue() == npc.getId());
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS,
                "Removed NPC #" + npc.getId() + ".");
        return true;
    }

    private boolean handleList(CommandSender sender) {
        List<NPC> npcs = registry.sorted();
        if (npcs.isEmpty()) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.WARNING, "No NPCs are registered.");
            return true;
        }
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "NPCs: " + ChatColor.YELLOW + npcs.size());
        for (NPC npc : npcs) {
            String name = npc.getName() == null || npc.getName().isBlank() ? "Unnamed" : npc.getName();
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                    "#" + npc.getId() + ChatColor.GRAY + " | " + ChatColor.YELLOW + name
                            + ChatColor.GRAY + " | " + npc.getType().name().toLowerCase(Locale.ROOT));
        }
        return true;
    }

    private boolean handleSelect(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "Only players can select NPCs.");
            return true;
        }
        if (args.length == 1) {
            return selectTargetNpc(player);
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("clear")) {
            selections.remove(player.getUniqueId());
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS, "Cleared NPC selection.");
            return true;
        }
        if (action.equals("target")) {
            return selectTargetNpc(player);
        }
        if (action.equals("skin")) {
            return handleSkin(player, label, args);
        }
        if (action.equals("lookclose")) {
            return handleLookClose(player, label, args);
        }
        Integer id = parseInt(action);
        if (id != null) {
            NPC npc = registry.getById(id);
            if (npc == null) {
                ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                        "No NPC found with id " + id + ".");
                return true;
            }
            selectNpc(player, npc);
            return true;
        }
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "Usage: /" + label + " sel <npcId|target|clear|skin|lookclose>");
        return true;
    }

    private boolean handleSkin(Player player, String label, String[] args) {
        NPC npc = getSelected(player);
        if (npc == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Select an NPC first with /" + label + " sel <npcId|target>.");
            return true;
        }
        if (args.length < 3) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Usage: /" + label + " sel skin <set|clear>");
            return true;
        }
        String action = args[2].toLowerCase(Locale.ROOT);
        if (action.equals("clear")) {
            npc.removeTrait(SkinTrait.class);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "Cleared skin trait for NPC #" + npc.getId() + ".");
            return true;
        }
        if (!action.equals("set")) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Usage: /" + label + " sel skin <set|clear>");
            return true;
        }
        if (args.length < 6) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Usage: /" + label + " sel skin set <skinName> <signature> <texture>");
            return true;
        }
        String skinName = args[3];
        String signature = args[4];
        String texture = args[5];
        SkinTrait trait = npc.getOrAddTrait(SkinTrait.class);
        trait.setSkinPersistent(skinName, signature, texture);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Updated skin trait for NPC #" + npc.getId() + ".");
        return true;
    }

    private boolean handleLookClose(Player player, String label, String[] args) {
        NPC npc = getSelected(player);
        if (npc == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Select an NPC first with /" + label + " sel <npcId|target>.");
            return true;
        }
        if (args.length < 3) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Usage: /" + label + " sel lookclose <on|off|remove> [range]");
            return true;
        }
        String action = args[2].toLowerCase(Locale.ROOT);
        if (action.equals("remove")) {
            npc.removeTrait(LookCloseTrait.class);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "Removed look-close trait for NPC #" + npc.getId() + ".");
            return true;
        }
        if (!action.equals("on") && !action.equals("off")) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Usage: /" + label + " sel lookclose <on|off|remove> [range]");
            return true;
        }
        LookCloseTrait trait = npc.getOrAddTrait(LookCloseTrait.class);
        trait.lookClose(action.equals("on"));
        if (args.length >= 4) {
            Double range = parseDouble(args[3]);
            if (range == null || range <= 0) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                        "Range must be a positive number.");
                return true;
            }
            trait.setRange(range);
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Look-close is now " + (trait.isEnabled() ? "enabled" : "disabled")
                        + " for NPC #" + npc.getId() + ".");
        return true;
    }

    private boolean selectTargetNpc(Player player) {
        NPC npc = resolveTargetNpc(player);
        if (npc == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Look at an NPC within 8 blocks to select it.");
            return true;
        }
        selectNpc(player, npc);
        return true;
    }

    private void selectNpc(Player player, NPC npc) {
        selections.put(player.getUniqueId(), npc.getId());
        String name = npc.getName() == null || npc.getName().isBlank() ? "Unnamed" : npc.getName();
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Selected NPC #" + npc.getId() + " (" + name + ").");
    }

    private NPC resolveNpc(CommandSender sender, String token) {
        if (token == null) {
            return null;
        }
        String lowered = token.toLowerCase(Locale.ROOT);
        if (lowered.equals("sel") || lowered.equals("selected")) {
            if (sender instanceof Player player) {
                return getSelected(player);
            }
            return null;
        }
        if (lowered.equals("target")) {
            if (sender instanceof Player player) {
                return resolveTargetNpc(player);
            }
            return null;
        }
        Integer id = parseInt(lowered);
        return id == null ? null : registry.getById(id);
    }

    private NPC getSelected(Player player) {
        Integer id = selections.get(player.getUniqueId());
        if (id == null) {
            return null;
        }
        NPC npc = registry.getById(id);
        if (npc == null) {
            selections.remove(player.getUniqueId());
        }
        return npc;
    }

    private NPC resolveTargetNpc(Player player) {
        org.bukkit.entity.Entity target = player.getTargetEntity(8);
        if (target == null) {
            return null;
        }
        return registry.getNPC(target);
    }

    private void sendUsage(CommandSender sender, String label) {
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "Usage: /" + label + " <create|remove|list|select|sel>");
    }

    private List<String> npcIdOptions() {
        return registry.sorted().stream()
                .sorted(Comparator.comparingInt(NPC::getId))
                .map(npc -> Integer.toString(npc.getId()))
                .toList();
    }

    private List<String> entityTypeNames() {
        List<String> names = new ArrayList<>();
        for (EntityType type : EntityType.values()) {
            if (type.isSpawnable()) {
                names.add(type.name().toLowerCase(Locale.ROOT));
            }
        }
        names.sort(String::compareToIgnoreCase);
        return names;
    }

    private EntityType parseEntityType(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        try {
            EntityType type = EntityType.valueOf(input.toUpperCase(Locale.ROOT));
            if (!type.isSpawnable()) {
                return null;
            }
            return type;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Integer parseInt(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Double parseDouble(String input) {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String joinArgs(String[] args, int startInclusive, int endExclusive) {
        StringBuilder builder = new StringBuilder();
        for (int i = startInclusive; i < endExclusive; i++) {
            if (i > startInclusive) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }
}
