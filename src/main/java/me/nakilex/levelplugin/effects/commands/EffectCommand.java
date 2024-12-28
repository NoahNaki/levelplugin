package me.nakilex.levelplugin.effects.commands;

import me.nakilex.levelplugin.effects.managers.EffectManager;
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

        if (args.length > 0) {
            String effectType = args[0].toLowerCase();
            effectManager.stopEffect(player); // Stop any active effect first

            switch (effectType) {
                case "circlesword":
                    effectManager.startSwordCircleEffect(player);
                    player.sendMessage("Started Circle Sword Effect!");
                    break;
                case "swordfire":
                    effectManager.startSwordFireEffect(player);
                    player.sendMessage("Started Sword Fire Effect!");
                    break;
                default:
                    player.sendMessage("Invalid effect type! Use circlesword or swordfire.");
            }
        } else {
            player.sendMessage("Usage: /effect <circlesword|swordfire>");
        }
        return true;
    }
}
