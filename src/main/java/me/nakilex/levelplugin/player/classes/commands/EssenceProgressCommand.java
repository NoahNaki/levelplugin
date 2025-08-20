package me.nakilex.levelplugin.player.classes.commands;

import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.player.classes.essence.gui.ClassEssenceProgressGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Opens the essence progression GUI for the item in hand. */
public class EssenceProgressCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players may use this command.");
            return true;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!ClassEssence.isEssence(hand)) {
            player.sendMessage("§cHold a class essence in your hand.");
            return true;
        }
        ClassEssenceProgressGUI.open(player, hand);
        return true;
    }
}

