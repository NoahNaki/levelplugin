package me.nakilex.levelplugin.chat;

import me.nakilex.levelplugin.guild.Guild;
import me.nakilex.levelplugin.guild.GuildManager;
import me.nakilex.levelplugin.party.Party;
import me.nakilex.levelplugin.party.PartyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles global chat state such as muting, clearing and per-player channels.
 */
public class ChatManager {

    private static boolean muted = false;
    private static final Map<UUID, ChatChannel> channels = new HashMap<>();
    private static PartyManager partyManager;
    private static GuildManager guildManager;

    /** Mute all chat messages. */
    public static void muteAll() {
        muted = true;
    }

    /** Unmute chat messages. */
    public static void unmuteAll() {
        muted = false;
    }

    /** @return whether chat is currently muted. */
    public static boolean isMuted() {
        return muted;
    }

    /** Clear the chat for all online players. */
    public static void clearChat() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < 100; i++) {
                p.sendMessage("");
            }
        }
    }

    /** Set a player's active chat channel. */
    public static void setChannel(UUID player, ChatChannel channel) {
        channels.put(player, channel);
    }

    /** Get a player's current chat channel. Defaults to region chat. */
    public static ChatChannel getChannel(UUID player) {
        return channels.getOrDefault(player, ChatChannel.REGION);
    }

    /** Initialize managers used for channel routing. */
    public static void init(PartyManager pm, GuildManager gm) {
        partyManager = pm;
        guildManager = gm;
    }

    /** Send a message to a player's active channel. */
    public static void sendChannelMessage(Player player, Component base) {
        ChatChannel channel = getChannel(player.getUniqueId());
        switch (channel) {
            case PARTY -> {
                Party party = partyManager.getParty(player.getUniqueId());
                if (party == null) {
                    player.sendMessage(ChatColor.RED + "You are not in a party.");
                    setChannel(player.getUniqueId(), ChatChannel.REGION);
                    return;
                }
                Component prefix = Component.text("[Party] ", NamedTextColor.GREEN);
                for (UUID memberId : party.getMembers()) {
                    Player member = Bukkit.getPlayer(memberId);
                    if (member != null && member.isOnline()) {
                        member.sendMessage(prefix.append(base));
                    }
                }
            }
            case GUILD -> {
                Guild guild = guildManager.getGuild(player.getUniqueId());
                if (guild == null) {
                    player.sendMessage(ChatColor.RED + "You are not in a guild.");
                    setChannel(player.getUniqueId(), ChatChannel.REGION);
                    return;
                }
                Component prefix = Component.text("[Guild] ", NamedTextColor.AQUA);
                for (UUID memberId : guild.getMembers()) {
                    Player member = Bukkit.getPlayer(memberId);
                    if (member != null && member.isOnline()) {
                        member.sendMessage(prefix.append(base));
                    }
                }
            }
            case GLOBAL -> {
                Component prefix = Component.text("[Global] ", NamedTextColor.GRAY);
                for (Player target : Bukkit.getOnlinePlayers()) {
                    target.sendMessage(prefix.append(base));
                }
            }
            case STAFF -> {
                if (!player.hasPermission("levelplugin.staffchat")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use staff chat.");
                    setChannel(player.getUniqueId(), ChatChannel.REGION);
                    return;
                }
                Component prefix = Component.text("[Staff] ", NamedTextColor.DARK_AQUA);
                for (Player target : Bukkit.getOnlinePlayers()) {
                    if (target.hasPermission("levelplugin.staffchat")) {
                        target.sendMessage(prefix.append(base));
                    }
                }
            }
            case REGION -> {
                for (Player target : Bukkit.getOnlinePlayers()) {
                    if (target.getWorld().equals(player.getWorld()) &&
                            target.getLocation().distanceSquared(player.getLocation()) <= 100 * 100) {
                        target.sendMessage(base);
                    }
                }
            }
        }
    }
}
