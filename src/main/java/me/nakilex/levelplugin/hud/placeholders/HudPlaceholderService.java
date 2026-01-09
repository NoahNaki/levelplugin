package me.nakilex.levelplugin.hud.placeholders;

import me.clip.placeholderapi.PlaceholderAPI;
import me.nakilex.levelplugin.utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HudPlaceholderService {
    private static final Pattern INTERNAL_PATTERN = Pattern.compile("%hud_([a-z0-9_]+)%", Pattern.CASE_INSENSITIVE);

    private final HudPlaceholderCache cache;
    private final Map<String, java.util.function.Function<Player, String>> internal;
    private final boolean placeholderApiAvailable;

    public HudPlaceholderService(HudPlaceholderRegistry registry, HudPlaceholderCache cache) {
        this.cache = cache;
        this.internal = registry.getPlaceholders();
        this.placeholderApiAvailable = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public void setCacheTtl(long ttlMs) {
        cache.setTtlMs(ttlMs);
    }

    public String resolve(Player player, String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String cached = cache.get(player, text);
        if (cached != null) {
            return cached;
        }
        String resolved = resolveInternal(player, text);
        if (placeholderApiAvailable && player != null && resolved.contains("%")) {
            resolved = PlaceholderAPI.setPlaceholders(player, resolved);
        }
        resolved = ChatUtil.applyEmojis(resolved);
        resolved = ChatColor.translateAlternateColorCodes('&', resolved);
        cache.put(player, text, resolved);
        return resolved;
    }

    private String resolveInternal(Player player, String text) {
        Matcher matcher = INTERNAL_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1).toLowerCase();
            java.util.function.Function<Player, String> resolver = internal.get(key);
            String replacement = resolver == null ? matcher.group() : resolver.apply(player);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement == null ? "" : replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
