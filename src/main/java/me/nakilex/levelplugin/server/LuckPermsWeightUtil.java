package me.nakilex.levelplugin.server;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.UUID;

final class LuckPermsWeightUtil {
    private static Object api;
    private static boolean checked;

    private LuckPermsWeightUtil() {
    }

    static Integer getWeight(Player player) {
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
            Object user = getUser.invoke(userManager, player.getUniqueId());
            if (user == null) {
                return null;
            }
            Method getCachedData = user.getClass().getMethod("getCachedData");
            Object cachedData = getCachedData.invoke(user);
            Method getMetaData = cachedData.getClass().getMethod("getMetaData");
            Object metaData = getMetaData.invoke(cachedData);
            Method getMetaValue = metaData.getClass().getMethod("getMetaValue", String.class);
            Object weight = getMetaValue.invoke(metaData, "weight");
            if (weight == null) {
                return null;
            }
            return Integer.parseInt(weight.toString());
        } catch (ReflectiveOperationException | NumberFormatException ignored) {
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
