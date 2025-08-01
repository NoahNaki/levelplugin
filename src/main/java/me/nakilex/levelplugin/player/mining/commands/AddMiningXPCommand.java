package me.nakilex.levelplugin.player.mining.commands;

import me.nakilex.levelplugin.player.mining.managers.MiningManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import me.nakilex.levelplugin.utils.ChatFormatter;
import org.bukkit.entity.Player;

public class AddMiningXPCommand implements CommandExecutor {

    private final MiningManager miningManager;

    public AddMiningXPCommand(MiningManager miningManager) {
        this.miningManager = miningManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /addminingxp <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("§cCould not find player " + args[0]);
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid amount: " + args[1]);
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage("§cPlease specify a positive integer amount.");
            return true;
        }

        miningManager.addXP(target, amount);
        String expLabel = me.nakilex.levelplugin.utils.ChatFormatter.experienceLabel();
        sender.sendMessage("§aGave " + amount + " Mining <glyph:experience_orb_icon> " + expLabel + " §ato " + target.getName());
        target.sendMessage("§aYou have received " + amount + " Mining <glyph:experience_orb_icon> " + expLabel + "!");
        return true;
    }
}
