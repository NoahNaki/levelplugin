package me.nakilex.levelplugin.player.classes.commands;

import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Debug command to generate a random class essence for a player.
 * Usage: /genclass <player>
 */
public class GenClassCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("§cUsage: /genclass <player>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return true;
        }
        ItemStack essence = ClassEssence.generateRandomEssence();
        target.getInventory().addItem(essence);

        PlayerClass clazz = ClassEssence.getClass(essence);
        StatsManager sm = StatsManager.getInstance();
        StatsManager.PlayerStats ps = sm.getPlayerStats(target.getUniqueId());
        if (clazz != null && !ps.unlockedClasses.contains(clazz)) {
            sm.addSkillPoints(target.getUniqueId(), 2);
            sm.unlockClass(target.getUniqueId(), clazz);
            target.sendMessage("§aGained ownership bonus: +2 Skill Points");
        }

        sender.sendMessage("§aGenerated class essence for " + target.getName());
        return true;
    }
}
