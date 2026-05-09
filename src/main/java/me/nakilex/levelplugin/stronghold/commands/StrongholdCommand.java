package me.nakilex.levelplugin.stronghold.commands;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.stronghold.StrongholdQueueManager;
import me.nakilex.levelplugin.stronghold.StrongholdQueueMode;
import me.nakilex.levelplugin.stronghold.StrongholdShrineManager;
import me.nakilex.levelplugin.stronghold.StrongholdTemplateConfig;
import me.nakilex.levelplugin.stronghold.gui.StrongholdQueueGUI;
import me.nakilex.levelplugin.stronghold.run.StrongholdHeat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

public class StrongholdCommand implements TabExecutor {
    private final Main plugin;
    private final StrongholdQueueGUI gui;
    private final StrongholdQueueManager queueManager;
    private final StrongholdShrineManager shrineManager;

    public StrongholdCommand(Main plugin, StrongholdQueueGUI gui, StrongholdQueueManager queueManager,
                             StrongholdShrineManager shrineManager) {
        this.plugin = plugin;
        this.gui = gui;
        this.queueManager = queueManager;
        this.shrineManager = shrineManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && equalsAny(args[0], "template")) {
            return handleTemplateSubcommand(sender, args);
        }
        if (args.length > 0 && equalsAny(args[0], "shrine")) {
            return handleShrineSubcommand(sender, args);
        }

        if (!(sender instanceof Player player)) {
            send(sender, MessageType.ERROR, "Only players can use this command.");
            return true;
        }

        if (args.length == 0 || equalsAny(args[0], "open", "menu", "gui")) {
            gui.open(player);
            return true;
        }

        UUID id = player.getUniqueId();
        if (equalsAny(args[0], "join", "queue")) {
            StrongholdQueueMode mode = parseMode(args, 1).orElse(StrongholdQueueMode.SOLO);
            Optional<StrongholdQueueMode> current = queueManager.getMode(id);
            if (current.isPresent() && !current.get().equals(mode)) {
                send(player, MessageType.ERROR, "Leave your current Stronghold queue before joining another.");
                return true;
            }
            if (current.isPresent()) {
                send(player, MessageType.WARNING, "You are already in the " + current.get().displayName() + ChatColor.GRAY + " queue.");
                return true;
            }
            StrongholdQueueManager.QueueJoinOutcome outcome = queueManager.join(player, mode);
            if (outcome.result() == StrongholdQueueManager.QueueJoinResult.JOINED
                    || outcome.result() == StrongholdQueueManager.QueueJoinResult.STARTED) {
                String success = outcome.result() == StrongholdQueueManager.QueueJoinResult.STARTED
                        ? "Generating your solo Stronghold run."
                        : "You joined the " + mode.displayName() + ChatColor.GRAY + " queue.";
                send(player, MessageType.SUCCESS, success);
                gui.refresh();
            } else {
                send(player, MessageType.ERROR, outcome.message() == null
                        ? ChatColor.RED + "Unable to join Stronghold queue."
                        : outcome.message());
            }
            return true;
        }

        if (equalsAny(args[0], "heat", "curse")) {
            StrongholdHeat heat = args.length >= 2 ? StrongholdHeat.byId(args[1]) : plugin.getStrongholdRunManager().cycleQueuedHeat(player);
            if (args.length >= 2) {
                plugin.getStrongholdRunManager().queueHeat(player, heat);
            }
            send(player, heat == StrongholdHeat.NONE ? MessageType.INFO : MessageType.WARNING,
                    "Stronghold heat set to " + heat.coloredName() + ChatColor.GRAY + ".");
            gui.refresh();
            return true;
        }

        if (equalsAny(args[0], "leave", "quit")) {
            Optional<StrongholdQueueMode> current = queueManager.getMode(id);
            if (current.isEmpty()) {
                send(player, MessageType.WARNING, "You are not in a Stronghold queue.");
                return true;
            }
            queueManager.leave(id);
            send(player, MessageType.INFO, "You left the " + current.get().displayName() + ChatColor.GRAY + " queue.");
            gui.refresh();
            return true;
        }

        send(player, MessageType.INFO, "Usage: /" + label + " [join|leave|open|heat|template|shrine]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return partial(args[0], List.of("join", "leave", "open", "heat", "template", "shrine"));
        }
        if (args.length == 2 && equalsAny(args[0], "join", "queue")) {
            return partial(args[1], Arrays.asList("solo", "duo", "squad"));
        }
        if (args.length == 2 && equalsAny(args[0], "heat", "curse")) {
            return partial(args[1], Arrays.stream(StrongholdHeat.values()).map(StrongholdHeat::id).toList());
        }
        if (args.length == 2 && equalsAny(args[0], "template")) {
            return partial(args[1], List.of("set", "clear", "show"));
        }
        if (args.length == 3 && equalsAny(args[0], "template") && equalsAny(args[1], "set")) {
            List<String> names = Bukkit.getWorlds().stream().map(World::getName).toList();
            return partial(args[2], names);
        }
        if (args.length == 2 && equalsAny(args[0], "shrine")) {
            return partial(args[1], List.of("create"));
        }
        return Collections.emptyList();
    }

    private List<String> partial(String input, List<String> options) {
        String current = input.toLowerCase();
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(current)) {
                matches.add(option);
            }
        }
        return matches;
    }

    private boolean equalsAny(String input, String... values) {
        for (String value : values) {
            if (input.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private Optional<StrongholdQueueMode> parseMode(String[] args, int index) {
        if (args.length <= index) {
            return Optional.empty();
        }
        return switch (args[index].toLowerCase()) {
            case "solo", "s" -> Optional.of(StrongholdQueueMode.SOLO);
            case "duo", "d", "2" -> Optional.of(StrongholdQueueMode.DUO);
            case "squad", "q", "4" -> Optional.of(StrongholdQueueMode.SQUAD);
            default -> Optional.empty();
        };
    }

    private boolean handleTemplateSubcommand(CommandSender sender, String[] args) {
        if (plugin == null || plugin.getCustomConfig() == null) {
            send(sender, MessageType.ERROR, "Config is unavailable. Try again after startup.");
            return true;
        }
        if (args.length < 2 || equalsAny(args[1], "show")) {
            String current = StrongholdTemplateConfig.templateWorld(plugin);
            if (current.isBlank()) {
                send(sender, MessageType.INFO, "Stronghold template world is currently unset (superflat fallback is used).");
            } else {
                String origin = StrongholdTemplateConfig.templateOrigin(plugin)
                        .map(StrongholdTemplateConfig::formatOrigin)
                        .orElse(ChatColor.GRAY + "unset (defaults to " + ChatColor.WHITE + "(0, -61, 0)" + ChatColor.GRAY + ")");
                send(sender, MessageType.INFO, "Stronghold template world: " + ChatColor.WHITE + current
                        + ChatColor.GRAY + " | Origin: " + origin);
            }
            return true;
        }
        if (equalsAny(args[1], "clear", "none", "off")) {
            StrongholdTemplateConfig.clearTemplate(plugin);
            send(sender, MessageType.SUCCESS, "Cleared Stronghold template world and origin. New runs will use superflat fallback.");
            return true;
        }
        if (equalsAny(args[1], "set")) {
            if (!(sender instanceof Player player)) {
                send(sender, MessageType.ERROR, "Only players can set the Stronghold template origin because it uses your current position.");
                return true;
            }
            if (args.length < 3) {
                send(sender, MessageType.WARNING, "Usage: /stronghold template set <worldName>");
                return true;
            }
            String worldName = args[2];
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                send(sender, MessageType.ERROR, "World '" + worldName + "' is not loaded. Load/import it first.");
                return true;
            }
            if (!player.getWorld().getUID().equals(world.getUID())) {
                send(player, MessageType.WARNING, "Stand at the desired origin inside world '" + world.getName()
                        + "' before running /stronghold template set " + world.getName() + ".");
                return true;
            }
            StrongholdTemplateConfig.setTemplate(plugin, world, player.getLocation());
            var origin = StrongholdTemplateConfig.templateOrigin(plugin).orElse(null);
            send(sender, MessageType.SUCCESS, "Set Stronghold template world to " + ChatColor.WHITE + world.getName()
                    + ChatColor.GREEN + " with generation origin " + StrongholdTemplateConfig.formatOrigin(origin) + ChatColor.GREEN + ".");
            return true;
        }
        send(sender, MessageType.WARNING, "Usage: /stronghold template <set|clear|show> [worldName]");
        return true;
    }

    private boolean handleShrineSubcommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, MessageType.ERROR, "Only players can use shrine commands.");
            return true;
        }
        if (!player.hasPermission("levelplugin.admin")) {
            send(player, MessageType.ERROR, "You do not have permission to manage Stronghold shrines.");
            return true;
        }
        if (shrineManager == null) {
            send(player, MessageType.ERROR, "Shrine manager unavailable.");
            return true;
        }
        if (args.length < 2 || !equalsAny(args[1], "create", "spawn")) {
            send(player, MessageType.WARNING, "Usage: /stronghold shrine create [hp]");
            return true;
        }
        double hp = 250.0;
        if (args.length >= 3) {
            try {
                hp = Math.max(20.0, Double.parseDouble(args[2]));
            } catch (NumberFormatException ignored) {
                send(player, MessageType.ERROR, "Invalid HP value. Example: /stronghold shrine create 250");
                return true;
            }
        }
        shrineManager.spawnShrine(player.getLocation(), hp).ifPresentOrElse(anchor -> {
            send(player, MessageType.SUCCESS, "Spawned shrine at your location. Right-click the shrine hologram to begin.");
        }, () -> send(player, MessageType.ERROR, "Failed to spawn shrine."));
        return true;
    }
}
