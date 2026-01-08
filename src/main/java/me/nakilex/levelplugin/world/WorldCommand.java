package me.nakilex.levelplugin.world;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import me.nakilex.levelplugin.lootchests.utils.LocationUtils;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import net.md_5.bungee.api.chat.*;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldType;
import org.bukkit.GameRule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class WorldCommand implements CommandExecutor, TabCompleter {
    private final WorldManager manager;

    public WorldCommand(WorldManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1) return false;
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /world create <name> <type>");
                    return true;
                }
                String name = args[1];
                String typeStr = args[2].toLowerCase();
                Environment env = Environment.NORMAL;
                WorldType type = WorldType.NORMAL;
                if (typeStr.equals("void")) {
                    type = WorldType.FLAT;
                    env = Environment.NORMAL;
                } else if (typeStr.equals("flatland")) {
                    type = WorldType.FLAT;
                } else if (typeStr.equals("nether")) {
                    env = Environment.NETHER;
                } else if (typeStr.equals("end")) {
                    env = Environment.THE_END;
                }
                World world = manager.createWorld(name, type, env);
                if (world != null) {
                    sender.sendMessage(ChatColor.GREEN + "World created: " + name);
                } else {
                    sender.sendMessage(ChatColor.RED + "Failed to create world.");
                }
                return true;
            }
            case "clone", "copy" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /world " + sub + " <source> <target>");
                    return true;
                }
                boolean ok = manager.cloneWorld(args[1], args[2]);
                if (ok) {
                    sender.sendMessage(ChatColor.YELLOW + "World cloned: " + args[1] + " -> " + args[2]);
                } else {
                    sender.sendMessage(ChatColor.RED + "Failed to clone world.");
                }
                return true;
            }
            case "import" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /world import <name>");
                    return true;
                }
                World world = manager.importWorld(args[1]);
                if (world != null) sender.sendMessage(ChatColor.GREEN + "Imported world " + world.getName());
                else sender.sendMessage(ChatColor.RED + "Failed to import world.");
                return true;
            }
            case "delete" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /world delete <name>");
                    return true;
                }
                boolean ok = manager.deleteWorld(args[1]);
                if (ok) sender.sendMessage(ChatColor.YELLOW + "World deleted: " + args[1]);
                else sender.sendMessage(ChatColor.RED + "Failed to delete world.");
                return true;
            }
            case "unload" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /world unload <name>");
                    return true;
                }
                boolean ok = manager.unloadWorld(args[1]);
                if (ok) sender.sendMessage(ChatColor.YELLOW + "World unloaded: " + args[1]);
                else sender.sendMessage(ChatColor.RED + "Failed to unload world.");
                return true;
            }
            case "tp" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /world tp <name>");
                    return true;
                }
                boolean ok = manager.teleport(player, args[1]);
                if (!ok) player.sendMessage(ChatColor.RED + "World not found.");
                return true;
            }
            case "spawn" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only");
                    return true;
                }
                String worldName = args.length >= 2 ? args[1] : player.getWorld().getName();
                boolean ok = manager.teleport(player, worldName);
                if (!ok) player.sendMessage(ChatColor.RED + "World not found.");
                return true;
            }
            case "setspawn" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only");
                    return true;
                }
                World world = player.getWorld();
                if (args.length >= 2) {
                    world = Bukkit.getWorld(args[1]);
                    if (world == null) {
                        player.sendMessage(ChatColor.RED + "World not found.");
                        return true;
                    }
                }
                manager.setSpawn(world, player.getLocation());
                player.sendMessage(ChatColor.YELLOW + "Spawn set for " + world.getName());
                return true;
            }
            case "info" -> {
                World world;
                if (args.length >= 2) {
                    world = Bukkit.getWorld(args[1]);
                    if (world == null) {
                        sender.sendMessage(ChatColor.RED + "World not found.");
                        return true;
                    }
                } else if (sender instanceof Player p) {
                    world = p.getWorld();
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /world info <name>");
                    return true;
                }

                sender.sendMessage(ChatColor.GOLD + "World: " + ChatColor.AQUA + world.getName());
                sender.sendMessage(ChatColor.GRAY + "Environment: " + world.getEnvironment());
                sender.sendMessage(ChatColor.GRAY + "Type: " + world.getWorldType());
                sender.sendMessage(ChatColor.GRAY + "Difficulty: " + world.getDifficulty());
                sender.sendMessage(ChatColor.GRAY + "Seed: " + world.getSeed());
                sender.sendMessage(ChatColor.GRAY + "Time: " + world.getTime());
                sender.sendMessage(ChatColor.GRAY + "Spawn: " + LocationUtils.locationToString(world.getSpawnLocation()));
                sender.sendMessage(ChatColor.GRAY + "Players: " + world.getPlayers().size());
                return true;
            }
            case "edit" -> {
                if (args.length < 4) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                            "Usage: /world edit <world> maxbuildheight <height>");
                    return true;
                }
                World world = Bukkit.getWorld(args[1]);
                if (world == null) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "World not found.");
                    return true;
                }
                String setting = args[2].toLowerCase(Locale.ROOT);
                if (!setting.equals("maxbuildheight")) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                            "Usage: /world edit <world> maxbuildheight <height>");
                    return true;
                }
                int height;
                try {
                    height = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "Height must be a number.");
                    return true;
                }
                boolean applied = manager.setMaxBuildHeight(world, height);
                if (applied) {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS,
                            "Updated max build height for " + world.getName() + " to " + manager.getConfiguredMaxBuildHeight(world) + ".");
                } else {
                    ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.WARNING,
                            "Saved max build height for " + world.getName() + " to " + manager.getConfiguredMaxBuildHeight(world)
                                    + ". Runtime changes are not supported on this server.");
                }
                return true;
            }
            case "gamerule" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /world gamerule <rule> <true|false>");
                    return true;
                }
                GameRule<?> rule = GameRule.getByName(args[1]);
                if (rule == null) {
                    sender.sendMessage(ChatColor.RED + "Unknown gamerule: " + args[1]);
                    return true;
                }
                if (rule.getType() != Boolean.class) {
                    sender.sendMessage(ChatColor.RED + "Only boolean gamerules are supported.");
                    return true;
                }
                String valueToken = args[2].toLowerCase(Locale.ROOT);
                if (!valueToken.equals("true") && !valueToken.equals("false")) {
                    sender.sendMessage(ChatColor.RED + "Usage: /world gamerule <rule> <true|false>");
                    return true;
                }
                @SuppressWarnings("unchecked")
                GameRule<Boolean> boolRule = (GameRule<Boolean>) rule;
                boolean value = Boolean.parseBoolean(valueToken);
                manager.applyBooleanGameRuleToAll(boolRule, value);
                sender.sendMessage(ChatColor.YELLOW + "Set " + rule.getName() + " to " + value + " for all worlds.");
                return true;
            }
            case "list" -> {
                if (sender instanceof Player p) {
                    TextComponent base = new TextComponent(ChatColor.GREEN + "Worlds: ");
                    boolean first = true;
                    for (World w : manager.listWorlds()) {
                        if (!first) base.addExtra(ChatColor.GRAY + ", ");
                        TextComponent name = new TextComponent(ChatColor.AQUA + w.getName());
                        name.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/world tp " + w.getName()));
                        name.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new ComponentBuilder("Teleport to " + w.getName()).create()));
                        base.addExtra(name);
                        first = false;
                    }
                    p.spigot().sendMessage(base);
                } else {
                    StringBuilder sb = new StringBuilder("Worlds: ");
                    for (World w : manager.listWorlds()) {
                        sb.append(w.getName()).append(' ');
                    }
                    sender.sendMessage(sb.toString().trim());
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) return List.of();

        if (args.length == 1) {
            List<String> subs = List.of("create", "clone", "copy", "import", "delete", "unload", "tp", "spawn", "setspawn", "info", "edit", "gamerule", "list");
            return filter(args[0], subs);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "tp", "spawn", "setspawn", "info", "unload", "delete" -> {
                return filter(args[1], manager.listWorlds().stream().map(World::getName).collect(Collectors.toList()));
            }
            case "clone", "copy" -> {
                if (args.length == 2) {
                    return filter(args[1], manager.listWorlds().stream().map(World::getName).collect(Collectors.toList()));
                }
            }
            case "edit" -> {
                if (args.length == 2) {
                    return filter(args[1], manager.listWorlds().stream().map(World::getName).collect(Collectors.toList()));
                }
                if (args.length == 3) {
                    return filter(args[2], List.of("maxbuildheight"));
                }
            }
            case "create" -> {
                if (args.length == 3) {
                    return filter(args[2], List.of("void", "flatland", "nether", "end"));
                }
            }
            case "gamerule" -> {
                if (args.length == 2) {
                    List<String> rules = new ArrayList<>();
                    for (GameRule<?> rule : GameRule.values()) {
                        rules.add(rule.getName());
                    }
                    return filter(args[1], rules);
                }
                if (args.length == 3) {
                    return filter(args[2], List.of("true", "false"));
                }
            }
            case "import" -> {
                java.io.File[] dirs = sender.getServer().getWorldContainer().listFiles(java.io.File::isDirectory);
                List<String> names = new ArrayList<>();
                if (dirs != null) {
                    for (java.io.File dir : dirs) {
                        if (new java.io.File(dir, "level.dat").exists()) {
                            names.add(dir.getName());
                        }
                    }
                }
                return filter(args[1], names);
            }
            default -> {
                return List.of();
            }
        }
        return List.of();
    }

    private List<String> filter(String token, List<String> options) {
        String lower = token.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String opt : options) {
            if (opt.toLowerCase(Locale.ROOT).startsWith(lower)) {
                matches.add(opt);
            }
        }
        return matches;
    }
}
