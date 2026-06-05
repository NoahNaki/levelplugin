package me.nakilex.levelplugin.dialogue.config;

import me.nakilex.levelplugin.dialogue.model.DialogueAnswer;
import me.nakilex.levelplugin.dialogue.model.DialogueColors;
import me.nakilex.levelplugin.dialogue.model.DialogueDefinition;
import me.nakilex.levelplugin.dialogue.model.DialogueImages;
import me.nakilex.levelplugin.dialogue.model.DialogueOffsets;
import me.nakilex.levelplugin.dialogue.model.DialoguePage;
import me.nakilex.levelplugin.dialogue.model.DialogueSettings;
import me.nakilex.levelplugin.dialogue.model.DialogueSoundSpec;
import me.nakilex.levelplugin.dialogue.model.DialogueSounds;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Parses LuxDialogues-style YAML into immutable dialogue model objects.
 */
public class DialogueConfigLoader {

    public DialogueDefinition load(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String fallbackId = stripYamlExtension(file == null ? "" : file.getName());
        return load(yaml, fallbackId);
    }

    public DialogueDefinition load(YamlConfiguration yaml, String fallbackId) {
        String id = firstString(yaml, fallbackId, "id", "Id", "ID");
        ConfigurationSection settings = firstSection(yaml, "Settings", "settings");
        ConfigurationSection character = firstSection(yaml, "Character", "character");
        ConfigurationSection sounds = firstSection(yaml, "Sounds", "sounds");
        ConfigurationSection offsets = firstSection(yaml, "Offsets", "offsets");
        ConfigurationSection images = firstSection(yaml, "Images", "images");
        ConfigurationSection colors = firstSection(yaml, "Colors", "colors");

        return new DialogueDefinition(
                id,
                new DialogueSettings(toRawMap(settings), toRawMap(character)),
                new DialogueSounds(toRawMap(sounds)),
                new DialogueOffsets(toRawMap(offsets)),
                new DialogueImages(toRawMap(images)),
                new DialogueColors(toRawMap(colors)),
                loadPages(firstSection(yaml, "Pages", "pages"), firstSection(yaml, "answers", "Answers"))
        );
    }

    private Map<String, DialoguePage> loadPages(ConfigurationSection pagesSection, ConfigurationSection sharedAnswers) {
        if (pagesSection == null) {
            return Map.of();
        }

        Map<String, DialoguePage> pages = new LinkedHashMap<>();
        for (String pageId : pagesSection.getKeys(false)) {
            ConfigurationSection page = pagesSection.getConfigurationSection(pageId);
            if (page == null) {
                continue;
            }

            Map<String, DialogueAnswer> answers = loadAnswers(
                    firstSection(page, "answers", "Answers"),
                    sharedAnswers
            );
            pages.put(pageId, new DialoguePage(
                    pageId,
                    stringList(page, "lines", "Lines", "text", "Text"),
                    firstString(page, null, "typingInfoLine", "typing_info_line", "typing-info-line", "typing_info", "typing-info"),
                    firstString(page, null, "steadyInfoLine", "steady_info_line", "steady-info-line", "steady_info", "steady-info"),
                    stringList(page, "goto", "Goto", "go-to", "go_to", "next", "Next"),
                    firstInteger(page, "timer", "Timer"),
                    stringList(page, "preActions", "pre-actions", "pre_actions", "pre", "PreActions"),
                    stringList(page, "postActions", "post-actions", "post_actions", "post", "PostActions"),
                    stringList(page, "exitActions", "exit-actions", "exit_actions", "exit", "ExitActions"),
                    answers
            ));
        }
        return Collections.unmodifiableMap(pages);
    }

    private Map<String, DialogueAnswer> loadAnswers(ConfigurationSection pageAnswers, ConfigurationSection sharedAnswers) {
        Map<String, DialogueAnswer> answers = new LinkedHashMap<>();
        loadAnswersInto(answers, sharedAnswers);
        loadAnswersInto(answers, pageAnswers);
        return Collections.unmodifiableMap(answers);
    }

    private void loadAnswersInto(Map<String, DialogueAnswer> answers, ConfigurationSection section) {
        if (section == null) {
            return;
        }
        for (String answerId : section.getKeys(false)) {
            ConfigurationSection answer = section.getConfigurationSection(answerId);
            if (answer == null) {
                String text = section.getString(answerId);
                if (text != null) {
                    answers.put(answerId, new DialogueAnswer(answerId, text, List.of(), List.of(), DialogueSoundSpec.empty(), List.of(), List.of()));
                }
                continue;
            }
            answers.put(answerId, new DialogueAnswer(
                    answerId,
                    firstString(answer, null, "text", "Text", "message", "Message"),
                    stringList(answer, "goto", "Goto", "go-to", "go_to", "next", "Next"),
                    stringList(answer, "reply", "replies", "replyMessages", "reply-messages", "reply_messages", "messages"),
                    soundSpec(answer, "sound", "Sound"),
                    stringList(answer, "conditions", "condition", "Conditions"),
                    stringList(answer, "actions", "action", "Actions")
            ));
        }
    }

    private static DialogueSoundSpec soundSpec(ConfigurationSection section, String... paths) {
        if (section == null || paths == null) {
            return DialogueSoundSpec.empty();
        }
        for (String path : paths) {
            if (!section.contains(path)) {
                continue;
            }
            ConfigurationSection soundSection = section.getConfigurationSection(path);
            if (soundSection != null) {
                return new DialogueSoundSpec(toRawMap(soundSection));
            }
            Object value = section.get(path);
            if (value instanceof Map<?, ?> map) {
                return new DialogueSoundSpec(immutableMap(map));
            }
            if (value != null) {
                return DialogueSoundSpec.ofId(String.valueOf(value));
            }
        }
        return DialogueSoundSpec.empty();
    }

    private static ConfigurationSection firstSection(ConfigurationSection section, String... paths) {
        if (section == null || paths == null) {
            return null;
        }
        for (String path : paths) {
            ConfigurationSection child = section.getConfigurationSection(path);
            if (child != null) {
                return child;
            }
        }
        return null;
    }

    private static String firstString(ConfigurationSection section, String fallback, String... paths) {
        if (section == null || paths == null) {
            return fallback;
        }
        for (String path : paths) {
            if (section.contains(path)) {
                String value = section.getString(path);
                if (value != null) {
                    return value;
                }
            }
        }
        return fallback;
    }

    private static Integer firstInteger(ConfigurationSection section, String... paths) {
        if (section == null || paths == null) {
            return null;
        }
        for (String path : paths) {
            if (section.contains(path)) {
                return section.getInt(path);
            }
        }
        return null;
    }

    private static List<String> stringList(ConfigurationSection section, String... paths) {
        if (section == null || paths == null) {
            return List.of();
        }
        for (String path : paths) {
            if (!section.contains(path)) {
                continue;
            }
            Object value = section.get(path);
            if (value instanceof List<?> list) {
                List<String> strings = new ArrayList<>();
                for (Object item : list) {
                    if (item != null) {
                        strings.add(String.valueOf(item));
                    }
                }
                return List.copyOf(strings);
            }
            if (value != null) {
                return List.of(String.valueOf(value));
            }
        }
        return List.of();
    }

    private static Map<String, Object> toRawMap(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection child) {
                values.put(key, toRawMap(child));
            } else if (value instanceof List<?> list) {
                values.put(key, immutableList(list));
            } else {
                values.put(key, value);
            }
        }
        return Collections.unmodifiableMap(values);
    }

    private static List<Object> immutableList(List<?> list) {
        List<Object> values = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof ConfigurationSection section) {
                values.add(toRawMap(section));
            } else if (item instanceof Map<?, ?> map) {
                values.add(immutableMap(map));
            } else if (item instanceof List<?> childList) {
                values.add(immutableList(childList));
            } else {
                values.add(item);
            }
        }
        return Collections.unmodifiableList(values);
    }

    private static Map<String, Object> immutableMap(Map<?, ?> map) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> childMap) {
                value = immutableMap(childMap);
            } else if (value instanceof List<?> list) {
                value = immutableList(list);
            }
            values.put(String.valueOf(entry.getKey()), value);
        }
        return Collections.unmodifiableMap(values);
    }

    private static String stripYamlExtension(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".yml")) {
            return fileName.substring(0, fileName.length() - 4);
        }
        if (lower.endsWith(".yaml")) {
            return fileName.substring(0, fileName.length() - 5);
        }
        return fileName;
    }
}
