package me.nakilex.levelplugin.npc.system.skin;

import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.npc.system.trait.SkinTrait;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight internal skin cache/service used by NPC traits without Citizens coupling.
 */
public final class NpcSkinService {
    private static final Map<String, SkinProperty> CACHE = new ConcurrentHashMap<>();

    private NpcSkinService() {
    }

    public static void cache(String skinName, String signature, String texture) {
        if (skinName == null || skinName.isBlank() || signature == null || texture == null) {
            return;
        }
        CACHE.put(skinName.toLowerCase(), new SkinProperty(texture, signature));
    }

    public static SkinProperty getCached(String skinName) {
        if (skinName == null || skinName.isBlank()) {
            return null;
        }
        return CACHE.get(skinName.toLowerCase());
    }

    public static CompletableFuture<SkinProperty> fetchOrGetCached(String skinName) {
        SkinProperty cached = getCached(skinName);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return CompletableFuture.supplyAsync(() -> null);
    }

    public static void applyToNpc(NPC npc, SkinTrait trait, boolean refreshSpawn) {
        if (npc == null || trait == null) {
            return;
        }
        SkinProperty property = resolveProperty(trait);
        if (property == null) {
            return;
        }
        npc.data().set("skin_name", trait.getSkinName());
        npc.data().set("skin_texture", property.texture());
        npc.data().set("skin_signature", property.signature());
        if (refreshSpawn && npc.isSpawned()) {
            Bukkit.getScheduler().runTask(me.nakilex.levelplugin.Main.getInstance(), () -> {
                if (!npc.isSpawned() || npc.getStoredLocation() == null) {
                    return;
                }
                npc.despawn();
                npc.spawn(npc.getStoredLocation());
            });
        }
    }

    private static SkinProperty resolveProperty(SkinTrait trait) {
        if (trait.getTexture() != null && trait.getSignature() != null) {
            return new SkinProperty(trait.getTexture(), trait.getSignature());
        }
        return getCached(trait.getSkinName());
    }

    public record SkinProperty(String texture, String signature) {
    }
}
