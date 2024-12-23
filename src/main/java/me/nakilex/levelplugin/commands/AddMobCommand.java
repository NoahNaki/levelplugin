package me.nakilex.levelplugin.commands;

import me.nakilex.levelplugin.managers.MobManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AddMobCommand implements CommandExecutor {

    private final MobManager mobManager;

    public AddMobCommand(MobManager mobManager) {
        this.mobManager = mobManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("Usage: /addmob <mobId> <amount>");
            return true;
        }
        Player player = (Player) sender;
        String mobId = args[0];
        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch(NumberFormatException e) {
            player.sendMessage("Invalid amount: " + args[1]);
            return true;
        }
        mobManager.spawnMob(mobId, player.getLocation(), amount);
        player.sendMessage("Spawned " + amount + " of " + mobId + " near you.");
        return true;
    }
}
