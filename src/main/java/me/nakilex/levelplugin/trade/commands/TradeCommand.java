package me.nakilex.levelplugin.trade.commands;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.DealMaker;
import me.nakilex.levelplugin.trade.utils.MessageStrings;
import me.nakilex.levelplugin.trade.utils.Translations;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import me.nakilex.levelplugin.utils.CommandUtil;

public class TradeCommand implements TabExecutor {
    private static final double MAX_DISTANCE = 10.0;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        MessageStrings messageStrings = Main.getPlugin().getMessageStrings();

        if (sender instanceof Player) {
            Player p = (Player) sender;
            final String WRONG_USAGE = Main.PREFIX + messageStrings.getTranslation(Translations.WRONG_USAGE);
            final String GITHUB_URL = "§6https://github.com/Robby3St/TradePlugin/§r";

            if (Main.getPlugin().getConfigValues().USE_WITHOUT_PERMISSION
                || p.hasPermission("trade.trade")
                || p.hasPermission("trade.*")) {

                DealMaker dm = Main.getPlugin().getDealMaker();

                if (args.length == 1) {
                    String sub = args[0].toLowerCase();

                    switch (sub) {
                        case "accept":
                            dm.acceptTrade(p);
                            return true;

                        case "cancel":
                            dm.cancelOwnTrade(p);
                            return true;

                        case "deny":
                            dm.denyTrade(p);
                            return true;

                        case "reload":
                            if (p.hasPermission("trade.reload")) {
                                Main.getPlugin().reloadConfig();
                                p.sendMessage(Main.PREFIX + messageStrings.getTranslation(Translations.RELOADED_CONFIG));
                            } else {
                                p.sendMessage(messageStrings.getTranslation(Translations.NO_PERMISSION));
                            }
                            return true;

                        case "author":
                            p.sendMessage(Main.PREFIX
                                + messageStrings.getTranslation(Translations.AUTHOR_OF_PLUGIN_IS)
                                + GITHUB_URL);
                            return true;

                        case "version":
                            if (p.hasPermission("trade.version") || p.hasPermission("trade.*")) {
                                p.sendMessage(String.format(
                                    Main.PREFIX + messageStrings.getTranslation(Translations.PLUGIN_VERSION_IS),
                                    Main.getPlugin().getDescription().getVersion(),
                                    GITHUB_URL));
                            } else {
                                p.sendMessage(messageStrings.getTranslation(Translations.NO_PERMISSION));
                            }
                            return true;

                        case "download":
                            p.sendMessage(Main.PREFIX
                                + messageStrings.getTranslation(Translations.DOWNLOAD_PLUGIN_HERE)
                                + GITHUB_URL);
                            return true;

                        case "toggle":
                            boolean useWithoutPermission = Main.getPlugin().getConfigValues().toggleUseWithoutPermission();
                            p.sendMessage(Main.PREFIX + (useWithoutPermission
                                ? messageStrings.getTranslation(Translations.YOU_ENABLED_USE_WITHOUT_PERMISSION)
                                : messageStrings.getTranslation(Translations.YOU_DISABLED_USE_WITHOUT_PERMISSION)));
                            return true;

                        case "block":
                            if (!Main.getPlugin().getConfigValues().ALLOW_BLOCKING) {
                                p.sendMessage(Main.PREFIX + messageStrings.getTranslation(Translations.THIS_FEATURE_IS_NOT_ENABLED_IN_CONFIG));
                            } else {
                                dm.blockAll(p);
                            }
                            return true;

                        case "unblock":
                            dm.unblockAll(p);
                            return true;

                        default:
                            // Assume player name
                            Player target = Bukkit.getPlayer(args[0]);
                            if (target != null) {
                                if (isWithinDistance(p, target, MAX_DISTANCE)) {
                                    boolean success = dm.makeTradeOffer(p, target);
                                    if (success) {
                                        return true;
                                    }
                                } else {
                                    p.sendMessage(Main.PREFIX
                                        + "You must be within " + (int) MAX_DISTANCE + " blocks of that player to trade.");
                                }
                            } else {
                                p.sendMessage(String.format(
                                    messageStrings.getTranslation(
                                        Translations.COULD_NOT_FIND_PLAYER_WITH_THAT_NAME_PLEASE_USE_COMMAND),
                                    Main.PREFIX, args[0]));
                            }
                            return true;
                    }

                } else if (args.length == 2) {
                    String sub = args[0].toLowerCase();
                    Player target = Bukkit.getPlayer(args[1]);

                    switch (sub) {
                        case "accept":
                            if (target != null) {
                                if (isWithinDistance(p, target, MAX_DISTANCE)) {
                                    dm.acceptTrade(p, target);
                                } else {
                                    p.sendMessage(Main.PREFIX
                                       , ChatColor.RED + "You must be within " + (int) MAX_DISTANCE + " blocks of that player to trade.");
                                }
                            } else {
                                p.sendMessage(String.format(
                                    messageStrings.getTranslation(
                                        Translations.COULD_NOT_FIND_PLAYER_WITH_THAT_NAME_PLEASE_USE_COMMAND),
                                    Main.PREFIX, args[1]));
                            }
                            return true;

                        case "deny":
                            if (target != null) {
                                dm.denyTrade(p, target);
                            } else {
                                p.sendMessage(Main.PREFIX
                                    + messageStrings.getTranslation(Translations.COULD_NOT_FIND_PLAYER_WITH_THAT_NAME));
                            }
                            return true;

                        case "block":
                            if (!Main.getPlugin().getConfigValues().ALLOW_BLOCKING) {
                                p.sendMessage(Main.PREFIX + messageStrings.getTranslation(Translations.THIS_FEATURE_IS_NOT_ENABLED_IN_CONFIG));
                            } else if (target != null) {
                                dm.addBlock(p, args[1].split(","));
                            } else {
                                p.sendMessage(Main.PREFIX
                                    + messageStrings.getTranslation(Translations.COULD_NOT_FIND_PLAYER_WITH_THAT_NAME));
                            }
                            return true;

                        case "unblock":
                            if (target != null) {
                                dm.addUnblock(p, args[1].split(","));
                            } else {
                                p.sendMessage(Main.PREFIX
                                    + messageStrings.getTranslation(Translations.COULD_NOT_FIND_PLAYER_WITH_THAT_NAME));
                            }
                            return true;

                        default:
                            p.sendMessage(WRONG_USAGE);
                            return true;
                    }

                } else {
                    p.sendMessage(WRONG_USAGE);
                }

            } else {
                p.sendMessage(messageStrings.getTranslation(Translations.NO_PERMISSION));
            }

        } else {
            // Console or non-player sender
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                Main.getPlugin().reloadConfig();
                Main.getPlugin().reloadConfigValues();
                Main.getPlugin().getMessageStrings().reloadConfig();
                sender.sendMessage(messageStrings.getTranslation(Translations.RELOADED_CONFIG));
            } else {
                sender.sendMessage(messageStrings.getTranslation(Translations.MUST_BE_A_PLAYER));
            }
        }
        return true;
    }

    private boolean isWithinDistance(Player a, Player b, double maxDistance) {
        if (!a.getWorld().equals(b.getWorld())) return false;
        Location locA = a.getLocation();
        Location locB = b.getLocation();
        return locA.distance(locB) <= maxDistance;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> subcommands = List.of("accept", "cancel", "deny", "reload", "author", "version", "download", "toggle", "block", "unblock");
        if (args.length == 1) {
            Set<String> suggestions = new LinkedHashSet<>(CommandUtil.filterStartingWith(subcommands, args[0]));
            suggestions.addAll(CommandUtil.onlinePlayerNames(args[0]));
            return new ArrayList<>(suggestions);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("accept") || sub.equals("deny") || sub.equals("block") || sub.equals("unblock")) {
                return CommandUtil.onlinePlayerNames(args[1]);
            }
        }
        return Collections.emptyList();
    }
}
