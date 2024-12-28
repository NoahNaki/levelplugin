// CooldownUtil.java
package me.nakilex.levelplugin.effects.utils;

import java.util.HashMap;
import java.util.UUID;

public class CooldownUtil {

    private static final HashMap<UUID, Long> cooldowns = new HashMap<>();

    public static void setCooldown(UUID playerId, long cooldownTimeMillis) {
        cooldowns.put(playerId, System.currentTimeMillis() + cooldownTimeMillis);
    }

    public static boolean isOnCooldown(UUID playerId) {
        return cooldowns.containsKey(playerId) && System.currentTimeMillis() < cooldowns.get(playerId);
    }

    public static long getRemainingCooldown(UUID playerId) {
        return cooldowns.containsKey(playerId) ? Math.max(0, cooldowns.get(playerId) - System.currentTimeMillis()) : 0;
    }
}
