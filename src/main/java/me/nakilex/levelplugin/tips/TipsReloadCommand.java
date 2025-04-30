package me.nakilex.levelplugin.tips;

import me.nakilex.levelplugin.tips.TipsConfigManager;
import me.nakilex.levelplugin.tips.BroadcastManager;
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
        cfg.load();
        mgr.start();
        sender.sendMessage("&a[Tips] Reloaded tips successfully.");
        return true;
    }
}
