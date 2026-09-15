package me.nakilex.levelplugin.playerhead;

import org.bukkit.plugin.Plugin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds a standalone resource pack containing only the {@code rendertype_text} shader override,
 * independent of Nexo's generated pack.
 * <p>
 * Nexo's pack.zip ships per-Minecraft-version "overlay" folders (declared in its {@code pack.mcmeta},
 * one per supported {@code pack_format} range) that vanilla resource-pack loading applies on top of
 * the base pack. The overlay active for this server's own client version carries Nexo's own
 * unmodified {@code rendertype_text}, which silently shadows whatever {@link PlayerHeadResourcePackManager}
 * stages into Nexo's base {@code assets/} tree - verified empirically: Nexo regenerates every overlay
 * from its own internal vanilla-asset cache on every reload, ignoring anything placed in the matching
 * overlay directory by hand. A second, independent pack sent by {@link PlayerModelResourcePackDispatcher}
 * after Nexo's own sidesteps the overlay mechanism entirely - the client stacks multiple server-sent
 * packs, and a later one wins per-path over an earlier one.
 */
public final class PlayerModelResourcePack {
    private static final String PACK_MCMETA = "{\"pack\":{\"pack_format\":22,\"supported_formats\":[1,99],"
            + "\"description\":\"LevelPlugin player-model shader\"}}";

    private final byte[] zipBytes;
    private final String sha1Hex;

    public PlayerModelResourcePack(Plugin plugin) throws IOException {
        this.zipBytes = build(plugin);
        this.sha1Hex = sha1Hex(zipBytes);
    }

    public byte[] zipBytes() {
        return zipBytes;
    }

    public String sha1Hex() {
        return sha1Hex;
    }

    private static byte[] build(Plugin plugin) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            writeEntry(zip, "pack.mcmeta", PACK_MCMETA.getBytes(StandardCharsets.UTF_8));
            writeEntry(zip, "assets/minecraft/shaders/core/rendertype_text.vsh",
                    readResource(plugin, PlayerHeadResourcePackManager.SOURCE_ROOT + PlayerHeadResourcePackManager.VERTEX_SHADER));
            writeEntry(zip, "assets/minecraft/shaders/core/rendertype_text.fsh",
                    readResource(plugin, PlayerHeadResourcePackManager.SOURCE_ROOT + PlayerHeadResourcePackManager.FRAGMENT_SHADER));
        }
        return buffer.toByteArray();
    }

    private static void writeEntry(ZipOutputStream zip, String name, byte[] content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content);
        zip.closeEntry();
    }

    private static byte[] readResource(Plugin plugin, String resource) throws IOException {
        try (InputStream stream = plugin.getResource(resource)) {
            if (stream == null) throw new IOException("Missing bundled resource: " + resource);
            return stream.readAllBytes();
        }
    }

    private static String sha1Hex(byte[] data) throws IOException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(data);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IOException("SHA-1 unavailable", exception);
        }
    }
}
