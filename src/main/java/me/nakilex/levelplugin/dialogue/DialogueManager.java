package me.nakilex.levelplugin.dialogue;

import me.nakilex.levelplugin.dialogue.config.DialogueConfigLoader;
import me.nakilex.levelplugin.dialogue.model.DialogueDefinition;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Standalone registry for Lux-style dialogue definitions loaded from the Dialogues folder.
 */
public class DialogueManager {
    private static final String PRIMARY_DIALOGUES_FOLDER = "Dialogues";
    private static final String LEGACY_DIALOGUES_FOLDER = "dialogues";

    private final JavaPlugin plugin;
    private final DialogueConfigLoader loader;
    private final Map<String, DialogueDefinition> definitions = new LinkedHashMap<>();

    public DialogueManager(JavaPlugin plugin) {
        this(plugin, new DialogueConfigLoader());
    }

    public DialogueManager(JavaPlugin plugin, DialogueConfigLoader loader) {
        this.plugin = plugin;
        this.loader = loader == null ? new DialogueConfigLoader() : loader;
        reload();
    }

    public DialogueDefinition getDialogue(String id) {
        if (id == null) {
            return null;
        }
        return definitions.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<DialogueDefinition> getDialogues() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    public void reload() {
        Map<String, DialogueDefinition> loaded = new LinkedHashMap<>();
        File primaryFolder = new File(plugin.getDataFolder(), PRIMARY_DIALOGUES_FOLDER);
        File legacyFolder = new File(plugin.getDataFolder(), LEGACY_DIALOGUES_FOLDER);

        if (!primaryFolder.exists() && !legacyFolder.exists() && !primaryFolder.mkdirs()) {
            plugin.getLogger().warning("[DialogueManager] Could not create dialogues folder: " + primaryFolder.getPath());
            definitions.clear();
            return;
        }

        if (legacyFolder.exists()) {
            loadFolder(legacyFolder, loaded);
        }
        if (!sameFile(primaryFolder, legacyFolder) && primaryFolder.exists()) {
            loadFolder(primaryFolder, loaded);
        }

        definitions.clear();
        definitions.putAll(loaded);
        plugin.getLogger().info("[DialogueManager] Loaded " + definitions.size() + " dialogue definition(s).");
    }

    private void loadFolder(File folder, Map<String, DialogueDefinition> loaded) {
        File[] files = folder.listFiles((dir, name) -> isDialogueConfigFile(name));
        if (files == null) {
            return;
        }

        for (File file : files) {
            try {
                DialogueDefinition definition = loader.load(file);
                if (definition.id().isBlank()) {
                    plugin.getLogger().warning("[DialogueManager] Skipping dialogue with blank id in " + file.getName());
                    continue;
                }
                loaded.put(definition.id().toLowerCase(Locale.ROOT), definition);
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("[DialogueManager] Failed to load " + file.getName()
                        + ": " + exception.getMessage());
            }
        }
    }

    private static boolean isDialogueConfigFile(String name) {
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".yml") || lower.endsWith(".yaml");
    }

    private static boolean sameFile(File first, File second) {
        try {
            return first.getCanonicalFile().equals(second.getCanonicalFile());
        } catch (java.io.IOException ignored) {
            return first.equals(second);
        }
    }
}
