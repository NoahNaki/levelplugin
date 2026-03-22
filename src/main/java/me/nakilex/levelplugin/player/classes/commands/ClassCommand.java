package me.nakilex.levelplugin.player.classes.commands;

import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.ClassSelectionUtil;
import me.nakilex.levelplugin.player.classes.gui.ClassSelectionGUI;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.utils.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClassCommand implements TabExecutor {
    private static final ClassSelectionGUI CLASS_SELECTION_GUI = new ClassSelectionGUI();

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
            PlayerClass cls = PlayerClass.fromString(args[2]);
            if (cls == null) {
                sender.sendMessage(ChatColor.RED + "Unknown class: " + args[2]);
                return true;
            }
            StatsManager.getInstance().unlockClass(target.getUniqueId(), cls);
            sender.sendMessage(ChatColor.GREEN + "Unlocked " + cls.name() + " for " + target.getName());
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("unlockall")) {
            if (args.length != 2) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /class unlockall <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                return true;
            }
            StatsManager.getInstance().unlockAllClasses(target.getUniqueId());
            sender.sendMessage(ChatColor.GREEN + "Unlocked all classes for " + target.getName());
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
            PlayerClass cls = PlayerClass.fromString(args[2]);
            if (cls == null) {
                sender.sendMessage(ChatColor.RED + "Unknown class: " + args[2]);
                return true;
            }
            StatsManager.getInstance().lockClass(target.getUniqueId(), cls);
            sender.sendMessage(ChatColor.GREEN + "Locked " + cls.name() + " for " + target.getName());
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
            PlayerClass chosen = PlayerClass.fromString(args[2]);
            if (chosen == null) {
                sender.sendMessage(ChatColor.RED + "Unknown class: " + args[2]);
                return true;
            }
            if (!ClassSelectionUtil.isSelectableBaseClass(chosen)) {
                sender.sendMessage(ChatColor.RED + "You cannot set that class with /class admin.");
                return true;
            }
            ClassSelectionUtil.applyClassSelection(target, chosen, true);
            sender.sendMessage(ChatColor.GREEN + "Class for " + target.getName() + " set to " + chosen.name());
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use /class");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            CLASS_SELECTION_GUI.open(player);
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /class <Mage|Archer|Rogue|Warrior|Cleric>");
            return true;
        }

        PlayerClass chosen = PlayerClass.fromString(args[0]);
        if (chosen == null) {
            player.sendMessage(ChatColor.RED + "Unknown class: " + args[0]);
            return true;
        }
        if (!ClassSelectionUtil.isSelectableBaseClass(chosen)) {
            player.sendMessage(ChatColor.RED + "You cannot select that class with /class.");
            return true;
        }
        ClassSelectionUtil.applyClassSelection(player, chosen, true);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("unlock", "unlockall", "lock", "admin",
                    "mage", "archer", "rogue", "warrior", "cleric"));
            return CommandUtil.filterStartingWith(options, args[0]);
        } else if (args.length == 2) {
            String first = args[0].toLowerCase();
            if (first.equals("unlock") || first.equals("unlockall") || first.equals("lock") || first.equals("admin")) {
                return CommandUtil.onlinePlayerNames(args[1]);
            }
        } else if (args.length == 3) {
            String first = args[0].toLowerCase();
            if (first.equals("unlock") || first.equals("lock") || first.equals("admin")) {
                List<String> classes = Arrays.stream(PlayerClass.values())
                        .map(pc -> pc.name().toLowerCase())
                        .toList();
                return CommandUtil.filterStartingWith(classes, args[2]);
            }
        }
        return List.of();
    }
}
