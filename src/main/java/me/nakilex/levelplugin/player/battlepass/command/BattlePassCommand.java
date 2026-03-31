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
        if (args.length >= 2 && args[0].equalsIgnoreCase("track")) {
            BattlePassManager.ProgressTrack track;
            try {
                track = BattlePassManager.ProgressTrack.valueOf(args[1].toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                ChatMessageUtil.send(player, MessageType.ERROR, "Invalid track. Use: balanced, combat, exploration, economy.");
                return true;
            }
            manager.setTrack(player, track);
            return true;
        }
        manager.openMenu(player);
        return true;
    }
}
