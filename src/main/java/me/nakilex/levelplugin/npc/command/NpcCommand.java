package me.nakilex.levelplugin.npc.command;

import me.nakilex.levelplugin.npc.core.NpcManager;
import me.nakilex.levelplugin.npc.core.PlayerNpc;
import me.nakilex.levelplugin.utils.CommandUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class NpcCommand implements TabExecutor {
    private final NpcManager npcManager;

    public NpcCommand(NpcManager npcManager) {
        this.npcManager = npcManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1 || !args[0].equalsIgnoreCase("create")) {
            sender.sendMessage(ChatColor.RED + "Usage: /npc create <name>");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can create NPCs.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /npc create <name>");
            return true;
        }

        String name = args[1];
        if (name.length() < 1 || name.length() > 16) {
            player.sendMessage(ChatColor.RED + "NPC names must be between 1 and 16 characters.");
            return true;
        }

        if (npcManager.getRegistry().exists(name)) {
            player.sendMessage(ChatColor.RED + "An NPC named '" + name + "' already exists.");
            return true;
        }

        PlayerNpc npc = npcManager.create(name, player.getLocation());
        npcManager.spawnForNearby(npc, 48.0);
        player.sendMessage(ChatColor.GREEN + "Spawned NPC '" + npc.getName() + "'.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtil.simpleSuggestions(args[0], "create");
        }
        return Collections.emptyList();
    }
}
