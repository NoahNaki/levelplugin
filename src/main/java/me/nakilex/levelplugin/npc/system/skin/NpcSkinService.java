package me.nakilex.levelplugin.npc.system.skin;

import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.npc.system.trait.SkinTrait;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight internal skin cache/service used by NPC traits without Citizens coupling.
 */
public final class NpcSkinService {
    private static final Map<String, SkinProperty> CACHE = new ConcurrentHashMap<>();
    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([a-fA-F0-9]{32})\"");
    private static final Pattern TEXTURE_VALUE_PATTERN = Pattern.compile("\"value\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern TEXTURE_SIG_PATTERN = Pattern.compile("\"signature\"\\s*:\\s*\"([^\"]+)\"");

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
        if (skinName == null || skinName.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.supplyAsync(() -> fetchSkinProperty(skinName));
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

    public static CompletableFuture<SkinProperty> fetchAndApplyToNpc(NPC npc, SkinTrait trait, boolean refreshSpawn) {
        if (trait == null) {
            return CompletableFuture.completedFuture(null);
        }
        return fetchOrGetCached(trait.getSkinName()).thenApply(property -> {
            if (property == null) {
                return null;
            }
            cache(trait.getSkinName(), property.signature(), property.texture());
            trait.setSkinPersistent(trait.getSkinName(), property.signature(), property.texture());
            Plugin plugin = me.nakilex.levelplugin.Main.getInstance();
            Bukkit.getScheduler().runTask(plugin, () -> applyToNpc(npc, trait, refreshSpawn));
            return property;
        });
    }

    private static SkinProperty resolveProperty(SkinTrait trait) {
        if (trait.getTexture() != null && trait.getSignature() != null) {
            return new SkinProperty(trait.getTexture(), trait.getSignature());
        }
        return getCached(trait.getSkinName());
    }

    private static SkinProperty fetchSkinProperty(String skinName) {
        try {
            String profileJson = readJson("https://api.mojang.com/users/profiles/minecraft/" + skinName);
            String uuidText = extract(ID_PATTERN, profileJson);
            if (uuidText == null) {
                return null;
            }
            UUID uuid = UUID.fromString(uuidText.replaceFirst(
                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                    "$1-$2-$3-$4-$5"));
            String sessionJson = readJson("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "") + "?unsigned=false");
            String texture = extract(TEXTURE_VALUE_PATTERN, sessionJson);
            String signature = extract(TEXTURE_SIG_PATTERN, sessionJson);
            if (texture == null || signature == null) {
                return null;
            }
            SkinProperty property = new SkinProperty(texture, signature);
            CACHE.put(skinName.toLowerCase(), property);
            return property;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String readJson(String rawUrl) throws IOException {
        URL url = URI.create(rawUrl).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(7000);
        connection.setReadTimeout(7000);
        connection.setRequestProperty("User-Agent", "LevelPlugin-NpcSkinService");
        try (InputStream input = connection.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String extract(Pattern pattern, String body) {
        if (body == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }

    public record SkinProperty(String texture, String signature) {
    }
}
