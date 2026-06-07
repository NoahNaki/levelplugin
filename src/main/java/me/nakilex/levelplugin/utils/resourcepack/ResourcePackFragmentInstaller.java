package me.nakilex.levelplugin.utils.resourcepack;

import org.bukkit.plugin.java.JavaPlugin;

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

/** Reusable installer for bundled resource-pack fragments that Nexo merges from external_packs. */
public final class ResourcePackFragmentInstaller {
    private final JavaPlugin plugin;
    private final String bundledFragment;
    private final Path installedPack;

    public ResourcePackFragmentInstaller(JavaPlugin plugin, String bundledFragment, Path installedPack) {
        this.plugin = plugin;
        this.bundledFragment = bundledFragment;
        this.installedPack = installedPack;
    }

    public void install() throws IOException, URISyntaxException {
        Files.createDirectories(installedPack.getParent());
        URL resource = plugin.getClass().getClassLoader().getResource(bundledFragment);
        if (resource == null) {
            return;
        }

        URI uri = resource.toURI();
        if ("jar".equalsIgnoreCase(uri.getScheme())) {
            copyFromJar(uri);
        } else {
            copyTree(Path.of(uri));
        }
    }

    private void copyFromJar(URI uri) throws IOException {
        FileSystem fileSystem = null;
        boolean closeFileSystem = false;
        try {
            try {
                fileSystem = FileSystems.newFileSystem(uri, Map.of());
                closeFileSystem = true;
            } catch (FileSystemAlreadyExistsException ignored) {
                fileSystem = FileSystems.getFileSystem(uri);
            }
            copyTree(fileSystem.getPath("/" + bundledFragment));
        } finally {
            if (closeFileSystem && fileSystem != null) {
                fileSystem.close();
            }
        }
    }

    private void copyTree(Path source) throws IOException {
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path destination = installedPack.resolve(source.relativize(path).toString());
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
