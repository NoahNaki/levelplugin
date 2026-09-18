package me.nakilex.playerspoofer;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Reads a LuckPerms <em>group's</em> prefix, reflectively.
 *
 * <p>Reflection rather than a compile-time dependency, matching how the rest of the plugin talks to
 * LuckPerms: it stays optional, and a missing or renamed API degrades to "no prefix" instead of
 * breaking chat.</p>
 *
 * <p>Only groups are read. Nothing is written, and no user is ever looked up or created - see
 * {@link FakeChatAppearance} for why that matters here.</p>
 */
final class LuckPermsGroupPrefix {

    private LuckPermsGroupPrefix() {
    }

    /** The group's prefix with its colour codes intact, or {@code null} if there isn't one. */
    static String of(String groupName) {
        try {
            Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object api = providerClass.getMethod("get").invoke(null);

            Object groupManager = api.getClass().getMethod("getGroupManager").invoke(api);
            Method getGroup = groupManager.getClass().getMethod("getGroup", String.class);
            getGroup.setAccessible(true);
            Object group = getGroup.invoke(groupManager, groupName);
            if (group == null) {
                return null;
            }

            Method getCachedData = group.getClass().getMethod("getCachedData");
            getCachedData.setAccessible(true);
            Object cachedData = getCachedData.invoke(group);
            Method getMetaData = cachedData.getClass().getMethod("getMetaData");
            getMetaData.setAccessible(true);
            Object metaData = getMetaData.invoke(cachedData);
            Method getPrefix = metaData.getClass().getMethod("getPrefix");
            getPrefix.setAccessible(true);

            Object prefix = getPrefix.invoke(metaData);
            if (prefix instanceof Optional<?> optional) {
                prefix = optional.orElse(null);
            }
            String text = prefix == null ? null : prefix.toString().trim();
            return text == null || text.isEmpty() ? null : text;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
