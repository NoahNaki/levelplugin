package me.nakilex.levelplugin.utils;

import dev.drawethree.xprison.api.XPrisonAPI;
import dev.drawethree.xprison.api.enchants.XPrisonEnchantsAPI;
import dev.drawethree.xprison.api.enchants.model.XPrisonEnchantment;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordinates LevelPlugin's temporary double-jump flight flag with real flight
 * sources such as X-Prison's Fly enchant.
 *
 * <p>Bukkit exposes only one {@code allowFlight} boolean. If every subsystem
 * writes that flag independently, one feature can accidentally revoke flight
 * granted by another. Double-jump therefore tracks whether it was the system
 * that actually enabled the flag and only releases flags it owns.
 */
public final class FlightUtil {

    private static final Set<UUID> DOUBLE_JUMP_FLIGHT_OWNERS = ConcurrentHashMap.newKeySet();

    private FlightUtil() {
    }

    /**
     * Ensures the player can emit Bukkit's flight-toggle event for a double jump.
     * Existing flight is treated as external and is never claimed by double-jump.
     */
    public static void ensureDoubleJumpFlight(Player player) {
        if (player == null || isIntrinsicFlightMode(player)) return;

        UUID id = player.getUniqueId();
        if (hasXPrisonFlyEnchant(player) || player.isFlying()) {
            DOUBLE_JUMP_FLIGHT_OWNERS.remove(id);
            return;
        }

        if (!player.getAllowFlight()) {
            player.setAllowFlight(true);
            DOUBLE_JUMP_FLIGHT_OWNERS.add(id);
        }
    }

    /**
     * Releases the flight flag only when double-jump was the feature that granted it.
     * X-Prison Fly and active sustained flight always win over this temporary flag.
     */
    public static void releaseDoubleJumpFlight(Player player) {
        if (player == null) return;

        UUID id = player.getUniqueId();
        if (!DOUBLE_JUMP_FLIGHT_OWNERS.remove(id)) return;
        if (isIntrinsicFlightMode(player) || hasXPrisonFlyEnchant(player) || player.isFlying()) return;

        player.setAllowFlight(false);
    }

    /** Stop considering the current allow-flight flag ours without changing Bukkit state. */
    public static void relinquishDoubleJumpOwnership(Player player) {
        if (player != null) DOUBLE_JUMP_FLIGHT_OWNERS.remove(player.getUniqueId());
    }

    public static boolean isDoubleJumpFlightOwned(Player player) {
        return player != null && DOUBLE_JUMP_FLIGHT_OWNERS.contains(player.getUniqueId());
    }

    public static void clear(UUID playerId) {
        if (playerId != null) DOUBLE_JUMP_FLIGHT_OWNERS.remove(playerId);
    }

    /**
     * True while the main-hand X-Prison tool currently carries the built-in Fly enchant.
     * The check is deliberately soft so LevelPlugin still works when X-Prison is absent.
     */
    public static boolean hasXPrisonFlyEnchant(Player player) {
        if (player == null || !Bukkit.getPluginManager().isPluginEnabled("X-Prison")) return false;

        try {
            XPrisonAPI api = XPrisonAPI.getInstance();
            if (api == null) return false;

            XPrisonEnchantsAPI enchantsApi = api.getEnchantsApi();
            if (enchantsApi == null) return false;

            XPrisonEnchantment fly = enchantsApi.getByName("fly");
            if (fly == null) return false;

            ItemStack held = player.getInventory().getItemInMainHand();
            if (held.getType() == Material.AIR) return false;

            return enchantsApi.getEnchantLevel(held, fly) > 0;
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static boolean isIntrinsicFlightMode(Player player) {
        GameMode mode = player.getGameMode();
        return mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR;
    }
}
