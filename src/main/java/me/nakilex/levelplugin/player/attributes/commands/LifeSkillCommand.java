package me.nakilex.levelplugin.player.attributes.commands;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.tools.ToolDiscipline;
import me.nakilex.levelplugin.player.farming.managers.FarmingManager;
import me.nakilex.levelplugin.player.fishing.managers.FishingManager;
import me.nakilex.levelplugin.player.mining.managers.MiningManager;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class LifeSkillCommand implements TabExecutor {

    private final Main plugin;

    public LifeSkillCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("addxp".equals(sub)) {
            if (args.length < 4) {
                sendUsage(sender);
                return true;
            }
            ToolDiscipline discipline = parseDiscipline(args[1]);
            if (discipline == null) {
                sendUnknownSkill(sender, args[1]);
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage("§cCould not find player " + args[2]);
                return true;
            }
            Integer amount = parseAmount(sender, args[3]);
            if (amount == null) {
                return true;
            }
            grantXp(sender, discipline, target, amount);
            return true;
        }
        if ("reset".equals(sub)) {
            if (args.length < 3) {
                sendUsage(sender);
                return true;
            }
            ToolDiscipline discipline = parseDiscipline(args[1]);
            if (discipline == null) {
                sendUnknownSkill(sender, args[1]);
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage("§cCould not find player " + args[2]);
                return true;
            }
            resetSkill(sender, discipline, target);
            return true;
        }
        sendUsage(sender);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtil.simpleSuggestions(args[0], "addxp", "reset");
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && ("addxp".equals(sub) || "reset".equals(sub))) {
            return CommandUtil.filterStartingWith(skillKeys(), args[1]);
        }
        if (args.length == 3 && ("addxp".equals(sub) || "reset".equals(sub))) {
            return CommandUtil.onlinePlayerNames(args[2]);
        }
        if (args.length == 4 && "addxp".equals(sub)) {
            return CommandUtil.numberOptions(args[3], 1, 5, 10, 25, 50, 100, 250, 500, 1000);
        }
        return Collections.emptyList();
    }

    public static boolean handleLegacyAddXp(CommandSender sender, ToolDiscipline discipline, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /lifeskill addxp <lifeskill> <player> <amount>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("§cCould not find player " + args[0]);
            return true;
        }
        Integer amount = parseAmount(sender, args[1]);
        if (amount == null) {
            return true;
        }
        grantXp(sender, discipline, target, amount);
        return true;
    }

    private static void grantXp(CommandSender sender, ToolDiscipline discipline, Player target, int amount) {
        switch (discipline) {
            case MINING -> MiningManager.getInstance().addXP(target, amount);
            case FARMING -> FarmingManager.getInstance().addXP(target, amount);
            case FISHING -> FishingManager.getInstance().addXP(target, amount);
        }
        String expLabel = ChatFormatter.experienceLabel();
        String expColor = ChatFormatter.experienceColor();
        String skillName = formatSkillName(discipline);
        sender.sendMessage("§aGave " + expColor + amount + " " + skillName + " <glyph:experience_orb_icon> " + expLabel
                + " §ato " + target.getName());
        target.sendMessage("§aYou have received " + expColor + amount + " " + skillName + " <glyph:experience_orb_icon> "
                + expLabel + "!");
    }

    private void resetSkill(CommandSender sender, ToolDiscipline discipline, Player target) {
        switch (discipline) {
            case MINING -> MiningManager.getInstance().setLevel(target.getUniqueId(), 1);
            case FARMING -> FarmingManager.getInstance().setLevel(target.getUniqueId(), 1);
            case FISHING -> FishingManager.getInstance().setLevel(target.getUniqueId(), 1);
        }
        switch (discipline) {
            case MINING -> MiningManager.getInstance().addXP(target, 0);
            case FARMING -> FarmingManager.getInstance().addXP(target, 0);
            case FISHING -> FishingManager.getInstance().addXP(target, 0);
        }
        if (plugin.getPlayerConfig() != null) {
            plugin.getPlayerConfig().savePlayerData(target.getUniqueId());
        }
        String message = "Reset " + ChatColor.YELLOW + formatSkillName(discipline) + ChatColor.WHITE
                + " for " + ChatColor.YELLOW + target.getName() + ChatColor.WHITE + ".";
        if (sender instanceof Player player) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, message);
        } else {
            sender.sendMessage(message);
        }
    }

    private static Integer parseAmount(CommandSender sender, String raw) {
        int amount;
        try {
            amount = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid amount: " + raw);
            return null;
        }
        if (amount <= 0) {
            sender.sendMessage("§cPlease specify a positive integer amount.");
            return null;
        }
        return amount;
    }

    private static ToolDiscipline parseDiscipline(String raw) {
        if (raw == null) return null;
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "mining" -> ToolDiscipline.MINING;
            case "farming" -> ToolDiscipline.FARMING;
            case "fishing" -> ToolDiscipline.FISHING;
            default -> null;
        };
    }

    private static List<String> skillKeys() {
        return List.of("mining", "farming", "fishing");
    }

    private static String formatSkillName(ToolDiscipline discipline) {
        String name = discipline.name().toLowerCase(Locale.ROOT);
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Usage:");
        sender.sendMessage(ChatColor.RED + "/lifeskill addxp <lifeskill> <player> <amount>");
        sender.sendMessage(ChatColor.RED + "/lifeskill reset <lifeskill> <player>");
    }

    private void sendUnknownSkill(CommandSender sender, String raw) {
        sender.sendMessage(ChatColor.RED + "Unknown lifeskill '" + raw + "'. Use mining, farming, or fishing.");
    }
}
