package me.nakilex.levelplugin.pet.commands;

import me.nakilex.levelplugin.pet.gui.PetSummonGUI;
import me.nakilex.levelplugin.pet.utils.PetChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PetSummonCommand implements CommandExecutor {
    private final PetSummonGUI summonGUI;

    public PetSummonCommand(PetSummonGUI summonGUI) {
        this.summonGUI = summonGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (summonGUI == null) {
            PetChatUtil.send(player, "Pet summons are currently unavailable.");
            return true;
        }
        summonGUI.open(player);
        return true;
    }
}
