package me.nakilex.npc.nms.v1_21_3.skin;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import me.nakilex.npc.core.model.SkinData;
import me.nakilex.npc.core.model.SkinRef;
import me.nakilex.npc.core.model.SkinSource;
import me.nakilex.npc.core.nms.SkinService;
import org.bukkit.Bukkit;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.profile.ProfileProperty;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DefaultSkinService implements SkinService {
    private final Plugin plugin;
    private final SkinResolver urlResolver;
    private final Cache<String, SkinData> cache;

    public DefaultSkinService(Plugin plugin, SkinResolver urlResolver, Duration cacheDuration) {
        this.plugin = plugin;
        this.urlResolver = urlResolver;
        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(cacheDuration)
                .build();
    }

    @Override
    public CompletableFuture<SkinData> resolveSkin(SkinRef ref) {
        if (ref == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Skin ref missing"));
        }
        String cacheKey = ref.getSource() + ":" + ref.getValue();
        SkinData cached = cache.getIfPresent(cacheKey);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return switch (ref.getSource()) {
            case TEXTURES -> CompletableFuture.completedFuture(new SkinData(ref.getValue(), ref.getSignature()));
            case PLAYER_NAME -> resolveProfile(Bukkit.createPlayerProfile(ref.getValue()));
            case PLAYER_UUID -> resolveProfile(Bukkit.createPlayerProfile(UUID.fromString(ref.getValue())));
            case URL -> urlResolver == null
                    ? CompletableFuture.failedFuture(new IllegalStateException("URL resolver not configured"))
                    : urlResolver.resolve(ref.getValue());
        }.thenApply(data -> {
            cache.put(cacheKey, data);
            return data;
        });
    }

    private CompletableFuture<SkinData> resolveProfile(PlayerProfile profile) {
        return CompletableFuture.supplyAsync(() -> {
            profile.complete();
            Set<ProfileProperty> properties = profile.getProperties();
            Optional<ProfileProperty> textures = properties.stream()
                    .filter(property -> property.getName().equals("textures"))
                    .findFirst();
            if (textures.isEmpty()) {
                PlayerTextures playerTextures = profile.getTextures();
                if (playerTextures.getSkin() == null) {
                    throw new IllegalStateException("No textures available for profile");
                }
                throw new IllegalStateException("Missing signed textures property for profile");
            }
            ProfileProperty property = textures.get();
            return new SkinData(property.getValue(), property.getSignature());
        }, Bukkit.getScheduler().getAsyncExecutor(plugin));
    }
}
