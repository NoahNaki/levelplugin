package me.nakilex.levelplugin.player.commands;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.PlayerStats;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class ViewProfileCommand implements TabExecutor {
    private final LevelManager levelManager;

    public ViewProfileCommand(LevelManager levelManager) {
        this.levelManager = levelManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /viewprofile <player>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
            return true;
        }

        PlayerStats stats = StatsManager.getInstance().getPlayerStats(target.getUniqueId());
        int level = levelManager.getLevel(target);
        int unlockedSlots = StatsManager.getInstance().getUnlockedEssenceSlots(target);

        if (sender instanceof Player viewer) {
            ChatFormatter.constructDivider(viewer, "§8-", 45);
            ChatFormatter.sendCenteredMessage(viewer, "§6§lPROFILE: " + target.getName());
            ChatFormatter.sendCenteredMessage(viewer, "§7Class: §e" + stats.playerClass.getDisplayName()
                    + " §8| §7Level: §e" + level);
            ChatFormatter.sendCenteredMessage(viewer, "§7HP: §e" + Math.round(target.getHealth())
                    + "§7/§e" + Math.round(target.getMaxHealth())
                    + " §8| §7Mana: §e" + Math.round(stats.currentMana)
                    + "§7/§e" + Math.round(stats.maxMana));
            ChatFormatter.sendCenteredMessage(viewer, "§7Essence Slots: §e" + unlockedSlots + "§7/3");
            ChatFormatter.constructDivider(viewer, "§8-", 45);
        } else {
            sender.sendMessage("Profile: " + target.getName());
            sender.sendMessage("Class: " + stats.playerClass.getDisplayName() + " | Level: " + level);
            sender.sendMessage("HP: " + Math.round(target.getHealth()) + "/" + Math.round(target.getMaxHealth())
                    + " | Mana: " + Math.round(stats.currentMana) + "/" + Math.round(stats.maxMana));
            sender.sendMessage("Essence Slots: " + unlockedSlots + "/3");
        }

        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "STR " + statTotal(stats.baseStrength, stats.bonusStrength)
                        + " | AGI " + statTotal(stats.baseAgility, stats.bonusAgility)
                        + " | INT " + statTotal(stats.baseIntelligence, stats.bonusIntelligence)
                        + " | DEX " + statTotal(stats.baseDexterity, stats.bonusDexterity));
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "VIT " + statTotal(stats.baseVitality, stats.bonusVitality)
                        + " | CDR " + String.format("%.1f%%", stats.cooldownReduction * 100.0));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return me.nakilex.levelplugin.utils.CommandUtil.onlinePlayerNames(args[0]);
        }
        return Collections.emptyList();
    }

    private String statTotal(int base, int bonus) {
        int total = base + bonus;
        return bonus > 0 ? total + ChatColor.GRAY.toString() + " (" + base + "+" + bonus + ")" : String.valueOf(total);
    }
}
