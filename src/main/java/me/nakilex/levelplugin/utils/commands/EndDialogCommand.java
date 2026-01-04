package me.nakilex.levelplugin.utils.commands;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class EndDialogCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("§cUsage: /enddialog <player>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage("§cPlayer not found.");
            return true;
        }
        NPCDialogManager dialogManager = Main.getInstance().getDialogManager();
        if (dialogManager == null) {
            sender.sendMessage("§cDialog manager is unavailable.");
            return true;
        }
        dialogManager.resetDialog(target);
        ChatMessageUtil.send(target, ChatMessageUtil.MessageType.INFO,
                "Dialog ended. You can cast skills again.");
        sender.sendMessage("§aEnded dialog for " + target.getName() + ".");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix))
                .sorted()
                .collect(Collectors.toList());
    }
}
