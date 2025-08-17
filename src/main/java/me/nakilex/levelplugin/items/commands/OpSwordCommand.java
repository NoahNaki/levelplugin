package me.nakilex.levelplugin.items.commands;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.data.StatRange;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OpSwordCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players may use this command.");
            return true;
        }

        CustomItem sword = new CustomItem(
                -9999,
                "§cAdmin Sword",
                ItemRarity.MYTHIC,
                0,
                "ANY",
                Material.NETHERITE_SWORD,
                new StatRange(1000, 1000),
                new StatRange(1000, 1000),
                new StatRange(1000, 1000),
                new StatRange(1000, 1000),
                new StatRange(1000, 1000),
                new StatRange(1000, 1000),
                new StatRange(1000, 1000),
                new StatRange(1000, 1000),
                false,
                null
        );
        ItemManager.getInstance().addInstance(sword);
        player.getInventory().addItem(ItemUtil.createItemStackFromCustomItem(sword, 1, player));
        player.sendMessage("§aYou have been given the Admin Sword!");
        return true;
    }
}
