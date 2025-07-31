package me.nakilex.levelplugin.world;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WorldCommand implements CommandExecutor {
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
            case "list" -> {
                StringBuilder sb = new StringBuilder(ChatColor.GREEN + "Worlds: ");
                for (World w : manager.listWorlds()) {
                    sb.append(w.getName()).append(" ");
                }
                sender.sendMessage(sb.toString());
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
