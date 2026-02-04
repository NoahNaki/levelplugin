package me.nakilex.levelplugin.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;

/** Utility methods for basic file operations. */
public final class FileUtil {
    private FileUtil() {}

    /**
     * Recursively delete the given directory or file if it exists.
     */
    public static void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return;
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteDirectory(f);
                }
            }
        }
        dir.delete();
    }

    /**
     * Recursively copy a directory and its contents to the destination.
     */
    public static void copyDirectory(File source, File target) throws IOException {
        if (source == null || target == null) {
            return;
        }
        Path sourcePath = source.toPath();
        Path targetPath = target.toPath();
        Files.walk(sourcePath).forEach(path -> {
            try {
                Path relative = sourcePath.relativize(path);
                Path dest = targetPath.resolve(relative);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Atomically write a Bukkit {@link FileConfiguration} to disk by saving to a
     * temporary file and swapping it into place.
     *
     * @param target target file to write
     * @param config configuration to persist
     * @throws IOException when a write fails
     */
    public static void writeYamlAtomic(File target, FileConfiguration config) throws IOException {
        if (target == null || config == null) {
            return;
        }
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        String tempName = target.getName() + "." + UUID.randomUUID() + ".tmp";
        File tempFile = new File(target.getParentFile(), tempName);
        config.save(tempFile);
        try {
            Files.move(tempFile.toPath(), target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tempFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
