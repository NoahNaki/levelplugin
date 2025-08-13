package me.nakilex.levelplugin.environment;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import me.nakilex.levelplugin.guild.Guild;
import me.nakilex.levelplugin.guild.GuildManager;
import me.nakilex.levelplugin.guild.siege.GuildSiegeManager;

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

        String owner = GuildSiegeManager.getInstance().getOwnerGuild();
        if (owner != null) {
            Guild g = GuildManager.getInstance().getGuild(p.getUniqueId());
            if (g == null || !owner.equalsIgnoreCase(g.getName())) {
                p.sendMessage(ChatColor.RED + "Your guild does not control this town.");
                return true;
            }
        }
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("start")) {
                if (args.length < 2) {
                    p.sendMessage(ChatColor.RED + "Usage: /town start <name>");
                    return true;
                }
                manager.startTown(p, args[1].toLowerCase());
                return true;
            } else if (args[0].equalsIgnoreCase("reset")) {
                manager.resetTown(p);
                return true;
            } else if (args[0].equalsIgnoreCase("invite") && args.length >= 2) {
                Player target = p.getServer().getPlayer(args[1]);
                if (target != null) {
                    manager.invite(p, target);
                } else {
                    p.sendMessage(ChatColor.RED + "Player not found.");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("accept")) {
                manager.accept(p);
                return true;
            } else if (args[0].equalsIgnoreCase("deny")) {
                manager.deny(p);
                return true;
            } else if (args[0].equalsIgnoreCase("kick") && args.length >= 2) {
                Player target = p.getServer().getPlayer(args[1]);
                if (target != null) {
                    manager.kick(p, target);
                } else {
                    p.sendMessage(ChatColor.RED + "Player not found.");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("leave")) {
                manager.leave(p);
                return true;
            } else if (args[0].equalsIgnoreCase("transfer") && args.length >= 2) {
                Player target = p.getServer().getPlayer(args[1]);
                if (target != null) {
                    manager.transfer(p, target);
                } else {
                    p.sendMessage(ChatColor.RED + "Player not found.");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("info")) {
                manager.sendInfo(p);
                return true;
            }
        }
        gui.open(p);
        return true;
    }
}
