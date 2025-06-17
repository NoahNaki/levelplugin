package me.nakilex.levelplugin.friend;

import me.nakilex.levelplugin.utils.ToggleFeedbackUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Command to toggle the friend glow feature. */
public class FriendGlowCommand implements CommandExecutor {
    private final FriendGlowManager glowManager;

    public FriendGlowCommand(FriendGlowManager glowManager) {
        this.glowManager = glowManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        boolean enabled = glowManager.toggle(player);
        ToggleFeedbackUtil.sendToggle(player, "Friend glow", enabled);
        return true;
    }
}
