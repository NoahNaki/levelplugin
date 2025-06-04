package me.nakilex.levelplugin.quests.commands;

import me.nakilex.levelplugin.quests.gui.QuestGUI;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class QuestCommand implements CommandExecutor {
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

        sender.sendMessage("Usage: /quest [start|reset|complete|status|debug]");
        return true;
    }
}
