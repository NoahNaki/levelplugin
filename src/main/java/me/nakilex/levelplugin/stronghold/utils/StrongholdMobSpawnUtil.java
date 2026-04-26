package me.nakilex.levelplugin.stronghold.utils;

import me.nakilex.levelplugin.mob.custom.CustomMobManager;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Shared spawn helper for Stronghold combat events. */
public final class StrongholdMobSpawnUtil {
    private StrongholdMobSpawnUtil() {
    }

    public static LivingEntity spawnStrongholdHostile(CustomMobManager customMobManager,
                                                      List<String> mobPool,
                                                      Location at) {
        if (at == null || at.getWorld() == null) {
            return null;
        }
        if (customMobManager != null && mobPool != null && !mobPool.isEmpty()) {
            String mobId = mobPool.get(ThreadLocalRandom.current().nextInt(mobPool.size()));
            List<LivingEntity> spawned = customMobManager.spawn(mobId, at, 1);
            if (!spawned.isEmpty()) {
                return spawned.get(0);
            }
        }
        Entity fallback = at.getWorld().spawnEntity(at, EntityType.ZOMBIE);
        return fallback instanceof LivingEntity living ? living : null;
    }
}
