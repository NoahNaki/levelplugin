package me.nakilex.levelplugin.utils;

import java.io.File;

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
}
