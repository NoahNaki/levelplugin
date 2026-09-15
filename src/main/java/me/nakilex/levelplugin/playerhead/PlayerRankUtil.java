package me.nakilex.levelplugin.playerhead;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;

/**
 * Resolves a player's server rank from LuckPerms without compiling against it.
 *
 * LuckPerms is an optional dependency here - the plugin already talks to it reflectively for group
 * weights in {@code server/LuckPermsWeightUtil}, so this follows the same pattern and simply returns
 * {@code null} whenever the plugin is absent or the lookup fails.
 */
public final class PlayerRankUtil {
    private static Object api;
    private static boolean checked;

    private PlayerRankUtil() {
    }

    /**
     * The player's rank prefix with its colour codes intact (e.g. {@code "&6[MVP]"}), or {@code null}
     * when LuckPerms has no prefix set for them.
     */
    public static String rankPrefix(Player player) {
        Object user = lookupUser(player);
        if (user == null) {
            return null;
        }
        try {
            Object metaData = cachedMetaData(user);
            if (metaData == null) {
                return null;
            }
            Method getPrefix = metaData.getClass().getMethod("getPrefix");
            Object prefix = getPrefix.invoke(metaData);
            String text = prefix == null ? null : prefix.toString().trim();
            return text == null || text.isEmpty() ? null : text;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    /** The player's primary group name, title-cased (e.g. {@code "Default"}), or {@code null}. */
    public static String primaryGroup(Player player) {
        Object user = lookupUser(player);
        if (user == null) {
            return null;
        }
        try {
            Method getPrimaryGroup = user.getClass().getMethod("getPrimaryGroup");
            Object group = getPrimaryGroup.invoke(user);
            String name = group == null ? null : group.toString().trim();
            if (name == null || name.isEmpty()) {
                return null;
            }
            return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1).toLowerCase(Locale.ROOT);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    /**
     * Best available rank label: the coloured prefix if one is set, otherwise the primary group name,
     * otherwise {@code "Player"} so the tooltip never shows a gap.
     */
    public static String rankLabel(Player player) {
        String prefix = rankPrefix(player);
        if (prefix != null) {
            return prefix;
        }
        String group = primaryGroup(player);
        return group == null ? "Player" : group;
    }

    private static Object cachedMetaData(Object user) throws ReflectiveOperationException {
        Method getCachedData = user.getClass().getMethod("getCachedData");
        Object cachedData = getCachedData.invoke(user);
        if (cachedData == null) {
            return null;
        }
        Method getMetaData = cachedData.getClass().getMethod("getMetaData");
        return getMetaData.invoke(cachedData);
    }

    private static Object lookupUser(Player player) {
        if (player == null) {
            return null;
        }
        Object apiInstance = getApi();
        if (apiInstance == null) {
            return null;
        }
        try {
            Method getUserManager = apiInstance.getClass().getMethod("getUserManager");
            Object userManager = getUserManager.invoke(apiInstance);
            Method getUser = userManager.getClass().getMethod("getUser", UUID.class);
            return getUser.invoke(userManager, player.getUniqueId());
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object getApi() {
        if (checked) {
            return api;
        }
        checked = true;
        try {
            Class<?> apiClass = Class.forName("net.luckperms.api.LuckPerms");
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration != null) {
                api = registration.getProvider();
            }
        } catch (ClassNotFoundException ignored) {
            api = null;
        }
        return api;
    }
}
