package me.nakilex.levelplugin.player.classes.commands;

import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.utils.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Command to generate a class essence with specific parameters.
 * Usage: /genclass <player> <class> <rarity> <star level>
 */
public class GenClassCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 4) {
            sender.sendMessage("§cUsage: /genclass <player> <class> <rarity> <star level>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return true;
        }
        PlayerClass clazz = PlayerClass.fromString(args[1]);
        ItemRarity rarity;
        int star;
        try {
            rarity = ItemRarity.valueOf(args[2].toUpperCase());
            star = Integer.parseInt(args[3]);
        } catch (Exception ex) {
            sender.sendMessage("§cInvalid arguments.");
            return true;
        }
        if (clazz == null) {
            sender.sendMessage("§cInvalid class.");
            return true;
        }
        star = Math.max(0, Math.min(5, star));
        ItemStack essence = ClassEssence.generateEssence(clazz, rarity, star);
        target.getInventory().addItem(essence);
        sender.sendMessage("§aGenerated " + clazz.getDisplayName() + " essence for " + target.getName());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return switch (args.length) {
            case 1 -> CommandUtil.onlinePlayerNames(args[0]);
            case 2 -> CommandUtil.filterStartingWith(
                    Arrays.stream(PlayerClass.values()).map(pc -> pc.name().toLowerCase(Locale.ROOT)).toList(),
                    args[1]);
            case 3 -> CommandUtil.filterStartingWith(
                    Arrays.stream(ItemRarity.values()).map(r -> r.name().toLowerCase(Locale.ROOT)).toList(),
                    args[2]);
            case 4 -> CommandUtil.filterStartingWith(List.of("0","1","2","3","4","5"), args[3]);
            default -> Collections.emptyList();
        };
    }
}
