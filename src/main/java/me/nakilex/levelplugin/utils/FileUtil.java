package me.nakilex.levelplugin.utils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;

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
     * Resolve the code source file for a class, typically the jar or classes directory.
     */
    public static File getCodeSourceFile(Class<?> sourceClass) {
        if (sourceClass == null) {
            return null;
        }
        CodeSource codeSource = sourceClass.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            return null;
        }
        URL location = codeSource.getLocation();
        if (location == null) {
            return null;
        }
        try {
            return Path.of(location.toURI()).toFile();
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
