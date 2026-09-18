package me.nakilex.levelplugin.playerhead;

import me.nakilex.levelplugin.dialogdemo.market.DialogPixels;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Renders a player's face as an 8x8 grid of colored font glyphs, for inline use in leaderboard rows.
 * <p>
 * Each of the 8 "row" glyphs is a 1px-wide, 8px-tall bitmap with a single row of that column lit.
 * Stacking all 8 row glyphs at the same x position (via a -2px shift after each) paints one
 * fully-colored 1x8 pixel column with each pixel independently tinted, then a final -1px shift leaves
 * a net +1px advance into the next column.
 * <p>
 * Both fonts involved have to be ones Nexo leaves alone. Nexo regenerates
 * {@code assets/minecraft/font/default.json} wholesale during pack generation, so anything registered
 * there is dropped and renders as missing-glyph boxes: the pixels live in our own
 * {@code assets/minecraft/font/playerhead.json} and the shifts go through
 * {@link DialogPixels#shift(int)} ({@code minecraft:space}), both separate files that survive.
 */
public final class PlayerHeadRenderer {
    private static final Key HEAD_FONT = Key.key("minecraft:playerhead");
    private static final char[] ROW_GLYPH = {
            '', '', '', '', '', '', '', ''
    };

    /**
     * Steve's face, read straight off Mojang's default skin texture, column-major to match
     * {@link #buildFace}. Used for empty leaderboard slots and while a real skin is still loading,
     * so every row has a head and the columns line up.
     */
    private static final int[] DEFAULT_FACE = {
            0x2F200D, 0x2B1E0D, 0x2B1E0D, 0xAA7D66, 0xB4846D, 0x9C6346, 0x905E43, 0x6F452C,
            0x2B1E0D, 0x2B1E0D, 0xB6896C, 0xB4846D, 0xFFFFFF, 0xB37B62, 0x965F40, 0x6D432A,
            0x2F1F0F, 0x2B1E0D, 0xBD8E72, 0xAA7D66, 0x523D89, 0xB78272, 0x774235, 0x815339,
            0x281C0B, 0x332411, 0xC69680, 0xAD806D, 0xB57B67, 0x6A4030, 0x774235, 0x815339,
            0x241808, 0x422A12, 0xBD8B72, 0x9C725C, 0xBB8972, 0x6A4030, 0x774235, 0x7A4E33,
            0x261A0A, 0x3F2A15, 0xBD8E74, 0xBB8972, 0x523D89, 0xBE886C, 0x774235, 0x83553B,
            0x2B1E0D, 0x2C1E0E, 0xAC765A, 0x9C694C, 0xFFFFFF, 0xA26A47, 0x8F5E3E, 0x83553B,
            0x2A1D0D, 0x281C0B, 0x342512, 0x9C694C, 0xAA7D66, 0x805334, 0x815339, 0x7A4E33
    };

    private static final Component DEFAULT_HEAD = buildFace(DEFAULT_FACE);

    private static final Pattern SKIN_URL = Pattern.compile("\"url\"\s*:\s*\"([^\"]+)\"");

    private static final long REFRESH_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(10);

    private static final Map<UUID, Component> CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_FETCHED = new ConcurrentHashMap<>();
    private static final Set<UUID> PENDING = ConcurrentHashMap.newKeySet();

    private PlayerHeadRenderer() {
    }

    /**
     * This player's face, falling back to Steve for empty slots, skins that can't be resolved, and
     * the window before the first async fetch lands. Never empty, so every row is the same width.
     */
    public static Component getHead(JavaPlugin plugin, OfflinePlayer player) {
        if (player == null) return DEFAULT_HEAD;
        UUID id = player.getUniqueId();
        // Padding rows carry a nil UUID and will never resolve - don't spend a profile lookup on them.
        if (id.getMostSignificantBits() == 0 && id.getLeastSignificantBits() == 0) return DEFAULT_HEAD;
        Component cached = CACHE.get(id);
        long lastFetched = LAST_FETCHED.getOrDefault(id, 0L);
        if (cached == null || System.currentTimeMillis() - lastFetched > REFRESH_INTERVAL_MILLIS) {
            requestRender(plugin, player);
        }
        return cached == null ? DEFAULT_HEAD : cached;
    }

    /**
     * The face for a skin we already hold as a Mojang {@code textures} property, keyed by
     * {@code id}. Spoofed players wear a donor account's skin rather than the skin belonging to
     * their own name, so looking them up as an OfflinePlayer would render the wrong face.
     */
    public static Component getHead(JavaPlugin plugin, UUID id, String base64Texture) {
        if (id == null || base64Texture == null || base64Texture.isBlank()) return DEFAULT_HEAD;
        Component cached = CACHE.get(id);
        long lastFetched = LAST_FETCHED.getOrDefault(id, 0L);
        if (cached == null || System.currentTimeMillis() - lastFetched > REFRESH_INTERVAL_MILLIS) {
            requestRender(plugin, id, () -> skinUrlFromTexture(base64Texture));
        }
        return cached == null ? DEFAULT_HEAD : cached;
    }

    /** Pulls the skin URL out of a base64-encoded Mojang textures property. */
    private static URL skinUrlFromTexture(String base64Texture) {
        try {
            String json = new String(Base64.getDecoder().decode(base64Texture), StandardCharsets.UTF_8);
            Matcher matcher = SKIN_URL.matcher(json);
            // Mojang's JSON escapes the slashes in the texture URL.
            return matcher.find() ? URI.create(matcher.group(1).replace("\\/", "/")).toURL() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private static void requestRender(JavaPlugin plugin, OfflinePlayer player) {
        requestRender(plugin, player.getUniqueId(), () -> resolveSkinUrl(player));
    }

    private static void requestRender(JavaPlugin plugin, UUID id, Supplier<URL> skinUrl) {
        if (!PENDING.add(id)) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Component glyphs = render(skinUrl.get());
                if (glyphs != null) {
                    CACHE.put(id, glyphs);
                    LAST_FETCHED.put(id, System.currentTimeMillis());
                }
            } finally {
                PENDING.remove(id);
            }
        });
    }

    private static Component render(URL skinUrl) {
        if (skinUrl == null) return null;
        BufferedImage skin;
        try {
            skin = ImageIO.read(skinUrl);
        } catch (Exception ex) {
            return null;
        }
        if (skin == null) return null;

        BufferedImage face = skin.getSubimage(8, 8, 8, 8);
        BufferedImage hat = skin.getHeight() >= 64 ? skin.getSubimage(40, 8, 8, 8) : null;

        int[] colors = new int[64];
        int index = 0;
        for (int col = 0; col < 8; col++) {
            for (int row = 0; row < 8; row++) {
                int argb = face.getRGB(col, row);
                if (hat != null) {
                    int hatArgb = hat.getRGB(col, row);
                    if ((hatArgb >>> 24) != 0) argb = hatArgb;
                }
                colors[index++] = argb & 0xFFFFFF;
            }
        }
        return buildFace(colors);
    }

    /** Paints 64 colors as stacked pixel columns. Colors are column-major: index = col * 8 + row. */
    private static Component buildFace(int[] colors) {
        Component out = Component.empty();
        int index = 0;
        for (int col = 0; col < 8; col++) {
            for (int row = 0; row < 8; row++) {
                out = out.append(Component.text(ROW_GLYPH[row], TextColor.color(colors[index++])).font(HEAD_FONT));
                out = out.append(DialogPixels.shift(row < 7 ? -2 : -1));
            }
        }
        return out;
    }

    private static URL resolveSkinUrl(OfflinePlayer player) {
        try {
            PlayerProfile profile = player.getPlayerProfile();
            PlayerTextures textures = profile.getTextures();
            if (textures.getSkin() == null) {
                profile = profile.update().get(10, TimeUnit.SECONDS);
                textures = profile.getTextures();
            }
            return textures.getSkin();
        } catch (Exception ex) {
            return null;
        }
    }
}
