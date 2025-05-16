package me.nakilex.levelplugin.mob.commands;

import me.nakilex.levelplugin.mob.managers.ChatToggleManager;
import me.nakilex.levelplugin.utils.ToggleFeedbackUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DmgChatCommand implements CommandExecutor {
    private final ChatToggleManager chatToggle = ChatToggleManager.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can toggle damage chat.");
            return true;
        }
        Player player = (Player) sender;
        boolean nowEnabled = chatToggle.toggle(player);

        ToggleFeedbackUtil.sendToggle(player, "Damage chat", nowEnabled);
        return true;
    }
}
