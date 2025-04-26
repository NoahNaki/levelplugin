package me.nakilex.levelplugin.potions.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PotionCooldownManager {

    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public boolean isOnCooldown(UUID uuid) {
        return cooldowns.containsKey(uuid) && cooldowns.get(uuid) > System.currentTimeMillis();
    }

    public void startCooldown(UUID uuid, int seconds) {
        cooldowns.put(uuid, System.currentTimeMillis() + (seconds * 2000L));
    }
}
