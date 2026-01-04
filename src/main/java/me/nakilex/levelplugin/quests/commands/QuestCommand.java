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
import me.nakilex.levelplugin.quests.gui.QuestState;
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

        if (sub.equals("start") && args.length >= 2) {
            Player target = null;
            if (args.length >= 3) {
                target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage("Player not found: " + args[2]);
                    return true;
                }
            } else if (sender instanceof Player p) {
                target = p;
            } else {
                sender.sendMessage("Usage: /quest start <questId> <player>");
                return true;
            }
            questManager.startQuest(target, args[1]);
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

        if (sub.equals("track") && args.length >= 3) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("Player not found: " + args[1]);
                return true;
            }
            Quest quest = questManager.getQuestById(args[2]);
            if (quest == null) {
                sender.sendMessage("Quest not found: " + args[2]);
                return true;
            }
            QuestState state = questManager.getQuestState(target, quest);
            if (state != QuestState.AVAILABLE && state != QuestState.ACCEPTED
                    && state != QuestState.IN_PROGRESS && state != QuestState.TURN_IN_READY) {
                sender.sendMessage("Quest is not available or accepted for " + target.getName() + ".");
                return true;
            }
            questManager.setTrackedQuest(target, quest.getId());
            sender.sendMessage("Tracking quest " + quest.getId() + " for " + target.getName());
            target.sendMessage("§aTracking quest: §f" + quest.getName());
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

        sender.sendMessage("Usage: /quest [start|reset|complete|status|track|debug|resetdailies|resetweeklies]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtil.filterStartingWith(List.of("start", "reset", "complete", "status", "track", "debug",
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
                case "track":
                    return CommandUtil.onlinePlayerNames(args[1]);
                default:
                    break;
            }
        }
        if (args.length == 3) {
            switch (sub) {
                case "start":
                    return CommandUtil.onlinePlayerNames(args[2]);
                case "reset":
                case "complete":
                case "status":
                    return CommandUtil.filterStartingWith(questManager.getQuests().stream()
                            .map(Quest::getId).toList(), args[2]);
                case "track":
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        return Collections.emptyList();
                    }
                    List<String> trackable = questManager.getQuests().stream()
                            .filter(quest -> {
                                QuestState state = questManager.getQuestState(target, quest);
                                return state == QuestState.AVAILABLE || state == QuestState.ACCEPTED
                                        || state == QuestState.IN_PROGRESS || state == QuestState.TURN_IN_READY;
                            })
                            .map(Quest::getId)
                            .toList();
                    return CommandUtil.filterStartingWith(trackable, args[2]);
                default:
                    break;
            }
        }
        return Collections.emptyList();
    }
}
