package me.nakilex.levelplugin.chat;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.guild.Guild;
import me.nakilex.levelplugin.guild.GuildManager;
import me.nakilex.levelplugin.party.Party;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.utils.ChatUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

/** Routes player chat to the appropriate channel. */
public class ChatChannelListener implements Listener {

    private final PartyManager partyManager;
    private final GuildManager guildManager;
    private static final double REGION_RANGE_SQ = 100 * 100;

    public ChatChannelListener(Main plugin) {
        this.partyManager = plugin.getPartyManager();
        this.guildManager = plugin.getGuildManager();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (ChatManager.isMuted() && !player.hasPermission("levelplugin.chat.bypass")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Chat is currently muted.");
            return;
        }

        ChatChannel channel = ChatManager.getChannel(player.getUniqueId());
        Component base = ChatUtil.buildMessage(player, event.getMessage());
        event.setCancelled(true);

        switch (channel) {
            case PARTY -> {
                Party party = partyManager.getParty(player.getUniqueId());
                if (party == null) {
                    player.sendMessage(ChatColor.RED + "You are not in a party.");
                    ChatManager.setChannel(player.getUniqueId(), ChatChannel.REGION);
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
                    ChatManager.setChannel(player.getUniqueId(), ChatChannel.REGION);
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
            case REGION -> {
                for (Player target : Bukkit.getOnlinePlayers()) {
                    if (target.getWorld().equals(player.getWorld()) &&
                            target.getLocation().distanceSquared(player.getLocation()) <= REGION_RANGE_SQ) {
                        target.sendMessage(base);
                    }
                }
            }
        }
    }
}
