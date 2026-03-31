package me.nakilex.levelplugin.tips;

import me.nakilex.levelplugin.tips.TipsConfigManager;
import me.nakilex.levelplugin.tips.BroadcastManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class TipsReloadCommand implements CommandExecutor {
    private final TipsConfigManager cfg;
    private final BroadcastManager mgr;

    public TipsReloadCommand(TipsConfigManager cfg, BroadcastManager mgr) {
        this.cfg = cfg;
        this.mgr = mgr;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("status")) {
            cfg.load();
            sender.sendMessage(ChatColor.GOLD + "[Tips] Loaded " + ChatColor.YELLOW + cfg.getTips().size()
                    + ChatColor.GOLD + " tips. Interval: " + ChatColor.YELLOW + cfg.getDelaySeconds()
                    + ChatColor.GOLD + "s");
            return true;
        }
        cfg.load();
        mgr.start();
        sender.sendMessage(ChatColor.GREEN + "[Tips] Reloaded tips successfully.");
        return true;
    }
}
