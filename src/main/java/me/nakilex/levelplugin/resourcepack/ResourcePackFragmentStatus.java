package me.nakilex.levelplugin.resourcepack;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable verification result for one Nexo external resource-pack fragment. */
public record ResourcePackFragmentStatus(
        String bundledResourcePath,
        String externalPackFolderName,
        Path nexoExternalPacksPath,
        Path installedPackPath,
        boolean nexoPluginAvailable,
        boolean nexoExternalPacksExists,
        boolean bundledResourceExists,
        boolean installed,
        Map<String, Boolean> requiredFiles,
        boolean configEnabled,
        boolean glyphUiEnabled,
        boolean fallbackEnabled
) {
    public ResourcePackFragmentStatus {
        requiredFiles = Collections.unmodifiableMap(new LinkedHashMap<>(requiredFiles));
    }

    public boolean requiredFileExists(String relativePath) {
        return requiredFiles.getOrDefault(relativePath, false);
    }

    public boolean allRequiredFilesExist() {
        return requiredFiles.values().stream().allMatch(Boolean::booleanValue);
    }
}
