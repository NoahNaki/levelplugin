package me.nakilex.levelplugin.party;

import me.nakilex.levelplugin.utils.ToggleFeedbackUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Command to toggle the party glow feature. */
public class PartyGlowCommand implements CommandExecutor {
    private final PartyGlowManager glowManager;

    public PartyGlowCommand(PartyGlowManager glowManager) {
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
        ToggleFeedbackUtil.sendToggle(player, "Party glow", enabled);
        return true;
    }
}
