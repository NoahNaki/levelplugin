package me.nakilex.levelplugin.player.classes.commands;

import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.player.classes.essence.gui.ClassEssenceUpgradeGUI;
import me.nakilex.levelplugin.items.data.ItemRarity;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Command to open the paginated essence upgrade menu. */
public class EssenceUpgradeCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players may use this command.");
            return true;
        }
        if (args.length > 0) {
            try {
                ItemRarity rarity = ItemRarity.valueOf(args[0].toUpperCase());
                player.getInventory().addItem(ClassEssence.generateRandomEssence(rarity));
            } catch (IllegalArgumentException ex) {
                player.sendMessage("§cUnknown rarity.");
            }
        }
        ClassEssenceUpgradeGUI.openInvest(player, null);
        return true;
    }
}

