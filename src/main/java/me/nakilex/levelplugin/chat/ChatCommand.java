package me.nakilex.levelplugin.chat;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.guild.GuildManager;
import me.nakilex.levelplugin.party.PartyManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Command to switch between chat channels. */
public class ChatCommand implements TabExecutor {

    private final PartyManager partyManager;
    private final GuildManager guildManager;

    public ChatCommand(Main plugin) {
        this.partyManager = plugin.getPartyManager();
        this.guildManager = plugin.getGuildManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /chat <guild|party|region>");
            return true;
        }

        String mode = args[0].toLowerCase();
        switch (mode) {
            case "guild" -> {
                if (guildManager.getGuild(player.getUniqueId()) == null) {
                    player.sendMessage(ChatColor.RED + "You are not in a guild.");
                    return true;
                }
                ChatManager.setChannel(player.getUniqueId(), ChatChannel.GUILD);
                player.sendMessage(ChatColor.GREEN + "Now chatting in guild channel.");
            }
            case "party" -> {
                if (partyManager.getParty(player.getUniqueId()) == null) {
                    player.sendMessage(ChatColor.RED + "You are not in a party.");
                    return true;
                }
                ChatManager.setChannel(player.getUniqueId(), ChatChannel.PARTY);
                player.sendMessage(ChatColor.GREEN + "Now chatting in party channel.");
            }
            case "region" -> {
                ChatManager.setChannel(player.getUniqueId(), ChatChannel.REGION);
                player.sendMessage(ChatColor.GREEN + "Now chatting in region channel.");
            }
            default -> player.sendMessage(ChatColor.RED + "Usage: /chat <guild|party|region>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.stream(new String[]{"guild", "party", "region"})
                    .filter(opt -> opt.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
