package me.nakilex.levelplugin.player.commands;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.economy.managers.GemsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Command: /wipeprofile <player>
 * Completely resets a player's stats, level and currency.
 */
public class WipeProfileCommand implements CommandExecutor {

    private final LevelManager levelManager;
    private final StatsManager statsManager;
    private final EconomyManager economyManager;
    private final GemsManager gemsManager;

    public WipeProfileCommand(LevelManager lm, StatsManager sm, EconomyManager em, GemsManager gm) {
        this.levelManager = lm;
        this.statsManager = sm;
        this.economyManager = em;
        this.gemsManager = gm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /wipeprofile <player>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
            return true;
        }

        UUID uuid = target.getUniqueId();
        statsManager.resetPlayer(uuid);
        levelManager.setLevel(uuid, 1);
        economyManager.setBalance(uuid, 0);
        gemsManager.setTotalUnits(target, 0);

        target.getInventory().clear();
        target.getInventory().setArmorContents(null);
        target.getInventory().setItemInOffHand(null);

        statsManager.recalcDerivedStats(target);

        target.sendMessage(ChatColor.RED + "Your profile has been wiped by an administrator.");
        sender.sendMessage(ChatColor.GREEN + "Wiped profile of " + target.getName() + ".");
        return true;
    }
}
