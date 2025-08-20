package me.nakilex.levelplugin.player.classes.commands;

import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.player.classes.essence.gui.ClassEssenceUpgradeGUI;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.utils.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Command to open the paginated essence upgrade menu. */
public class EssenceUpgradeCommand implements TabExecutor {
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = Arrays.stream(ItemRarity.values())
                    .map(r -> r.name().toLowerCase(Locale.ROOT))
                    .toList();
            return CommandUtil.filterStartingWith(names, args[0]);
        }
        return Collections.emptyList();
    }
}

