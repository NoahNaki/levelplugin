package me.nakilex.levelplugin.environment;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

public class TownCommand implements CommandExecutor {
    private final UpgradeGUI gui;
    private final EnvironmentManager manager;

    public TownCommand(UpgradeGUI gui, EnvironmentManager manager) {
        this.gui = gui;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "start" -> {
                    if (args.length < 2) {
                        p.sendMessage(ChatColor.RED + "Usage: /town start <name>");
                        return true;
                    }
                    manager.startTown(p, args[1].toLowerCase());
                }
                case "reset" -> manager.resetTown(p);
                case "invite" -> {
                    if (args.length < 2) {
                        p.sendMessage(ChatColor.RED + "Usage: /town invite <player>");
                        return true;
                    }
                    Player target = p.getServer().getPlayer(args[1]);
                    if (target == null) {
                        p.sendMessage(ChatColor.RED + "Player not found.");
                        return true;
                    }
                    manager.invite(p, target);
                }
                case "kick" -> {
                    if (args.length < 2) {
                        p.sendMessage(ChatColor.RED + "Usage: /town kick <player>");
                        return true;
                    }
                    Player target = p.getServer().getPlayer(args[1]);
                    if (target == null) {
                        p.sendMessage(ChatColor.RED + "Player not found.");
                        return true;
                    }
                    manager.kick(p, target);
                }
                case "leave" -> manager.leave(p);
                case "info" -> manager.sendInfo(p);
                default -> gui.open(p);
            }
            return true;
        }
        gui.open(p);
        return true;
    }
}
