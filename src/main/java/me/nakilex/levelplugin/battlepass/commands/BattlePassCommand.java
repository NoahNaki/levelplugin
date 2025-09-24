package me.nakilex.levelplugin.battlepass.commands;

import me.nakilex.levelplugin.battlepass.BattlePassManager;
import me.nakilex.levelplugin.battlepass.BattlePassReward;
import me.nakilex.levelplugin.battlepass.BattlePassTier;
import me.nakilex.levelplugin.battlepass.gui.BattlePassGUI;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
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
        if (args.length == 0) {
            if (sender instanceof Player player) {
                BattlePassGUI.open(player, manager);
            } else {
                ChatMessageUtil.send(sender, MessageType.INFO,
                        "Usage: /" + label + " <claim|unlock|lock|rewards>");
            }
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "unlock" -> {
                if (args.length == 1) {
                    if (!(sender instanceof Player player)) {
                        ChatMessageUtil.send(sender, MessageType.INFO,
                                "Usage: /" + label + " unlock <player|everyone>");
                        return true;
                    }
                    if (manager.unlockPremium(player)) {
                        BattlePassGUI.open(player, manager);
                    }
                    return true;
                }
                handlePremiumToggle(sender, args[1], true);
                return true;
            }
            case "lock" -> {
                if (args.length < 2) {
                    ChatMessageUtil.send(sender, MessageType.INFO,
                            "Usage: /" + label + " lock <player|everyone>");
                    return true;
                }
                handlePremiumToggle(sender, args[1], false);
                return true;
            }
            case "claim" -> {
                if (!(sender instanceof Player player)) {
                    ChatMessageUtil.send(sender, MessageType.ERROR, "Only players may claim battle pass rewards.");
                    return true;
                }
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
            case "rewards" -> {
                sendRewardsList(sender);
                return true;
            }
            default -> {
                if (sender instanceof Player player) {
                    BattlePassGUI.open(player, manager);
                } else {
                    ChatMessageUtil.send(sender, MessageType.INFO,
                            "Usage: /" + label + " <claim|unlock|lock|rewards>");
                }
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtil.filterStartingWith(List.of("unlock", "lock", "claim", "rewards"), args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("claim")) {
                List<String> tiers = IntStream.rangeClosed(1, manager.getTiers().size())
                        .mapToObj(Integer::toString)
                        .collect(Collectors.toList());
                return CommandUtil.filterStartingWith(tiers, args[1]);
            }
            if (sub.equals("unlock") || sub.equals("lock")) {
                List<String> names = new ArrayList<>(CommandUtil.onlinePlayerNames(args[1]));
                String lower = args[1].toLowerCase();
                if ("everyone".startsWith(lower)) {
                    names.add("everyone");
                }
                if ("@everyone".startsWith(lower)) {
                    names.add("@everyone");
                }
                return names;
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("claim")) {
            return CommandUtil.filterStartingWith(List.of("free", "premium"), args[2]);
        }
        return Collections.emptyList();
    }

    private void handlePremiumToggle(CommandSender sender, String targetArg, boolean premium) {
        if (isEveryoneTarget(targetArg)) {
            var online = Bukkit.getOnlinePlayers();
            if (online.isEmpty()) {
                ChatMessageUtil.send(sender, MessageType.WARNING, "No online players to modify.");
                return;
            }
            int changed = 0;
            int unchanged = 0;
            for (Player target : online) {
                if (manager.setPremiumStatus(target.getUniqueId(), premium)) {
                    changed++;
                    ChatMessageUtil.send(target, premium ? MessageType.SUCCESS : MessageType.WARNING,
                            premium
                                    ? "Your battle pass premium track was unlocked."
                                    : "Your battle pass premium track was locked by staff.");
                } else {
                    unchanged++;
                }
            }
            if (changed > 0) {
                ChatMessageUtil.send(sender, premium ? MessageType.SUCCESS : MessageType.WARNING,
                        (premium ? "Unlocked" : "Locked") + " premium track for "
                                + changed + (changed == 1 ? " player." : " players."));
            }
            if (unchanged > 0) {
                ChatMessageUtil.send(sender, MessageType.INFO,
                        unchanged + (unchanged == 1 ? " player already " : " players already ")
                                + (premium ? "had" : "did not have") + " premium access.");
            }
            return;
        }

        Player target = Bukkit.getPlayerExact(targetArg);
        if (target == null) {
            target = Bukkit.getPlayer(targetArg);
        }
        if (target == null) {
            ChatMessageUtil.send(sender, MessageType.ERROR, "Player not found: " + targetArg);
            return;
        }

        if (manager.setPremiumStatus(target.getUniqueId(), premium)) {
            ChatMessageUtil.send(sender, premium ? MessageType.SUCCESS : MessageType.WARNING,
                    (premium ? "Unlocked" : "Locked") + " premium track for " + target.getName() + ".");
            ChatMessageUtil.send(target, premium ? MessageType.SUCCESS : MessageType.WARNING,
                    premium
                            ? "Your battle pass premium track was unlocked."
                            : "Your battle pass premium track was locked by staff.");
        } else {
            ChatMessageUtil.send(sender, MessageType.INFO,
                    target.getName() + " already " + (premium ? "has" : "does not have") + " premium access.");
        }
    }

    private void sendRewardsList(CommandSender sender) {
        ChatMessageUtil.send(sender, MessageType.INFO,
                ChatColor.GOLD + "Battle Pass Rewards " + ChatColor.GRAY + "(" + manager.getTiers().size() + " tiers)" + ChatColor.WHITE);
        for (BattlePassTier tier : manager.getTiers()) {
            BattlePassReward free = tier.freeReward();
            BattlePassReward premium = tier.premiumReward();
            String freeName = free != null ? free.getDisplayName() : ChatColor.DARK_GRAY + "None";
            String premiumName = premium != null ? premium.getDisplayName() : ChatColor.DARK_GRAY + "None";
            ChatMessageUtil.send(sender, MessageType.INFO,
                    ChatColor.YELLOW + "Tier " + tier.index() + ChatColor.GRAY + ": "
                            + ChatColor.GREEN + "Free " + ChatColor.WHITE + "→ " + freeName + ChatColor.GRAY
                            + " | " + ChatColor.LIGHT_PURPLE + "Premium " + ChatColor.WHITE + "→ " + premiumName);
        }
    }

    private boolean isEveryoneTarget(String input) {
        return input.equalsIgnoreCase("everyone") || input.equalsIgnoreCase("@everyone");
    }
}
