package me.nakilex.levelplugin.luxbridge;

import me.nakilex.levelplugin.luxbridge.model.LuxAnswer;
import me.nakilex.levelplugin.luxbridge.model.LuxDialogue;
import me.nakilex.levelplugin.luxbridge.model.LuxPage;
import me.nakilex.levelplugin.luxbridge.model.LuxSoundSpec;
import me.nakilex.levelplugin.luxbridge.resource.LuxBridgeResourceManager;
import me.nakilex.levelplugin.luxbridge.session.LuxDialogueSession;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class LuxBridgeManager {
    private static final String DIALOGUES_FOLDER = "LuxBridge/Dialogues";

    private final JavaPlugin plugin;
    private final LuxBridgeResourceManager resourceManager;
    private final Map<String, LuxDialogue> dialogues = new LinkedHashMap<>();
    private final Map<UUID, LuxDialogueSession> sessions = new LinkedHashMap<>();

    public LuxBridgeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.resourceManager = new LuxBridgeResourceManager(plugin);
        reload();
    }

    public void reload() {
        stopAll();
        ensureDefaultFiles();
        resourceManager.reloadAndGenerate();
        dialogues.clear();
        File folder = new File(plugin.getDataFolder(), DIALOGUES_FOLDER);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml") || name.toLowerCase(Locale.ROOT).endsWith(".yaml"));
        if (files == null) return;
        for (File file : files) {
            try {
                LuxDialogue dialogue = load(file);
                dialogues.put(dialogue.id().toLowerCase(Locale.ROOT), dialogue);
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("[LuxBridge] Failed to load " + file.getName() + ": " + exception.getMessage());
            }
        }
        plugin.getLogger().info("[LuxBridge] Loaded " + dialogues.size() + " dialogue(s).");
    }

    public LuxDialogue getDialogue(String id) {
        return id == null ? null : dialogues.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<LuxDialogue> dialogues() {
        return Collections.unmodifiableCollection(dialogues.values());
    }

    public LuxBridgeResourceManager resourceManager() {
        return resourceManager;
    }

    public void start(Player player, String id) {
        LuxDialogue dialogue = getDialogue(id);
        if (dialogue == null) throw new IllegalArgumentException("Unknown dialogue: " + id);
        stop(player);
        LuxDialogueSession session = new LuxDialogueSession(plugin, this, player, dialogue);
        sessions.put(player.getUniqueId(), session);
        session.start();
    }

    public void stop(Player player) {
        LuxDialogueSession session = sessions.remove(player.getUniqueId());
        if (session != null) session.stop(false);
    }

    public void removeSession(UUID uuid) {
        sessions.remove(uuid);
    }

    public boolean hasSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public void skipOrNext(Player player) {
        LuxDialogueSession session = sessions.get(player.getUniqueId());
        if (session != null) session.skipOrNext();
    }

    public void selectNext(Player player) {
        LuxDialogueSession session = sessions.get(player.getUniqueId());
        if (session != null) session.selectNext();
    }

    public void acceptAnswer(Player player) {
        LuxDialogueSession session = sessions.get(player.getUniqueId());
        if (session != null) session.acceptAnswer();
    }

    public void stopAll() {
        for (LuxDialogueSession session : List.copyOf(sessions.values())) session.stop(false);
        sessions.clear();
    }

    public void shutdown() {
        stopAll();
    }

    private void ensureDefaultFiles() {
        File folder = new File(plugin.getDataFolder(), DIALOGUES_FOLDER);
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("[LuxBridge] Could not create dialogue folder " + folder.getPath());
        }
        saveDefault("luxbridge/Dialogues/kingdom_example.yml", new File(folder, "kingdom_example.yml"));
        saveDefault("luxbridge/Dialogues/default_example.yml", new File(folder, "default_example.yml"));
    }

    private void saveDefault(String resource, File target) {
        if (target.exists()) return;
        try (var input = plugin.getResource(resource)) {
            if (input == null) return;
            java.nio.file.Files.copy(input, target.toPath());
        } catch (IOException exception) {
            plugin.getLogger().warning("[LuxBridge] Could not save " + target.getName() + ": " + exception.getMessage());
        }
    }

    private LuxDialogue load(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String id = file.getName().replaceFirst("\\.ya?ml$", "");
        ConfigurationSection settings = yaml.getConfigurationSection("Settings");
        ConfigurationSection sounds = yaml.getConfigurationSection("Sounds");
        ConfigurationSection offsets = yaml.getConfigurationSection("Offsets");
        ConfigurationSection character = yaml.getConfigurationSection("Character");
        ConfigurationSection images = yaml.getConfigurationSection("Images");
        ConfigurationSection colors = yaml.getConfigurationSection("Colors");
        return new LuxDialogue(
                id,
                getInt(settings, "typing-speed", 1),
                getDouble(settings, "range", 3.0),
                getString(settings, "effect", "Slowness"),
                getBool(settings, "answer-numbers", true),
                getBool(settings, "prevent-exit", false),
                getBool(settings, "prevent-skip", false),
                getBool(settings, "character-name", true),
                getBool(settings, "character-image", true),
                getBool(settings, "background-fog", true),
                getBool(settings, "npc-focus", false),
                getBool(settings, "save-progress", false),
                sound(sounds == null ? null : sounds.getConfigurationSection("typing")),
                sound(sounds == null ? null : sounds.getConfigurationSection("selection")),
                getInt(offsets, "name", 0), getInt(offsets, "name-background", 25), getInt(offsets, "dialogue-background", 0),
                getInt(offsets, "dialogue-line", 12), getInt(offsets, "answer-background", 135), getInt(offsets, "answer-line", 13),
                getInt(offsets, "arrow", -7), getInt(offsets, "character", -16),
                getString(character, "name", ""),
                getString(images, "character-background", "character-background"), getString(images, "arrow", "hand"),
                getString(images, "dialogue-background", "dialogue-background"), getString(images, "answer-background", "answer-background"),
                getString(images, "name-start", "name-start"), getString(images, "name-mid", "name-mid"), getString(images, "name-end", "name-end"), getString(images, "fog", "fog"),
                getString(colors, "name", "#ffffff"), getString(colors, "name-background", "#ffffff"), getString(colors, "dialogue", "#ffffff"),
                getString(colors, "dialogue-background", "#ffffff"), getString(colors, "answer", "#ffffff"), getString(colors, "answer-background", "#ffffff"),
                getString(colors, "character-background", "#ffffff"), getString(colors, "arrow", "#ffffff"), getString(colors, "selected", "#ffffff"), getString(colors, "fog", "#000000"),
                pages(yaml.getConfigurationSection("Pages"))
        );
    }

    private Map<String, LuxPage> pages(ConfigurationSection section) {
        Map<String, LuxPage> pages = new LinkedHashMap<>();
        if (section == null) return pages;
        for (String id : section.getKeys(false)) {
            ConfigurationSection page = section.getConfigurationSection(id);
            if (page == null) continue;
            pages.put(id, new LuxPage(id, page.getStringList("lines"), page.getString("typing-info-line", ""), page.getString("steady-info-line", ""),
                    page.getString("goto", ""), page.getInt("timer", 0), stringValues(page.getConfigurationSection("pre-actions")),
                    stringValues(page.getConfigurationSection("post-actions")), stringValues(page.getConfigurationSection("exit-actions")), answers(page.getConfigurationSection("answers"))));
        }
        return pages;
    }

    private Map<String, LuxAnswer> answers(ConfigurationSection section) {
        Map<String, LuxAnswer> answers = new LinkedHashMap<>();
        if (section == null) return answers;
        for (String id : section.getKeys(false)) {
            ConfigurationSection answer = section.getConfigurationSection(id);
            if (answer == null) continue;
            answers.put(id, new LuxAnswer(id, answer.getString("text", id), answer.getString("goto", ""), list(answer, "reply"),
                    answer.getString("condition", ""), sound(answer.getConfigurationSection("sound")), stringValues(answer.getConfigurationSection("actions"))));
        }
        return answers;
    }

    private static LuxSoundSpec sound(ConfigurationSection section) {
        if (section == null) return LuxSoundSpec.EMPTY;
        return new LuxSoundSpec(section.getString("id", ""), section.getString("source", "MASTER"), (float) section.getDouble("volume", 1.0), (float) section.getDouble("pitch", 1.0));
    }

    private static List<String> list(ConfigurationSection section, String path) {
        Object raw = section == null ? null : section.get(path);
        if (raw instanceof List<?> list) {
            List<String> values = new ArrayList<>();
            for (Object item : list) values.add(String.valueOf(item));
            return values;
        }
        return raw == null ? List.of() : List.of(String.valueOf(raw));
    }

    private static List<String> stringValues(ConfigurationSection section) {
        if (section == null) return List.of();
        List<String> values = new ArrayList<>();
        for (String key : section.getKeys(false)) values.add(String.valueOf(section.get(key)));
        return values;
    }

    private static String getString(ConfigurationSection s, String p, String d) { return s == null ? d : s.getString(p, d); }
    private static int getInt(ConfigurationSection s, String p, int d) { return s == null ? d : s.getInt(p, d); }
    private static double getDouble(ConfigurationSection s, String p, double d) { return s == null ? d : s.getDouble(p, d); }
    private static boolean getBool(ConfigurationSection s, String p, boolean d) { return s == null ? d : s.getBoolean(p, d); }
}
