package me.nakilex.levelplugin.xprison;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/** Installs the custom level-up totem model into Nexo's external pack sources. */
public final class LevelUpTotemResourcePackManager {
    private static final String BUNDLED_FRAGMENT = "resourcepack/level_up_totem";

    private LevelUpTotemResourcePackManager() { }

    public static void install(Main plugin) {
        if (Bukkit.getPluginManager().getPlugin("Nexo") == null) return;

        Path pluginsDirectory = plugin.getDataFolder().toPath().getParent();
        if (pluginsDirectory == null) pluginsDirectory = Path.of("plugins");
        Path destination = pluginsDirectory.resolve("Nexo/pack/external_packs/levelplugin-level-up-totem");

        try {
            URL resource = plugin.getClass().getClassLoader().getResource(BUNDLED_FRAGMENT);
            if (resource == null) {
                plugin.getLogger().warning("Bundled pickaxe level-up totem resource pack was not found.");
                return;
            }
            Files.createDirectories(destination);
            URI uri = resource.toURI();
            if ("jar".equalsIgnoreCase(uri.getScheme())) {
                copyFromJar(uri, destination);
            } else {
                copyTree(Path.of(uri), destination);
            }
            plugin.getLogger().info("Installed the pickaxe level-up totem model into Nexo external_packs.");
        } catch (IOException | URISyntaxException exception) {
            plugin.getLogger().warning("Could not install the pickaxe level-up totem model: " + exception.getMessage());
        }
    }

    private static void copyFromJar(URI uri, Path destination) throws IOException {
        FileSystem fileSystem = null;
        boolean close = false;
        try {
            try {
                fileSystem = FileSystems.newFileSystem(uri, Map.of());
                close = true;
            } catch (FileSystemAlreadyExistsException ignored) {
                fileSystem = FileSystems.getFileSystem(uri);
            }
            copyTree(fileSystem.getPath("/" + BUNDLED_FRAGMENT), destination);
        } finally {
            if (close && fileSystem != null) fileSystem.close();
        }
    }

    private static void copyTree(Path source, Path destinationRoot) throws IOException {
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path destination = destinationRoot.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
