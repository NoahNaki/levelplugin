package me.nakilex.levelplugin.xprison.rebirth;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /rebirth player command plus op-only tools for testing the progression loop quickly. */
public final class RebirthCommand implements CommandExecutor, TabCompleter, Listener {

    private final XPrisonRebirthManager manager;
    private final RebirthGUI gui;

    public RebirthCommand(XPrisonRebirthManager manager, RebirthGUI gui) {
        this.manager = manager;
        this.gui = gui;
    }

    /**
     * X-Prison registers its own /rebirth (and wins the command map), which answers
     * "maximum rebirth level" because its native rebirth tiers are unused. Route every
     * rebirth label, namespaced or not, to this command before X-Prison sees it.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (tryRoute(event.getPlayer(), event.getMessage().substring(1))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        String line = event.getCommand().startsWith("/") ? event.getCommand().substring(1) : event.getCommand();
        if (tryRoute(event.getSender(), line)) {
            event.setCancelled(true);
        }
    }

    private boolean tryRoute(CommandSender sender, String line) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return false;
        }
        String label = parts[0].toLowerCase(Locale.ROOT);
        int colon = label.indexOf(':');
        String bare = colon >= 0 ? label.substring(colon + 1) : label;
        if (!bare.equals("rebirth") && !bare.equals("rb")) {
            return false;
        }
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);
        onCommand(sender, null, bare, args);
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Players use /rebirth. Console can use /rebirth debug status <player>.");
                return true;
            }
            gui.open(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("status")) {
            Player target = args.length >= 2 ? Bukkit.getPlayerExact(args[1])
                    : (sender instanceof Player player ? player : null);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player must be online.");
                return true;
            }
            sendStatus(sender, target);
            return true;
        }

        if (!args[0].equalsIgnoreCase("debug")) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /rebirth [info] or /rebirth debug <...>");
            return true;
        }
        if (!sender.hasPermission(XPrisonRebirthManager.DEBUG_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use rebirth debug tools.");
            return true;
        }
        if (args.length < 2) {
            sendDebugHelp(sender);
            return true;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "status" -> {
                Player target = target(sender, args, 2);
                if (target != null) sendStatus(sender, target);
            }
            case "setcount" -> {
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /rebirth debug setcount <player> <amount>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[2]);
                Integer amount = integer(args[3]);
                if (target == null || amount == null || amount < 0) {
                    sender.sendMessage(ChatColor.RED + "Online player and non-negative rebirth count required.");
                    return true;
                }
                manager.setRebirthsForDebug(target.getUniqueId(), amount);
                sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + " to Rebirth " + amount + ".");
            }
            case "eligible" -> {
                Player target = target(sender, args, 2);
                if (target == null) return true;
                int requirement = manager.requiredLevelFor(manager.getRebirths(target.getUniqueId()));
                if (!manager.setPickaxeLevelForDebug(target, requirement)) {
                    sender.sendMessage(ChatColor.RED + "Could not set the pickaxe. The player must have exactly one X-Prison pickaxe in their inventory.");
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s pickaxe to required Level " + requirement + ".");
            }
            case "setearned" -> {
                if (args.length < 5) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /rebirth debug setearned <player> <money> <tokens>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[2]);
                BigDecimal money = decimal(args[3]);
                BigDecimal tokens = decimal(args[4]);
                if (target == null || money == null || tokens == null || money.signum() < 0 || tokens.signum() < 0) {
                    sender.sendMessage(ChatColor.RED + "Online player and non-negative money/tokens required.");
                    return true;
                }
                manager.setRunEarningsForDebug(target.getUniqueId(), money, tokens);
                sender.sendMessage(ChatColor.GREEN + "Set tracked run earnings for " + target.getName() + ".");
            }
            case "minereward" -> {
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /rebirth debug minereward <money|tokens> <amount> [player]");
                    return true;
                }
                String currency = args[2].toLowerCase(Locale.ROOT);
                BigDecimal amount = decimal(args[3]);
                Player target = args.length >= 5 ? Bukkit.getPlayerExact(args[4])
                        : (sender instanceof Player player ? player : null);
                if (target == null || amount == null || amount.signum() <= 0
                        || (!currency.equals("money") && !currency.equals("tokens"))) {
                    sender.sendMessage(ChatColor.RED + "Use money/tokens, a positive amount, and an online player.");
                    return true;
                }
                if (!manager.giveMiningRewardForDebug(target, currency, amount)) {
                    sender.sendMessage(ChatColor.RED + "X-Prison rejected the test mining reward.");
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + "Injected a MINING reward of " + amount.toPlainString() + " " + currency
                        + " for " + target.getName() + ". The rebirth multiplier should have been applied.");
            }
            case "force" -> {
                Player target = target(sender, args, 2);
                if (target == null) return true;
                XPrisonRebirthManager.RebirthResult result = manager.rebirth(target, true);
                sender.sendMessage((result.success() ? ChatColor.GREEN : ChatColor.RED) + result.message()
                        + (result.success() ? " New rebirth: " + result.newRebirths() + ", gems: " + result.gemsAwarded() : ""));
            }
            case "resetstate" -> {
                Player target = target(sender, args, 2);
                if (target == null) return true;
                manager.setRebirthsForDebug(target.getUniqueId(), 0);
                manager.setRunEarningsForDebug(target.getUniqueId(), BigDecimal.ZERO, BigDecimal.ZERO);
                sender.sendMessage(ChatColor.GREEN + "Reset LevelPlugin rebirth count/run earnings for " + target.getName()
                        + ". X-Prison balances, pickaxe and pickaxe binding were not changed.");
            }
            case "unbind" -> {
                Player target = target(sender, args, 2);
                if (target == null) return true;
                if (!manager.unbindPickaxeForDebug(target)) {
                    sender.sendMessage(ChatColor.RED + "Could not unbind the pickaxe. The player must have exactly one X-Prison pickaxe in their inventory.");
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + "Cleared " + target.getName() + "'s rebirth pickaxe binding. Their next successful rebirth will bind the current pickaxe again.");
            }
            case "cap" -> sender.sendMessage(ChatColor.YELLOW + "X-Prison max pickaxe level: "
                    + ChatColor.WHITE + manager.actualXPrisonMaxPickaxeLevel()
                    + ChatColor.GRAY + " | desired practical cap: "
                    + ChatColor.WHITE + manager.desiredPracticalMaxPickaxeLevel());
            default -> sendDebugHelp(sender);
        }
        return true;
    }

    private void sendStatus(CommandSender sender, Player target) {
        XPrisonRebirthManager.RebirthPreview preview = manager.preview(target);
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Rebirth status for " + target.getName());
        sender.sendMessage(ChatColor.GRAY + "Rebirth: " + ChatColor.WHITE + preview.rebirths()
                + ChatColor.GRAY + " | Level: " + ChatColor.WHITE + preview.currentLevel()
                + ChatColor.GRAY + "/" + preview.requiredLevel());
        sender.sendMessage(ChatColor.GRAY + "Multiplier: " + ChatColor.WHITE + preview.currentMultiplier() + "x"
                + ChatColor.GRAY + " -> " + ChatColor.GREEN + preview.nextMultiplier() + "x");
        sender.sendMessage(ChatColor.GRAY + "Tracked mining money: " + ChatColor.WHITE + preview.runMoneyEarned().toPlainString());
        sender.sendMessage(ChatColor.GRAY + "Tracked mining tokens: " + ChatColor.WHITE + preview.runTokensEarned().toPlainString());
        sender.sendMessage(ChatColor.GRAY + "Projected gems: " + ChatColor.AQUA + preview.gemReward());
        sender.sendMessage(ChatColor.GRAY + "Eligible: " + (preview.eligible() ? ChatColor.GREEN + "yes"
                : ChatColor.RED + "no" + ChatColor.GRAY + " (" + preview.blocker() + ")"));
    }

    private Player target(CommandSender sender, String[] args, int index) {
        Player target = args.length > index ? Bukkit.getPlayerExact(args[index])
                : (sender instanceof Player player ? player : null);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Specify an online player.");
        }
        return target;
    }

    private static Integer integer(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static BigDecimal decimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void sendDebugHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Rebirth debug commands:");
        sender.sendMessage(ChatColor.GRAY + "/rebirth debug status [player]");
        sender.sendMessage(ChatColor.GRAY + "/rebirth debug eligible [player]");
        sender.sendMessage(ChatColor.GRAY + "/rebirth debug setcount <player> <amount>");
        sender.sendMessage(ChatColor.GRAY + "/rebirth debug setearned <player> <money> <tokens>");
        sender.sendMessage(ChatColor.GRAY + "/rebirth debug minereward <money|tokens> <amount> [player]");
        sender.sendMessage(ChatColor.GRAY + "/rebirth debug force [player]");
        sender.sendMessage(ChatColor.GRAY + "/rebirth debug resetstate [player]");
        sender.sendMessage(ChatColor.GRAY + "/rebirth debug unbind [player]");
        sender.sendMessage(ChatColor.GRAY + "/rebirth debug cap");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("info", "debug"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("debug") && sender.hasPermission(XPrisonRebirthManager.DEBUG_PERMISSION)) {
            return filter(List.of("status", "eligible", "setcount", "setearned", "minereward", "force", "resetstate", "unbind", "cap"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("debug")) {
            if (args[1].equalsIgnoreCase("minereward")) {
                return filter(List.of("money", "tokens"), args[2]);
            }
            if (List.of("status", "eligible", "setcount", "setearned", "force", "resetstate", "unbind").contains(args[1].toLowerCase(Locale.ROOT))) {
                List<String> names = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) names.add(player.getName());
                return filter(names, args[2]);
            }
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("minereward")) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) names.add(player.getName());
            return filter(names, args[4]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> values, String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
