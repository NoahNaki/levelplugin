package me.nakilex.levelplugin.mob.utils;

import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps track of players that have reward holograms or chat messages disabled.
 */
public final class DropDisplayToggles {
    private static final Set<UUID> dropDetailsDisabled = new HashSet<>();
    private static final Set<UUID> chatDetailsDisabled = new HashSet<>();

    private DropDisplayToggles() {}

    public static boolean isDropDetailsEnabled(Player p) {
        return !dropDetailsDisabled.contains(p.getUniqueId());
    }

    public static boolean toggleDropDetails(Player p) {
        UUID u = p.getUniqueId();
        if (dropDetailsDisabled.contains(u)) dropDetailsDisabled.remove(u);
        else dropDetailsDisabled.add(u);
        return isDropDetailsEnabled(p);
    }

    public static boolean isChatEnabled(Player p) {
        return !chatDetailsDisabled.contains(p.getUniqueId());
    }

    public static boolean toggleChat(Player p) {
        UUID id = p.getUniqueId();
        if (chatDetailsDisabled.contains(id)) chatDetailsDisabled.remove(id);
        else chatDetailsDisabled.add(id);
        return isChatEnabled(p);
    }
}
