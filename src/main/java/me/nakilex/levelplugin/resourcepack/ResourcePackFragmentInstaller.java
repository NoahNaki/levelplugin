package me.nakilex.levelplugin.resourcepack;

import org.bukkit.Bukkit;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Shared installer for small bundled resource-pack fragments that Nexo later merges into its final pack.
 */
public final class ResourcePackFragmentInstaller {
    private final JavaPlugin plugin;
    private final String displayName;
    private final String bundledResourcePath;
    private final String externalPackFolderName;
    private final List<String> requiredFiles;
    private final Path nexoExternalPacks;
    private final Path installedPack;

    public ResourcePackFragmentInstaller(JavaPlugin plugin, String displayName, String bundledResourcePath,
                                         String externalPackFolderName, List<String> requiredFiles) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.bundledResourcePath = normalizeResourcePath(bundledResourcePath);
        this.externalPackFolderName = Objects.requireNonNull(externalPackFolderName, "externalPackFolderName");
        this.requiredFiles = List.copyOf(Objects.requireNonNull(requiredFiles, "requiredFiles"));
        Path pluginsDirectory = plugin.getDataFolder().toPath().getParent();
        if (pluginsDirectory == null) pluginsDirectory = Path.of("plugins");
        this.nexoExternalPacks = pluginsDirectory.resolve("Nexo/pack/external_packs");
        this.installedPack = nexoExternalPacks.resolve(externalPackFolderName);
    }

    /** Copies the bundled fragment when both the source resource and the Nexo external_packs folder are present. */
    public void installBundledFragment() {
        Logger logger = plugin.getLogger();
        if (!Files.isDirectory(nexoExternalPacks)) {
            logger.warning(displayName + " resource-pack fragment not installed: Nexo external_packs folder does not exist at "
                    + nexoExternalPacks + ".");
            return;
        }

        URL resource = plugin.getClass().getClassLoader().getResource(bundledResourcePath);
        if (resource == null) {
            logger.warning(displayName + " resource-pack fragment not installed: bundled resource folder '"
                    + bundledResourcePath + "' was not found in the plugin jar/resources.");
            return;
        }

        try {
            URI uri = resource.toURI();
            if ("jar".equalsIgnoreCase(uri.getScheme())) {
                copyFromJar(uri);
            } else {
                copyTree(Path.of(uri));
            }
            logger.info("Installed " + displayName + " resource-pack fragment into Nexo external_packs/"
                    + externalPackFolderName + ". Regenerate/reload Nexo pack to apply changes.");
        } catch (IOException | URISyntaxException exception) {
            logger.warning("Could not install " + displayName + " resource-pack fragment into Nexo.");
            logger.warning(displayName + " pack fragment installation failed: " + exception.getMessage());
        }
    }

    public ResourcePackFragmentStatus status(boolean configEnabled, boolean fallbackEnabled) {
        boolean bundledResourceExists = plugin.getClass().getClassLoader().getResource(bundledResourcePath) != null;
        boolean nexoPluginAvailable = Bukkit.getPluginManager().getPlugin("Nexo") != null;
        boolean externalPacksExists = Files.isDirectory(nexoExternalPacks);
        boolean installedDirectoryExists = Files.isDirectory(installedPack);
        Map<String, Boolean> requiredFileStatuses = new LinkedHashMap<>();
        for (String requiredFile : requiredFiles) {
            requiredFileStatuses.put(requiredFile, Files.isRegularFile(installedPack.resolve(requiredFile)));
        }
        boolean glyphUiEnabled = configEnabled
                && nexoPluginAvailable
                && externalPacksExists
                && installedDirectoryExists
                && requiredFileStatuses.values().stream().allMatch(Boolean::booleanValue);
        return new ResourcePackFragmentStatus(bundledResourcePath, externalPackFolderName, nexoExternalPacks,
                installedPack, nexoPluginAvailable, externalPacksExists, bundledResourceExists, installedDirectoryExists,
                requiredFileStatuses, configEnabled, glyphUiEnabled, fallbackEnabled);
    }

    public Path nexoExternalPacksPath() { return nexoExternalPacks; }
    public Path installedPackPath() { return installedPack; }

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
            copyTree(fileSystem.getPath("/" + bundledResourcePath));
        } finally {
            if (closeFileSystem && fileSystem != null) fileSystem.close();
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

    private static String normalizeResourcePath(String path) {
        String normalized = Objects.requireNonNull(path, "bundledResourcePath").replace('\\', '/');
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        return normalized;
    }
}
