package me.nakilex.levelplugin.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

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
     * Create a head with a custom Base64 skin texture.
     */
    public static ItemStack createCustomHead(String base64, String name, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.getProperties().clear();
        profile.getProperties().add(new ProfileProperty("textures", base64));
        meta.setPlayerProfile(profile);
        if (name != null) meta.setDisplayName(name);
        if (lore != null) meta.setLore(lore);
        head.setItemMeta(meta);
        return head;
    }
}
