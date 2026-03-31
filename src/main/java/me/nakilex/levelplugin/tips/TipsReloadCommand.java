package me.nakilex.levelplugin.tips;

import me.nakilex.levelplugin.tips.TipsConfigManager;
import me.nakilex.levelplugin.tips.BroadcastManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TipsReloadCommand implements CommandExecutor {
    private final TipsConfigManager cfg;
    private final BroadcastManager mgr;

    public TipsReloadCommand(TipsConfigManager cfg, BroadcastManager mgr) {
        this.cfg = cfg;
        this.mgr = mgr;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("preview")) {
            cfg.load();
            List<String> tips = cfg.getTips();
            if (tips == null || tips.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "[Tips] No tips configured.");
                return true;
            }
            String tip = tips.get(ThreadLocalRandom.current().nextInt(tips.size()));
            sender.sendMessage(ChatColor.GOLD + "[Tips Preview] " + ChatColor.translateAlternateColorCodes('&', tip));
            return true;
        }
        cfg.load();
        mgr.start();
        sender.sendMessage(ChatColor.GREEN + "[Tips] Reloaded tips successfully.");
        return true;
    }
}
