package me.nakilex.levelplugin.player.battlepass.command;

import me.nakilex.levelplugin.player.battlepass.BattlePassManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Simple command that opens the battle pass menu for players.
 */
public class BattlePassCommand implements CommandExecutor {

    private final BattlePassManager manager;

    public BattlePassCommand(BattlePassManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, MessageType.ERROR, "Only players can use this command.");
            return true;
        }
        manager.openMenu(player);
        return true;
    }
}
