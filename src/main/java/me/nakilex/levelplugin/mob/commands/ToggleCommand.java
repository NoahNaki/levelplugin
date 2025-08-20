package me.nakilex.levelplugin.mob.commands;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ToggleFeedbackUtil;
import me.nakilex.levelplugin.settings.managers.SettingsManager;
import me.nakilex.levelplugin.settings.data.PlayerSettings;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ToggleCommand implements TabExecutor {
    private final Main plugin;

    public ToggleCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage("Usage: /toggle <dropdetails|dropdetailschat|songskip>");
            return true;
        }

        String feature = args[0].toLowerCase();
        SettingsManager sm = plugin.getSettingsManager();
        PlayerSettings ps = sm != null ? sm.getSettings(player) : null;

        switch (feature) {
            case "dropdetails":
                if (ps != null) {
                    ps.toggleDropDetails();
                    ToggleFeedbackUtil.sendToggle(player, "Drop details (holograms)", ps.isDropDetailsEnabled());
                }
                break;

            case "dropdetailschat":
                if (ps != null) {
                    ps.toggleDropDetailsChat();
                    ToggleFeedbackUtil.sendToggle(player, "Drop details chat", ps.isDropDetailsChatEnabled());
                }
                break;

            case "songskip":
                if (ps != null) {
                    ps.toggleAutoSkipSongs();
                    ToggleFeedbackUtil.sendToggle(player, "Auto Skip Songs", ps.isAutoSkipSongs());
                    if (ps.isAutoSkipSongs()) {
                        plugin.getLocationMusicManager().skip(player);
                    }
                }
                break;

            default:
                player.sendMessage("Unknown feature: " + feature);
                player.sendMessage("Usage: /toggle <dropdetails|dropdetailschat|songskip>");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("dropdetails", "dropdetailschat", "songskip").stream()
                    .filter(opt -> opt.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
