package me.nakilex.levelplugin.quests.commands;

import me.nakilex.levelplugin.quests.gui.QuestGUI;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.data.QuestRepeatType;
import me.nakilex.levelplugin.utils.CommandUtil;

public class QuestCommand implements TabExecutor {
    private final QuestManager questManager;

    public QuestCommand(QuestManager questManager) {
        this.questManager = questManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player p) {
                QuestGUI.openQuestGUI(p, questManager);
            } else {
                sender.sendMessage("This command must be run by a player to open the GUI.");
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("start") && args.length >= 2 && sender instanceof Player p) {
            questManager.startQuest(p, args[1]);
            return true;
        }

        if (sub.equals("reset") && args.length >= 3) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("Player not found: " + args[1]);
                return true;
            }
            questManager.resetQuest(target.getUniqueId(), args[2]);
            sender.sendMessage("Reset quest " + args[2] + " for " + target.getName());
            return true;
        }

        if (sub.equals("complete") && args.length >= 3) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("Player not found: " + args[1]);
                return true;
            }
            questManager.completeQuest(target.getUniqueId(), args[2]);
            sender.sendMessage("Completed quest " + args[2] + " for " + target.getName());
            return true;
        }

        if (sub.equals("status") && args.length >= 3) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("Player not found: " + args[1]);
                return true;
            }
            sender.sendMessage(questManager.getQuestStatus(target.getUniqueId(), args[2]));
            return true;
        }

        if (sub.equals("debug")) {
            boolean enabled = questManager.toggleDebug();
            sender.sendMessage("Quest debug mode " + (enabled ? "enabled" : "disabled"));
            return true;
        }

        if (sub.equals("resetdailies")) {
            int cleared = questManager.resetRepeatableProgress(QuestRepeatType.DAILY);
            sender.sendMessage("Cleared " + cleared + " daily quest completion records.");
            return true;
        }

        if (sub.equals("resetweeklies")) {
            int cleared = questManager.resetRepeatableProgress(QuestRepeatType.WEEKLY);
            sender.sendMessage("Cleared " + cleared + " weekly quest completion records.");
            return true;
        }

        sender.sendMessage("Usage: /quest [start|reset|complete|status|debug|resetdailies|resetweeklies]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtil.filterStartingWith(List.of("start", "reset", "complete", "status", "debug",
                    "resetdailies", "resetweeklies"), args[0]);
        }
        String sub = args[0].toLowerCase();
        if (args.length == 2) {
            switch (sub) {
                case "start":
                    return CommandUtil.filterStartingWith(questManager.getQuests().stream()
                            .map(Quest::getId).toList(), args[1]);
                case "reset":
                case "complete":
                case "status":
                    return CommandUtil.onlinePlayerNames(args[1]);
                default:
                    break;
            }
        }
        if (args.length == 3) {
            switch (sub) {
                case "reset":
                case "complete":
                case "status":
                    return CommandUtil.filterStartingWith(questManager.getQuests().stream()
                            .map(Quest::getId).toList(), args[2]);
                default:
                    break;
            }
        }
        return Collections.emptyList();
    }
}
