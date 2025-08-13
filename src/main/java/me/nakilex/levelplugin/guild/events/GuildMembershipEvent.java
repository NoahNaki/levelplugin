package me.nakilex.levelplugin.guild.events;

import me.nakilex.levelplugin.guild.Guild;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Fired when a player joins or leaves a guild.
 */
public class GuildMembershipEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();

    public enum Action { JOIN, LEAVE }

    private final Guild guild;
    private final Action action;

    public GuildMembershipEvent(Player player, Guild guild, Action action) {
        super(player);
        this.guild = guild;
        this.action = action;
    }

    public Guild getGuild() {
        return guild;
    }

    public Action getAction() {
        return action;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
