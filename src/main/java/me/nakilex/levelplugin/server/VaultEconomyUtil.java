package me.nakilex.levelplugin.server;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;

/**
 * Reads a player's Vault economy balance ("Money" on the prison scoreboard) without a compile-time
 * dependency on Vault, mirroring the reflection pattern in {@link LuckPermsWeightUtil}. Returns null
 * when Vault or an economy plugin isn't present, so callers can fall back gracefully.
 */
public final class VaultEconomyUtil {
    private static Object economy;
    private static boolean checked;

    private VaultEconomyUtil() {
    }

    /** The player's raw balance, or null if no Vault economy provider is registered. */
    public static Double getBalance(OfflinePlayer player) {
        Object provider = getEconomy();
        if (provider == null) return null;
        try {
            Method getBalance = provider.getClass().getMethod("getBalance", OfflinePlayer.class);
            return (Double) getBalance.invoke(provider, player);
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            return null;
        }
    }

    private static Object getEconomy() {
        if (checked) return economy;
        checked = true;
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(economyClass);
            if (registration != null) {
                economy = registration.getProvider();
            }
        } catch (ClassNotFoundException ignored) {
            economy = null;
        }
        return economy;
    }
}
