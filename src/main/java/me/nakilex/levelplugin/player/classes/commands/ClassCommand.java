package me.nakilex.levelplugin.player.classes.commands;

import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClassCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("unlock")) {
            if (args.length != 3) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /class unlock <player> <class>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                return true;
            }
            try {
                PlayerClass cls = PlayerClass.valueOf(args[2].toUpperCase());
                StatsManager.getInstance().unlockClass(target.getUniqueId(), cls);
                sender.sendMessage(ChatColor.GREEN + "Unlocked " + cls.name() + " for " + target.getName());
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(ChatColor.RED + "Unknown class: " + args[2]);
            }
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("lock")) {
            if (args.length != 3) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /class lock <player> <class>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                return true;
            }
            try {
                PlayerClass cls = PlayerClass.valueOf(args[2].toUpperCase());
                StatsManager.getInstance().lockClass(target.getUniqueId(), cls);
                sender.sendMessage(ChatColor.GREEN + "Locked " + cls.name() + " for " + target.getName());
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(ChatColor.RED + "Unknown class: " + args[2]);
            }
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("admin")) {
            if (args.length != 3) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /class admin <player> <class>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                return true;
            }
            try {
                PlayerClass chosen = PlayerClass.valueOf(args[2].toUpperCase());
                StatsManager.PlayerStats tps = StatsManager.getInstance().getPlayerStats(target.getUniqueId());
                tps.playerClass = chosen;
                tps.unlockedClasses.add(chosen);
                boolean flight = chosen == PlayerClass.ARCHER || chosen == PlayerClass.ROGUE;
                target.setAllowFlight(flight);
                if (!flight) target.setFlying(false);
                sender.sendMessage(ChatColor.GREEN + "Class for " + target.getName() + " set to " + chosen.name());
                if (sender != target) {
                    target.sendMessage(ChatColor.GREEN + "Your class has been set to " + chosen.name());
                }
                me.nakilex.levelplugin.items.utils.ItemUtil.refreshTooltips(target);
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(ChatColor.RED + "Unknown class: " + args[2]);
            }
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use /class");
            return true;
        }

        Player player = (Player) sender;
        if (args.length != 1) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /class <Mage|Archer|Rogue|Warrior|Cleric>");
            return true;
        }

        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        if (ps.playerClass != PlayerClass.VILLAGER) {
            player.sendMessage(ChatColor.RED + "You have already chosen the " + ps.playerClass.name() + " class.");
            return true;
        }

        try {
            PlayerClass chosen = PlayerClass.valueOf(args[0].toUpperCase());
            if (chosen != PlayerClass.MAGE && chosen != PlayerClass.ARCHER
                    && chosen != PlayerClass.ROGUE && chosen != PlayerClass.WARRIOR
                    && chosen != PlayerClass.CLERIC) {
                player.sendMessage(ChatColor.RED + "You cannot select that class with /class.");
                return true;
            }

            ps.playerClass = chosen;
            ps.unlockedClasses.add(chosen);
            boolean flight = chosen == PlayerClass.ARCHER || chosen == PlayerClass.ROGUE;
            player.setAllowFlight(flight);
            if (!flight) player.setFlying(false);
            player.sendMessage(ChatColor.GREEN + "Class set to " + ChatColor.AQUA + chosen.name());
            me.nakilex.levelplugin.items.utils.ItemUtil.refreshTooltips(player);
        } catch (IllegalArgumentException ex) {
            player.sendMessage(ChatColor.RED + "Unknown class: " + args[0]);
        }
        return true;
    }
}
