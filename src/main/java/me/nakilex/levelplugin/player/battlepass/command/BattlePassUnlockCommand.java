package me.nakilex.levelplugin.player.battlepass.command;

import me.nakilex.levelplugin.player.battlepass.BattlePassManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import me.nakilex.levelplugin.utils.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Administrative command to unlock the premium battle pass track.
 */
public class BattlePassUnlockCommand implements CommandExecutor, TabCompleter {

    private final BattlePassManager manager;

    public BattlePassUnlockCommand(BattlePassManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            ChatMessageUtil.send(sender, MessageType.ERROR, "Usage: /" + label + " <player|@everyone>");
            return true;
        }

        String targetArg = args[0];
        if (targetArg.equalsIgnoreCase("@everyone")) {
            handleEveryone(sender);
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetArg);
        if (target.getName() == null) {
            ChatMessageUtil.send(sender, MessageType.ERROR,
                    ChatColor.GRAY + "Player not found: " + ChatColor.YELLOW + targetArg + ChatColor.GRAY + ".");
            return true;
        }

        boolean changed = manager.setPremium(target.getUniqueId(), true);
        if (!changed) {
            ChatMessageUtil.send(sender, MessageType.INFO,
                    ChatColor.YELLOW + target.getName() + ChatColor.GRAY + " already has the premium battle pass unlocked.");
            return true;
        }

        ChatMessageUtil.send(sender, MessageType.SUCCESS,
                ChatColor.GRAY + "Unlocked the premium battle pass for " + ChatColor.YELLOW + target.getName() + ChatColor.GRAY + ".");

        Player online = target.getPlayer();
        if (online != null) {
            ChatMessageUtil.send(online, MessageType.REWARD,
                    ChatColor.GRAY + "Your Battle Pass premium track has been unlocked!");
        }

        return true;
    }

    private void handleEveryone(CommandSender sender) {
        int unlocked = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (manager.setPremium(uuid, true)) {
                unlocked++;
                ChatMessageUtil.send(player, MessageType.REWARD,
                        ChatColor.GRAY + "Your Battle Pass premium track has been unlocked!");
            }
        }

        if (unlocked == 0) {
            ChatMessageUtil.send(sender, MessageType.INFO,
                    ChatColor.GRAY + "All online players already have the premium battle pass unlocked.");
        } else {
            ChatMessageUtil.send(sender, MessageType.SUCCESS,
                    ChatColor.GRAY + "Unlocked the premium battle pass for " + ChatColor.GOLD + unlocked
                            + ChatColor.GRAY + " player" + (unlocked == 1 ? "" : "s") + ".");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0];
            List<String> names = new ArrayList<>(CommandUtil.onlinePlayerNames(prefix));
            if ("@everyone".startsWith(prefix.toLowerCase(Locale.ROOT))) {
                names.add("@everyone");
            }
            Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
            return names;
        }
        return Collections.emptyList();
    }
}
