package me.nakilex.levelplugin.storage.utils;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.UUID;

public class SkullCreator {

    // Create a player head with a custom texture
    public static ItemStack createSkull(String base64) {
        ItemStack skull = new ItemStack(org.bukkit.Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        try {
            UUID uuid = UUID.randomUUID();
            Object profile = createGameProfile(uuid, base64);
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        skull.setItemMeta(meta);
        return skull;
    }

    private static Object createGameProfile(UUID uuid, String base64) throws Exception {
        Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
        Object profile = gameProfileClass.getConstructor(UUID.class, String.class).newInstance(uuid, null);

        Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
        Object property = propertyClass.getConstructor(String.class, String.class).newInstance("textures", base64);

        Field propertiesField = profile.getClass().getDeclaredField("properties");
        propertiesField.setAccessible(true);
        Object properties = propertiesField.get(profile);
        properties.getClass().getMethod("put", Object.class, Object.class).invoke(properties, "textures", property);

        return profile;
    }

    // Encode a URL to Base64 for texture creation
    public static String encodeTextureURL(String url) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes());
    }
}
