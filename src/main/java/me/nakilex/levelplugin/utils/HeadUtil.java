package me.nakilex.levelplugin.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/** Utility for creating player heads with custom skins. */
public final class HeadUtil {
    private HeadUtil() {}

    /**
     * Create a player head using an OfflinePlayer's skin.
     */
    public static ItemStack createPlayerHead(OfflinePlayer player, String name, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            if (name != null) meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    /**
     * Create a head with a custom skin texture from a Base64-encoded value.
     *
     * @param base64 Base64-encoded texture data containing a texture URL
     */
    public static ItemStack createCustomHead(String base64, String name, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
        String url = extractTextureUrl(base64);
        if (url != null) {
            try {
                profile.getTextures().setSkin(new URL(url));
            } catch (MalformedURLException ignored) {
            }
        }
        meta.setPlayerProfile(profile);
        if (name != null) meta.setDisplayName(name);
        if (lore != null) meta.setLore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private static String extractTextureUrl(String base64) {
        try {
            String decoded = new String(Base64.getDecoder().decode(base64));
            int urlIndex = decoded.indexOf("\"url\":\"");
            if (urlIndex == -1) return null;
            int start = urlIndex + 8; // length of "url":"
            int end = decoded.indexOf('"', start);
            if (end == -1) return null;
            return decoded.substring(start, end);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
