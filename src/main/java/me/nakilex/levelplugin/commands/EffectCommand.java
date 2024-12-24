// EffectCommand.java
package me.nakilex.levelplugin.commands;

import me.nakilex.levelplugin.effects.EffectManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EffectCommand implements CommandExecutor {

    private final EffectManager effectManager;

    public EffectCommand(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        // Toggle the sword circle effect
        if (effectManager.hasActiveEffect(player)) {
            effectManager.stopSwordCircleEffect(player);
            player.sendMessage("Effect stopped.");
        } else {
            effectManager.startSwordCircleEffect(player);
            player.sendMessage("Effect started.");
        }

        return true;
    }
}
