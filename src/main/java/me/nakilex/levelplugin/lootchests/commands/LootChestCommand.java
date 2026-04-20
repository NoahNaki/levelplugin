package me.nakilex.levelplugin.lootchests.commands;

import me.nakilex.levelplugin.lootchests.config.ConfigManager;
import me.nakilex.levelplugin.lootchests.data.ChestData;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.CommandUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class LootChestCommand implements CommandExecutor, TabCompleter {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final List<String> SUBCOMMANDS = List.of("reload", "list", "clear", "delete", "tp", "wand", "reset");

    private final ConfigManager configManager;
    private final LootChestManager lootChestManager;


    public LootChestCommand(ConfigManager configManager, LootChestManager lootChestManager) {
        this.configManager = configManager;
        this.lootChestManager = lootChestManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload":
                handleReload(sender);
                return true;

            case "list":
                handleList(sender);
                return true;

            case "clear":
                handleClear(sender, args);
                return true;

            case "delete":
                handleDelete(sender, args);
                return true;

            case "tp":
                handleTeleport(sender, args);
                return true;

            case "wand":
                handleWand(sender);
                return true;

            case "reset":
                handleReset(sender, args);
                return true;

            default:
                sendHelp(sender);
                return true;
        }
    }

    /**
     * Reload the lootchests.yml config file from disk.
     */
    private void handleReload(CommandSender sender) {
        configManager.reloadLootChestsConfig();
        lootChestManager.reloadFromConfig();
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS, "Loot chest configuration reloaded.");
    }

    /**
     * List all loaded chest data (ID, Tier, Coordinates).
     * We assume they all spawn in the same world (rpgworld).
     */
    private void handleList(CommandSender sender) {
        List<ChestData> allChests = new ArrayList<>(lootChestManager.getAllChestData());
        if (allChests.isEmpty()) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO, "No loot chests found.");
            return;
        }

        allChests.sort((a, b) -> Integer.compare(a.getChestId(), b.getChestId()));
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "Loaded loot chests: " + ChatColor.YELLOW + allChests.size());

        if (!(sender instanceof Player player)) {
            for (ChestData chestData : allChests) {
                sender.sendMessage(ChatColor.DARK_GREEN + "- #" + chestData.getChestId()
                        + ChatColor.GRAY + " (" + chestData.getWorldName() + ") "
                        + chestData.getX() + ", " + chestData.getY() + ", " + chestData.getZ());
            }
            return;
        }

        for (ChestData chestData : allChests) {
            int id = chestData.getChestId();
            String locationText = ChatColor.GRAY + "(" + ChatColor.YELLOW + (int) chestData.getX()
                    + ChatColor.GRAY + ", " + ChatColor.YELLOW + (int) chestData.getY()
                    + ChatColor.GRAY + ", " + ChatColor.YELLOW + (int) chestData.getZ() + ChatColor.GRAY + ")";

            Component deleteBtn = LEGACY.deserialize(ChatColor.RED + "[" + ChatColor.BOLD + "x" + ChatColor.RED + "]")
                    .clickEvent(ClickEvent.runCommand("/lootchest delete " + id))
                    .hoverEvent(HoverEvent.showText(Component.text("Delete chest #" + id)));
            Component tpBtn = LEGACY.deserialize(ChatColor.GREEN + "[" + ChatColor.BOLD + "+" + ChatColor.GREEN + "]")
                    .clickEvent(ClickEvent.runCommand("/lootchest tp " + id))
                    .hoverEvent(HoverEvent.showText(Component.text("Teleport to chest #" + id)));
            Component line = LEGACY.deserialize(ChatColor.DARK_GREEN + "#"
                            + id + ChatColor.GRAY + " " + chestData.getWorldName() + " " + locationText + " ")
                    .append(tpBtn)
                    .append(LEGACY.deserialize(" "))
                    .append(deleteBtn);
            player.sendMessage(line);
        }
    }

    // LootChestCommand.java

    private void handleClear(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /lootchest clear <id|all>");
            return;
        }

        if (args[1].equalsIgnoreCase("all")) {
            boolean anyRemoved = false;
            for (ChestData data : lootChestManager.getAllChestData()) {
                int id = data.getChestId();
                if (lootChestManager.removeChest(id)) {
                    lootChestManager.getCooldownManager().startChestCooldown(id);
                    anyRemoved = true;
                }
            }
            if (anyRemoved) {
                sender.sendMessage(ChatColor.GREEN + "Cleared all loot chests (they will respawn after cooldown).");
            } else {
                sender.sendMessage(ChatColor.RED + "No spawned loot chests to clear.");
            }
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "'" + args[1] + "' is not a valid chest ID.");
            return;
        }

        if (lootChestManager.removeChest(id)) {
            // This kicks off the respawn timer
            lootChestManager.getCooldownManager().startChestCooldown(id);
            sender.sendMessage(ChatColor.GREEN + "Cleared loot chest " + id + " (will respawn after cooldown).");
        } else {
            sender.sendMessage(ChatColor.RED + "No spawned loot chest found with ID " + id + ".");
        }
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length != 2) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "Usage: /lootchest delete <id>");
            return;
        }
        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "Invalid chest ID: " + args[1]);
            return;
        }
        ChestData data = lootChestManager.getChestDataById(id);
        if (data == null) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.WARNING, "Loot chest #" + id + " does not exist.");
            return;
        }
        lootChestManager.deleteChest(id);
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS, "Deleted loot chest #" + id + ".");
    }

    private void handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "Only players can teleport to loot chests.");
            return;
        }
        if (args.length != 2) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Usage: /lootchest tp <id>");
            return;
        }
        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Invalid chest ID: " + args[1]);
            return;
        }
        ChestData data = lootChestManager.getChestDataById(id);
        if (data == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "Loot chest #" + id + " does not exist.");
            return;
        }
        Location location = data.toLocation();
        if (location == null || location.getWorld() == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Could not resolve chest world for #" + id + ".");
            return;
        }
        Location target = location.clone().add(0.5, 1.0, 0.5);
        player.teleport(target);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Teleported to loot chest #" + id + ChatColor.GRAY + " at " + ChatColor.YELLOW
                        + location.getBlockX() + ChatColor.GRAY + ", " + ChatColor.YELLOW + location.getBlockY()
                        + ChatColor.GRAY + ", " + ChatColor.YELLOW + location.getBlockZ() + ChatColor.GRAY + ".");
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length != 2 || !args[1].equalsIgnoreCase("confirm")) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.WARNING,
                    "This removes all configured loot chests. Run " + ChatColor.YELLOW + "/lootchest reset confirm");
            return;
        }
        lootChestManager.removeAllChests();
        List<ChestData> snapshot = new ArrayList<>(lootChestManager.getAllChestData());
        for (ChestData data : snapshot) {
            configManager.removeLootChest(data.getChestId());
        }
        configManager.reloadLootChestsConfig();
        lootChestManager.reloadFromConfig();
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS, "Removed all loot chest entries from config.");
    }


    private void sendHelp(CommandSender sender) {
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO, ChatColor.YELLOW + "Loot chest commands:");
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO, ChatColor.YELLOW + "/lootchest reload");
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO, ChatColor.YELLOW + "/lootchest list");
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO, ChatColor.YELLOW + "/lootchest tp <id>");
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO, ChatColor.YELLOW + "/lootchest delete <id>");
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO, ChatColor.YELLOW + "/lootchest clear <id|all>");
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO, ChatColor.YELLOW + "/lootchest wand");
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO, ChatColor.YELLOW + "/lootchest reset confirm");
    }

    private void handleWand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return;
        }

        player.getInventory().addItem(lootChestManager.createWand());
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS, "Loot chest wand added to your inventory.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtil.filterStartingWith(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("clear")) {
                List<String> options = new ArrayList<>();
                options.add("all");
                for (ChestData data : lootChestManager.getAllChestData()) {
                    options.add(Integer.toString(data.getChestId()));
                }
                return CommandUtil.filterStartingWith(options, args[1]);
            }
            if (sub.equals("delete") || sub.equals("tp")) {
                List<String> ids = new ArrayList<>();
                for (ChestData data : lootChestManager.getAllChestData()) {
                    ids.add(Integer.toString(data.getChestId()));
                }
                return CommandUtil.filterStartingWith(ids, args[1]);
            }
            if (sub.equals("reset")) {
                return CommandUtil.simpleSuggestions(args[1], "confirm");
            }
        }
        return Collections.emptyList();
    }
}
