package me.nakilex.levelplugin.battlepass.commands;

import me.nakilex.levelplugin.battlepass.BattlePassManager;
import me.nakilex.levelplugin.battlepass.gui.BattlePassGUI;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;

/**
 * Command entry point for the battle pass.
 */
public class BattlePassCommand implements TabExecutor {

    private final BattlePassManager manager;

    public BattlePassCommand(BattlePassManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, MessageType.ERROR, "Only players may use this command.");
            return true;
        }
        if (args.length == 0) {
            BattlePassGUI.open(player, manager);
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "unlock" -> {
                if (manager.unlockPremium(player)) {
                    BattlePassGUI.open(player, manager);
                }
                return true;
            }
            case "claim" -> {
                if (args.length < 2) {
                    ChatMessageUtil.send(player, MessageType.INFO,
                            "Usage: /" + label + " claim <tier> [premium]");
                    return true;
                }
                int tier;
                try {
                    tier = Integer.parseInt(args[1]);
                } catch (NumberFormatException ex) {
                    ChatMessageUtil.send(player, MessageType.ERROR, "Tier must be a number.");
                    return true;
                }
                boolean premium = args.length > 2 && args[2].equalsIgnoreCase("premium");
                if (manager.claim(player, tier, premium)) {
                    BattlePassGUI.open(player, manager);
                }
                return true;
            }
            default -> {
                BattlePassGUI.open(player, manager);
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtil.filterStartingWith(List.of("unlock", "claim"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("claim")) {
            List<String> tiers = IntStream.rangeClosed(1, manager.getTiers().size())
                    .mapToObj(Integer::toString)
                    .collect(Collectors.toList());
            return CommandUtil.filterStartingWith(tiers, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("claim")) {
            return CommandUtil.filterStartingWith(List.of("free", "premium"), args[2]);
        }
        return Collections.emptyList();
    }
}
