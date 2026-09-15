package me.nakilex.levelplugin.playerhead;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Copies the player-head font, its pixel textures, and the {@code rendertype_text} shader override
 * out of the jar and into Nexo's pack, so the glyphs {@link PlayerHeadRenderer} emits and the 3D
 * model {@link PlayerModelComponent} draws actually reach the client.
 * <p>
 * Targets {@code Nexo/pack/assets/minecraft/...} rather than {@code external_packs/}: Nexo merges
 * that directory into the pack it generates (verified - the repo's own {@code space.json} and mouse
 * textures come out the other side), and these paths belong to this feature alone, so nothing
 * else can be overwritten. Nexo still has to regenerate its pack before changes reach players.
 */
public final class PlayerHeadResourcePackManager {
    static final String SOURCE_ROOT = "resourcepack/assets/minecraft/";
    private static final String FONT = "font/playerhead.json";
    static final String VERTEX_SHADER = "shaders/core/rendertype_text.vsh";
    static final String FRAGMENT_SHADER = "shaders/core/rendertype_text.fsh";

    private PlayerHeadResourcePackManager() {
    }

    public static void install(Plugin plugin) {
        if (Bukkit.getPluginManager().getPlugin("Nexo") == null) return;
        Path pluginsDirectory = plugin.getDataFolder().toPath().getParent();
        if (pluginsDirectory == null) return;
        Path target = pluginsDirectory.resolve("Nexo/pack/assets/minecraft");

        List<String> assets = new ArrayList<>(List.of(FONT, VERTEX_SHADER, FRAGMENT_SHADER));
        for (int i = 1; i <= 8; i++) {
            assets.add("textures/playerhead/pixel" + i + ".png");
        }

        List<String> updated = new ArrayList<>();
        for (String asset : assets) {
            if (copyIfChanged(plugin, SOURCE_ROOT + asset, target.resolve(asset))) {
                updated.add(asset);
            }
        }
        if (!updated.isEmpty()) {
            plugin.getLogger().info("Installed " + updated.size() + " player-head pack file(s) into Nexo. "
                    + "Regenerate the Nexo pack for leaderboard heads to render.");
        }
    }

    private static boolean copyIfChanged(Plugin plugin, String resource, Path destination) {
        try (InputStream stream = plugin.getResource(resource)) {
            if (stream == null) {
                plugin.getLogger().warning("Missing bundled player-head asset: " + resource);
                return false;
            }
            byte[] bundled = stream.readAllBytes();
            if (Files.isRegularFile(destination) && Arrays.equals(bundled, Files.readAllBytes(destination))) {
                return false;
            }
            Files.createDirectories(destination.getParent());
            Files.write(destination, bundled);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not install player-head asset " + resource + ": " + exception.getMessage());
            return false;
        }
    }
}
