package me.nakilex.levelplugin.tower;

import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command entry point for the infinite tower.
 */
public class TowerCommand implements CommandExecutor {

    private final TowerGUI towerGUI;
    private final TowerManager towerManager;

    public TowerCommand(TowerGUI towerGUI, TowerManager towerManager) {
        this.towerGUI = towerGUI;
        this.towerManager = towerManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "Only players can use this command.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("exit")) {
            towerManager.exit(player);
            return true;
        }

        towerGUI.open(player);
        return true;
    }
}
