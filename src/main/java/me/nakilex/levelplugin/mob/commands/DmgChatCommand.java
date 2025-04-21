package me.nakilex.levelplugin.mob.commands;

import me.nakilex.levelplugin.mob.managers.ChatToggleManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DmgChatCommand implements CommandExecutor {

    private final ChatToggleManager chatToggle = ChatToggleManager.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can toggle damage‑chat.");
            return true;
        }

        Player p = (Player) sender;
        boolean now = chatToggle.toggle(p);
        p.sendMessage(
            now
                ? ChatColor.GREEN + "Damage chat: ON"
                : ChatColor.RED   + "Damage chat: OFF"
        );
        return true;
    }
}
