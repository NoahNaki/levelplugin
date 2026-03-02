package me.nakilex.levelplugin.player.classes.commands;

import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.CommandUtil;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.gui.dynamic.DynamicMenuManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClassCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reloadmenu")) {
            if (!sender.hasPermission("levelplugin.class.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to do that.");
                return true;
            }
            DynamicMenuManager.getInstance(Main.getInstance()).forceReload();
            sender.sendMessage(ChatColor.GREEN + "Reloaded dynamic_menus.yml");
            return true;
        }

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
            applyClassSelection(target, chosen);
            sender.sendMessage(ChatColor.GREEN + "Class for " + target.getName() + " set to " + chosen.name());
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use /class");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            boolean opened = DynamicMenuManager.getInstance(Main.getInstance()).openMenu(player, "class_select");
            if (!opened) {
                player.sendMessage(ChatColor.RED + "Class menu is not configured. Ask an admin to check dynamic_menus.yml.");
            }
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /class <Mage|Archer|Rogue|Warrior|Cleric>");
            return true;
        }

        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        if (ps.playerClass != PlayerClass.VILLAGER) {
            player.sendMessage(ChatColor.RED + "You have already chosen the " + ps.playerClass.name() + " class.");
            return true;
        }

        PlayerClass chosen = PlayerClass.fromString(args[0]);
        if (chosen == null) {
            player.sendMessage(ChatColor.RED + "Unknown class: " + args[0]);
            return true;
        }
        if (chosen != PlayerClass.MAGE && chosen != PlayerClass.ARCHER
                && chosen != PlayerClass.ROGUE && chosen != PlayerClass.WARRIOR
                && chosen != PlayerClass.CLERIC) {
            player.sendMessage(ChatColor.RED + "You cannot select that class with /class.");
            return true;
        }

        applyClassSelection(player, chosen);
        return true;
    }

    private void applyClassSelection(Player player, PlayerClass chosen) {
        StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        stats.playerClass = chosen;
        stats.unlockedClasses.add(chosen);

        boolean flight = chosen == PlayerClass.ARCHER || chosen == PlayerClass.ROGUE;
        player.setAllowFlight(flight);
        if (!flight) {
            player.setFlying(false);
        }

        ChatFormatter.constructDivider(player, "§6§l-", 45);
        ChatFormatter.sendCenteredMessage(player, "§6§lCLASS SELECTED!");
        ChatFormatter.sendCenteredMessage(player, "");
        ChatFormatter.sendCenteredMessage(player,
                "§7You are now the §e§l" + chosen.name() + " §7class!");
        ChatFormatter.sendCenteredMessage(player, "");
        ChatFormatter.constructDivider(player, "§6§l-", 45);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.closeInventory();
        Main.getInstance().getQuestManager().handleClassSelect(player);
        ItemUtil.refreshTooltips(player);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("reloadmenu", "unlock", "unlockall", "lock", "admin",
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
