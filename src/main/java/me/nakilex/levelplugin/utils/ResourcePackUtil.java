package me.nakilex.levelplugin.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Utility helpers for re-sending the configured server resource pack to players.
 */
public final class ResourcePackUtil {
    private ResourcePackUtil() {
    }

    public static boolean refresh(Player player) {
        return refresh(player, Component.empty(), true);
    }

    public static boolean refresh(Player player, Component prompt, boolean force) {
        if (player == null) {
            return false;
        }
        String url = getServerResourcePackUrl(player.getServer());
        if (url == null || url.isBlank()) {
            return false;
        }
        return applyResourcePack(player, url, getServerResourcePackHash(player.getServer()), prompt, force);
    }

    private static String getServerResourcePackUrl(Server server) {
        if (server == null) {
            return null;
        }
        try {
            Method method = server.getClass().getMethod("getResourcePack");
            Object value = method.invoke(server);
            return value == null ? null : value.toString();
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private static byte[] getServerResourcePackHash(Server server) {
        if (server == null) {
            return null;
        }
        try {
            Method method = server.getClass().getMethod("getResourcePackHash");
            Object value = method.invoke(server);
            if (value instanceof byte[] bytes) {
                return bytes;
            }
            if (value instanceof String text) {
                return text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (ReflectiveOperationException ex) {
            return null;
        }
        return null;
    }

    private static boolean applyResourcePack(Player player, String url, byte[] hash, Component prompt, boolean force) {
        try {
            Method method = player.getClass().getMethod("setResourcePack", String.class, byte[].class, Component.class, boolean.class);
            method.invoke(player, url, hash, prompt == null ? Component.empty() : prompt, force);
            return true;
        } catch (ReflectiveOperationException ignored) {
            // continue
        }
        try {
            Method method = player.getClass().getMethod("setResourcePack", String.class, byte[].class, String.class, boolean.class);
            method.invoke(player, url, hash, "", force);
            return true;
        } catch (ReflectiveOperationException ignored) {
            // continue
        }
        try {
            Method method = player.getClass().getMethod("setResourcePack", String.class, byte[].class);
            method.invoke(player, url, hash);
            return true;
        } catch (ReflectiveOperationException ignored) {
            // continue
        }
        try {
            Method method = player.getClass().getMethod("setResourcePack", String.class);
            method.invoke(player, url);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
