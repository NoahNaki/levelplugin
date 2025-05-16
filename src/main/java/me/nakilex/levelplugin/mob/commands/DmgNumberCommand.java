package me.nakilex.levelplugin.mob.commands;

import me.nakilex.levelplugin.mob.managers.DmgNumberToggleManager;
import me.nakilex.levelplugin.utils.ToggleFeedbackUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DmgNumberCommand implements CommandExecutor {
    private final DmgNumberToggleManager toggleManager;

    public DmgNumberCommand(DmgNumberToggleManager toggleManager) {
        this.toggleManager = toggleManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can toggle damage numbers.");
            return true;
        }
        Player player = (Player) sender;
        boolean nowEnabled = toggleManager.toggle(player);

        // Centralized, styled feedback:
        ToggleFeedbackUtil.sendToggle(player, "Damage numbers", nowEnabled);
        return true;
    }
}
