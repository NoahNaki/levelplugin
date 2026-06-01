package me.nakilex.levelplugin.npc.dialog.engine;

import me.nakilex.levelplugin.Main;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

/** Loads page dialogues from plugins/LevelPlugin/dialogues/*.yml. */
public final class DialogueDefinitionLoader {
    private final Main plugin;
    private final File directory;
    private final Map<String, DialogueDefinition> dialogues = new LinkedHashMap<>();

    public DialogueDefinitionLoader(Main plugin) {
        this.plugin = plugin;
        this.directory = new File(plugin.getDataFolder(), "dialogues");
        if (!directory.exists() && !directory.mkdirs()) {
            plugin.getLogger().warning("Could not create dialogue directory: " + directory);
        }
        reload();
    }

    public void reload() {
        dialogues.clear();
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            try {
                DialogueDefinition dialogue = load(file);
                dialogues.put(dialogue.id().toLowerCase(Locale.ROOT), dialogue);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().log(Level.WARNING, "Could not load dialogue " + file.getName(), exception);
            }
        }
    }

    public DialogueDefinition get(String id) {
        return id == null ? null : dialogues.get(id.toLowerCase(Locale.ROOT));
    }

    public Map<String, DialogueDefinition> all() {
        return Collections.unmodifiableMap(dialogues);
    }

    private DialogueDefinition load(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String fallbackId = file.getName().substring(0, file.getName().length() - 4);
        String id = yaml.getString("id", fallbackId);
        ConfigurationSection pagesSection = yaml.getConfigurationSection("pages");
        if (pagesSection == null || pagesSection.getKeys(false).isEmpty()) {
            throw new IllegalArgumentException("Dialogue must define at least one page");
        }
        Map<String, DialoguePage> pages = new LinkedHashMap<>();
        for (String pageId : pagesSection.getKeys(false)) pages.put(pageId, loadPage(pageId, pagesSection.getConfigurationSection(pageId)));
        String startPage = yaml.getString("start-page", pages.containsKey("start") ? "start" : pages.keySet().iterator().next());
        return new DialogueDefinition(id, startPage, pages,
                yaml.getInt("settings.typing-speed", DialogueDefinition.DEFAULT_TYPING_SPEED_TICKS),
                yaml.getDouble("settings.range", DialogueDefinition.DEFAULT_RANGE),
                yaml.getBoolean("settings.prevent-skip", false),
                yaml.getBoolean("settings.prevent-exit", false),
                parseEffect(yaml.getString("settings.effect", DialogueEffect.SLOWNESS.name())),
                yaml.getBoolean("settings.answer-numbers", false),
                loadSound(yaml.getConfigurationSection("sounds.typing"), null),
                loadSound(yaml.getConfigurationSection("sounds.selection"), DialogueDefinition.DEFAULT_SELECTION_SOUND),
                loadSound(yaml.getConfigurationSection("sounds.confirm"), DialogueDefinition.DEFAULT_CONFIRM_SOUND));
    }

    private DialoguePage loadPage(String id, ConfigurationSection section) {
        if (section == null) throw new IllegalArgumentException("Page " + id + " must be a section");
        List<DialogueAnswer> answers = new ArrayList<>();
        ConfigurationSection answersSection = section.getConfigurationSection("answers");
        if (answersSection != null) {
            for (String answerId : answersSection.getKeys(false)) {
                ConfigurationSection answer = answersSection.getConfigurationSection(answerId);
                if (answer == null) continue;
                answers.add(new DialogueAnswer(answer.getString("text", answerId), values(answer, "goto"),
                        values(answer, "actions"), conditions(answer), values(answer, "replies")));
            }
        }
        return new DialoguePage(id, values(section, "lines"), values(section, "goto"),
                values(section, "pre-actions"), values(section, "post-actions"),
                values(section, "exit-actions"), answers);
    }

    private List<String> conditions(ConfigurationSection section) {
        List<String> conditions = values(section, "conditions");
        return conditions.isEmpty() ? values(section, "condition") : conditions;
    }

    private List<String> values(ConfigurationSection section, String path) {
        if (section.isList(path)) return section.getStringList(path);
        String value = section.getString(path);
        return value == null || value.isBlank() ? List.of() : List.of(value);
    }

    private DialogueEffect parseEffect(String value) {
        try {
            return DialogueEffect.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Unknown dialogue effect " + value + "; using SLOWNESS");
            return DialogueEffect.SLOWNESS;
        }
    }

    private DialogueSound loadSound(ConfigurationSection section, DialogueSound fallback) {
        if (section == null) return fallback;
        String id = section.getString("id");
        if (id == null || id.isBlank()) return fallback;
        SoundCategory category;
        try {
            category = SoundCategory.valueOf(section.getString("source", SoundCategory.MASTER.name()).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            category = SoundCategory.MASTER;
        }
        return new DialogueSound(id, category, (float) section.getDouble("volume", 1.0), (float) section.getDouble("pitch", 1.0));
    }
}
