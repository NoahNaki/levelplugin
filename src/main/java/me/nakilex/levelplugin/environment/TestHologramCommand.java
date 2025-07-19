package me.nakilex.levelplugin.environment;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Simple command to spawn a test hologram for debugging. */
public class TestHologramCommand implements CommandExecutor {
    private final EnvironmentManager manager;

    public TestHologramCommand(EnvironmentManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Location loc = p.getLocation().add(0, 2, 0);
        manager.spawnTestHologram(p, loc);
        p.sendMessage(ChatColor.GREEN + "Spawned test hologram.");
        return true;
    }
}
