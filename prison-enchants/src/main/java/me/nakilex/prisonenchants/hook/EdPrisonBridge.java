package me.nakilex.prisonenchants.hook;

import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Reflection keeps this companion plugin buildable without redistributing the
 * premium EdPrison jar. All method names used here are part of EdPrison 6.0.5's
 * public API.
 */
public final class EdPrisonBridge {
    private final JavaPlugin plugin;
    private Object enchantApi;
    private Object economyApi;
    private Method getEnchantLevel;
    private Method addEconomy;

    public EdPrisonBridge(JavaPlugin plugin) {
        this.plugin = plugin;
        connect();
    }

    private void connect() {
        try {
            Class<?> managerType = Class.forName("com.edwardbelt.edprison.api.ApiManager");
            Object manager = managerType.getConstructor().newInstance();
            enchantApi = managerType.getMethod("getEnchantApi").invoke(manager);
            economyApi = managerType.getMethod("getEconomyApi").invoke(manager);
            getEnchantLevel = enchantApi.getClass().getMethod("getLevel", UUID.class, String.class);
            addEconomy = economyApi.getClass().getMethod("addEco", UUID.class, String.class, double.class);
            plugin.getLogger().info("Connected to EdPrison 6.0.5 API.");
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().severe("Could not connect to the EdPrison API: " + ex.getMessage());
            enchantApi = null;
            economyApi = null;
        }
    }

    public double enchantLevel(UUID playerId, String enchantId) {
        if (enchantApi == null) return 0.0;
        try {
            Object value = getEnchantLevel.invoke(enchantApi, playerId, enchantId);
            return value instanceof Number number ? number.doubleValue() : 0.0;
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().warning("Could not read " + enchantId + " level: " + ex.getMessage());
            return 0.0;
        }
    }

    public void addCurrency(UUID playerId, String currency, double amount) {
        if (economyApi == null || amount <= 0.0) return;
        try {
            addEconomy.invoke(economyApi, playerId, currency, amount);
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().warning("Could not credit EdPrison currency " + currency + ": " + ex.getMessage());
        }
    }
}
